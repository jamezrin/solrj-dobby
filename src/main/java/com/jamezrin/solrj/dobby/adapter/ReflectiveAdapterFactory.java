package com.jamezrin.solrj.dobby.adapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import com.jamezrin.solrj.dobby.BeanProperties;
import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.DobbyException;
import com.jamezrin.solrj.dobby.DobbyUtils;
import com.jamezrin.solrj.dobby.FieldNamingStrategy;
import com.jamezrin.solrj.dobby.TypeAdapter;
import com.jamezrin.solrj.dobby.TypeAdapterFactory;
import com.jamezrin.solrj.dobby.TypeToken;
import com.jamezrin.solrj.dobby.annotation.SolrField;

/**
 * Factory that creates adapters for classes annotated with {@link SolrField}.
 *
 * <p>Handles both traditional POJOs (via no-arg constructor + field/setter injection) and Java
 * records (via canonical constructor).
 *
 * <p>For nested documents, supports reading from both named field values (when Solr returns {@code
 * SolrDocument} instances as field values) and from {@link SolrDocument#getChildDocuments()}.
 */
public final class ReflectiveAdapterFactory implements TypeAdapterFactory {

  @Override
  public <T> TypeAdapter<T> create(Dobby dobby, TypeToken<T> type) {
    Class<? super T> raw = type.getRawType();

    // Skip interfaces, abstract classes, primitives, arrays, and common JDK types
    if (raw.isInterface()
        || Modifier.isAbstract(raw.getModifiers())
        || raw.isPrimitive()
        || raw.isArray()
        || raw.isEnum()
        || raw.getPackageName().startsWith("java.")
        || raw.getPackageName().startsWith("javax.")
        || raw.getPackageName().startsWith("org.apache.solr.")) {
      return null;
    }

    // Only handle classes that have at least one @SolrField annotation
    List<BoundField> boundFields = collectBoundFields(dobby, raw);
    if (boundFields.isEmpty()) {
      return null;
    }

    if (raw.isRecord()) {
      return createRecordAdapter(dobby, raw, boundFields);
    }

    return createPojoAdapter(dobby, raw, boundFields);
  }

  private <T> TypeAdapter<T> createPojoAdapter(
      Dobby dobby, Class<? super T> raw, List<BoundField> boundFields) {
    Constructor<?> constructor;
    try {
      constructor = raw.getDeclaredConstructor();
      constructor.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new DobbyException(
          raw.getName()
              + " requires a no-arg constructor for POJO binding. "
              + "Consider using a Java record instead.",
          e);
    }

    return new TypeAdapter<>() {
      @Override
      public T read(Object solrValue) {
        if (solrValue == null) return null;
        if (!(solrValue instanceof SolrDocument doc)) {
          throw new DobbyException(
              "Expected SolrDocument for "
                  + raw.getName()
                  + ", got "
                  + solrValue.getClass().getName());
        }

        try {
          T obj = DobbyUtils.uncheckedCast(constructor.newInstance());
          for (BoundField bf : boundFields) {
            Object fieldValue = readFieldValue(bf, doc);
            if (fieldValue != null || !bf.type().isPrimitive()) {
              bf.set(obj, fieldValue);
            }
          }
          return obj;
        } catch (DobbyException e) {
          throw e;
        } catch (Exception e) {
          throw new DobbyException("Failed to create " + raw.getName(), e);
        }
      }

      @Override
      public Object write(T value) {
        if (value == null) return null;
        return writeToDoc(boundFields, value);
      }

      @Override
      public String toString() {
        return "ReflectiveAdapter[" + raw.getSimpleName() + "]";
      }
    };
  }

  private <T> TypeAdapter<T> createRecordAdapter(
      Dobby dobby, Class<? super T> raw, List<BoundField> boundFields) {
    RecordComponent[] components = raw.getRecordComponents();
    Constructor<?> canonicalConstructor;
    try {
      Class<?>[] paramTypes = new Class<?>[components.length];
      for (int i = 0; i < components.length; i++) {
        paramTypes[i] = components[i].getType();
      }
      canonicalConstructor = raw.getDeclaredConstructor(paramTypes);
      canonicalConstructor.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new DobbyException("Cannot find canonical constructor for record " + raw.getName(), e);
    }

    // Map solr field name → index in canonical constructor
    Map<String, Integer> componentIndexByName = new LinkedHashMap<>();
    for (int i = 0; i < components.length; i++) {
      componentIndexByName.put(components[i].getName(), i);
    }

    return new TypeAdapter<>() {
      @Override
      public T read(Object solrValue) {
        if (solrValue == null) return null;
        if (!(solrValue instanceof SolrDocument doc)) {
          throw new DobbyException(
              "Expected SolrDocument for record "
                  + raw.getName()
                  + ", got "
                  + solrValue.getClass().getName());
        }

        Object[] args = new Object[components.length];
        for (BoundField bf : boundFields) {
          Integer idx = componentIndexByName.get(bf.javaName());
          if (idx != null) {
            Object fieldValue = readFieldValue(bf, doc);
            args[idx] = fieldValue;
          }
        }

        // Fill defaults for primitives
        for (int i = 0; i < components.length; i++) {
          if (args[i] == null && components[i].getType().isPrimitive()) {
            args[i] = defaultValue(components[i].getType());
          }
        }

        try {
          return DobbyUtils.uncheckedCast(canonicalConstructor.newInstance(args));
        } catch (Exception e) {
          throw new DobbyException("Failed to create record " + raw.getName(), e);
        }
      }

      @Override
      public Object write(T value) {
        if (value == null) return null;
        return writeToDoc(boundFields, value);
      }

      @Override
      public String toString() {
        return "ReflectiveAdapter[record " + raw.getSimpleName() + "]";
      }
    };
  }

  private static Object readFieldValue(BoundField bf, SolrDocument doc) {
    if (bf.nested()) {
      return readNestedValue(bf, doc);
    }
    Object raw = doc.getFieldValue(bf.solrName());
    if (raw == null) {
      // For Optional fields, we must still invoke the adapter so that
      // null becomes Optional.empty() instead of remaining null.
      if (Optional.class.isAssignableFrom(bf.type())) {
        return bf.adapter().read(null);
      }
      return null;
    }
    return bf.adapter().read(raw);
  }

  private static Object readNestedValue(BoundField bf, SolrDocument doc) {
    // Strategy 1: Look for named field value that contains SolrDocument(s)
    Object fieldValue = doc.getFieldValue(bf.solrName());
    if (fieldValue != null) {
      return bf.adapter().read(fieldValue);
    }

    // Strategy 2: Look in child documents
    List<SolrDocument> children = doc.getChildDocuments();
    if (children != null && !children.isEmpty()) {
      // If the target type is a collection/array, pass the full list
      Class<?> fieldType = bf.type();
      if (Collection.class.isAssignableFrom(fieldType) || fieldType.isArray()) {
        return bf.adapter().read(children);
      }
      // Single nested object - use the first child
      return bf.adapter().read(children.get(0));
    }

    return null;
  }

  private static SolrInputDocument writeToDoc(List<BoundField> boundFields, Object value) {
    SolrInputDocument doc = new SolrInputDocument();
    for (BoundField bf : boundFields) {
      Object fieldValue = bf.get(value);
      if (fieldValue == null) continue;

      TypeAdapter<Object> adapter = DobbyUtils.uncheckedCast(bf.adapter());
      Object solrValue = adapter.write(fieldValue);
      if (solrValue == null) continue;

      if (bf.nested()) {
        // Nested documents become child documents
        if (solrValue instanceof Collection<?> coll) {
          for (Object item : coll) {
            if (item instanceof SolrInputDocument child) {
              doc.addChildDocument(child);
            }
          }
        } else if (solrValue instanceof SolrInputDocument child) {
          doc.addChildDocument(child);
        }
      } else if (bf.dynamicMap() && solrValue instanceof Map<?, ?> map) {
        // Dynamic field: spread map entries as individual fields
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          doc.setField(entry.getKey().toString(), entry.getValue());
        }
      } else {
        doc.setField(bf.solrName(), solrValue);
      }
    }
    return doc;
  }

  private List<BoundField> collectBoundFields(Dobby dobby, Class<?> clazz) {
    List<BoundField> result = new ArrayList<>();
    FieldNamingStrategy namingStrategy = dobby.getFieldNamingStrategy();

    // Walk up the class hierarchy
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
      if (current.isRecord()) {
        collectRecordComponents(dobby, current, namingStrategy, result);
      } else {
        collectClassFields(dobby, current, namingStrategy, result);
        collectSetterMethods(dobby, current, namingStrategy, result);
      }
      current = current.getSuperclass();
    }
    return result;
  }

  private void collectRecordComponents(
      Dobby dobby, Class<?> clazz, FieldNamingStrategy namingStrategy, List<BoundField> result) {
    for (RecordComponent rc : clazz.getRecordComponents()) {
      SolrField ann = rc.getAnnotation(SolrField.class);
      if (ann == null) {
        // Also check the backing field
        try {
          Field backingField = clazz.getDeclaredField(rc.getName());
          ann = backingField.getAnnotation(SolrField.class);
        } catch (NoSuchFieldException ignored) {
        }
      }
      if (ann == null) continue;

      String solrName = resolveSolrName(ann, rc.getName(), namingStrategy);
      Type genericType = rc.getGenericType();
      TypeAdapter<?> adapter = dobby.getAdapter(TypeToken.of(genericType));

      Method accessor = rc.getAccessor();
      accessor.setAccessible(true);

      boolean isDynamicMap = Map.class.isAssignableFrom(rc.getType()) && !ann.nested();

      result.add(
          new BoundField(
              rc.getName(),
              solrName,
              rc.getType(),
              ann.nested(),
              isDynamicMap,
              adapter,
              null,
              null,
              accessor,
              null));
    }
  }

  private void collectClassFields(
      Dobby dobby, Class<?> clazz, FieldNamingStrategy namingStrategy, List<BoundField> result) {
    for (Field field : clazz.getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
        continue;
      }

      SolrField ann = field.getAnnotation(SolrField.class);
      if (ann == null) continue;

      field.setAccessible(true);
      String solrName = resolveSolrName(ann, field.getName(), namingStrategy);
      Type genericType = field.getGenericType();
      TypeAdapter<?> adapter = dobby.getAdapter(TypeToken.of(genericType));

      boolean isDynamicMap = Map.class.isAssignableFrom(field.getType()) && !ann.nested();

      result.add(
          new BoundField(
              field.getName(),
              solrName,
              field.getType(),
              ann.nested(),
              isDynamicMap,
              adapter,
              field,
              null,
              null,
              null));
    }
  }

  private void collectSetterMethods(
      Dobby dobby, Class<?> clazz, FieldNamingStrategy namingStrategy, List<BoundField> result) {
    Map<String, Method> getters = BeanProperties.getterMap(clazz);

    for (Method method : clazz.getDeclaredMethods()) {
      if (Modifier.isStatic(method.getModifiers())) continue;

      SolrField ann = method.getAnnotation(SolrField.class);
      if (ann == null) continue;

      if (method.getParameterCount() != 1) {
        throw new DobbyException(
            "@SolrField on method " + method + " requires exactly one parameter (setter pattern)");
      }

      method.setAccessible(true);

      // Derive property name from setter using Introspector conventions
      String javaName = BeanProperties.propertyNameFromSetter(method);
      String solrName = resolveSolrName(ann, javaName, namingStrategy);
      Type genericType = method.getGenericParameterTypes()[0];
      Class<?> paramType = method.getParameterTypes()[0];
      TypeAdapter<?> adapter = dobby.getAdapter(TypeToken.of(genericType));

      // Look up matching getter via Introspector
      Method getter = getters.get(javaName);

      boolean isDynamicMap = Map.class.isAssignableFrom(paramType) && !ann.nested();

      result.add(
          new BoundField(
              javaName,
              solrName,
              paramType,
              ann.nested(),
              isDynamicMap,
              adapter,
              null,
              method,
              getter,
              null));
    }
  }

  private static String resolveSolrName(
      SolrField ann, String javaName, FieldNamingStrategy strategy) {
    if (ann.value() != null && !ann.value().isEmpty()) {
      return ann.value();
    }
    return strategy.translateName(javaName);
  }

  private static Object defaultValue(Class<?> type) {
    if (type == boolean.class) return false;
    if (type == byte.class) return (byte) 0;
    if (type == short.class) return (short) 0;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == float.class) return 0.0f;
    if (type == double.class) return 0.0d;
    if (type == char.class) return '\0';
    return null;
  }

  /** Represents a single bound field with all the metadata needed for reading/writing. */
  record BoundField(
      String javaName,
      String solrName,
      Class<?> type,
      boolean nested,
      boolean dynamicMap,
      TypeAdapter<?> adapter,
      Field field, // non-null for class fields
      Method setter, // non-null for setter-based binding
      Method getter, // non-null for getter-based reading (setters or records)
      Method recordAccessor // alias for getter in records (unused, getter covers both)
      ) {
    void set(Object target, Object value) {
      try {
        if (field != null) {
          field.set(target, value);
        } else if (setter != null) {
          setter.invoke(target, value);
        }
      } catch (Exception e) {
        throw new DobbyException(
            "Failed to set field '" + javaName + "' on " + target.getClass().getName(), e);
      }
    }

    Object get(Object target) {
      try {
        if (field != null) {
          return field.get(target);
        } else if (getter != null) {
          return getter.invoke(target);
        }
        throw new DobbyException(
            "No getter available for field '" + javaName + "' on " + target.getClass().getName());
      } catch (DobbyException e) {
        throw e;
      } catch (Exception e) {
        throw new DobbyException(
            "Failed to get field '" + javaName + "' from " + target.getClass().getName(), e);
      }
    }
  }
}
