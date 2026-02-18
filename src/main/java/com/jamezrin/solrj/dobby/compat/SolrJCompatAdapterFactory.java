package com.jamezrin.solrj.dobby.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.beans.Field;
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
 * Compatibility factory that reads SolrJ's {@link Field @Field} annotation.
 *
 * <p>This allows gradual migration from SolrJ's built-in {@code DocumentObjectBinder} to Dobby -
 * existing classes annotated with {@code @Field} work without changes.
 *
 * <p>This factory has lower priority than the {@code @SolrField}-based {@link
 * com.jamezrin.solrj.dobby.adapter.ReflectiveAdapterFactory}. If a class has both annotations,
 * {@code @SolrField} wins.
 */
public final class SolrJCompatAdapterFactory implements TypeAdapterFactory {

  /** The sentinel value SolrJ uses to mean "use the Java field name". */
  private static final String SOLRJ_DEFAULT = "#default";

  @Override
  public <T> TypeAdapter<T> create(Dobby dobby, TypeToken<T> type) {
    Class<? super T> raw = type.getRawType();

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

    List<CompatBoundField> fields = collectFields(dobby, raw);
    if (fields.isEmpty()) {
      return null;
    }

    if (raw.isRecord()) {
      return createRecordAdapter(raw, fields);
    }
    return createPojoAdapter(raw, fields);
  }

  private <T> TypeAdapter<T> createPojoAdapter(
      Class<? super T> raw, List<CompatBoundField> fields) {
    Constructor<?> constructor;
    try {
      constructor = raw.getDeclaredConstructor();
      constructor.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new DobbyException(
          raw.getName() + " requires a no-arg constructor for SolrJ @Field compat binding.", e);
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
          for (CompatBoundField bf : fields) {
            Object val = readValue(bf, doc);
            if (val != null || !bf.type.isPrimitive()) {
              bf.set(obj, val);
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
        return writeDoc(fields, value);
      }

      @Override
      public String toString() {
        return "SolrJCompatAdapter[" + raw.getSimpleName() + "]";
      }
    };
  }

  private <T> TypeAdapter<T> createRecordAdapter(
      Class<? super T> raw, List<CompatBoundField> fields) {
    RecordComponent[] components = raw.getRecordComponents();
    Constructor<?> canonicalCtor;
    try {
      Class<?>[] paramTypes = new Class<?>[components.length];
      for (int i = 0; i < components.length; i++) {
        paramTypes[i] = components[i].getType();
      }
      canonicalCtor = raw.getDeclaredConstructor(paramTypes);
      canonicalCtor.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new DobbyException("Cannot find canonical constructor for record " + raw.getName(), e);
    }

    Map<String, Integer> indexByName = new LinkedHashMap<>();
    for (int i = 0; i < components.length; i++) {
      indexByName.put(components[i].getName(), i);
    }

    return new TypeAdapter<>() {
      @Override
      public T read(Object solrValue) {
        if (solrValue == null) return null;
        if (!(solrValue instanceof SolrDocument doc)) {
          throw new DobbyException("Expected SolrDocument for record " + raw.getName());
        }

        Object[] args = new Object[components.length];
        for (CompatBoundField bf : fields) {
          Integer idx = indexByName.get(bf.javaName);
          if (idx != null) {
            args[idx] = readValue(bf, doc);
          }
        }
        // Default primitives
        for (int i = 0; i < components.length; i++) {
          if (args[i] == null && components[i].getType().isPrimitive()) {
            args[i] = defaultPrimitive(components[i].getType());
          }
        }
        try {
          return DobbyUtils.uncheckedCast(canonicalCtor.newInstance(args));
        } catch (Exception e) {
          throw new DobbyException("Failed to create record " + raw.getName(), e);
        }
      }

      @Override
      public Object write(T value) {
        if (value == null) return null;
        return writeDoc(fields, value);
      }

      @Override
      public String toString() {
        return "SolrJCompatAdapter[record " + raw.getSimpleName() + "]";
      }
    };
  }

  private static Object readValue(CompatBoundField bf, SolrDocument doc) {
    if (bf.child) {
      // Try field value first, then child documents
      Object fv = doc.getFieldValue(bf.solrName);
      if (fv != null) return bf.adapter.read(fv);

      List<SolrDocument> children = doc.getChildDocuments();
      if (children != null && !children.isEmpty()) {
        if (Collection.class.isAssignableFrom(bf.type) || bf.type.isArray()) {
          return bf.adapter.read(children);
        }
        return bf.adapter.read(children.get(0));
      }
      return null;
    }
    Object raw = doc.getFieldValue(bf.solrName);
    if (raw == null) return null;
    return bf.adapter.read(raw);
  }

  private static SolrInputDocument writeDoc(List<CompatBoundField> fields, Object value) {
    SolrInputDocument doc = new SolrInputDocument();
    for (CompatBoundField bf : fields) {
      Object fv = bf.get(value);
      if (fv == null) continue;
      TypeAdapter<Object> adapter = DobbyUtils.uncheckedCast(bf.adapter);
      Object sv = adapter.write(fv);
      if (sv == null) continue;

      if (bf.child) {
        if (sv instanceof Collection<?> c) {
          for (Object item : c) {
            if (item instanceof SolrInputDocument sid) doc.addChildDocument(sid);
          }
        } else if (sv instanceof SolrInputDocument sid) {
          doc.addChildDocument(sid);
        }
      } else {
        doc.setField(bf.solrName, sv);
      }
    }
    return doc;
  }

  private List<CompatBoundField> collectFields(Dobby dobby, Class<?> clazz) {
    List<CompatBoundField> result = new ArrayList<>();
    FieldNamingStrategy naming = dobby.getFieldNamingStrategy();

    Class<?> current = clazz;
    while (current != null && current != Object.class) {
      if (current.isRecord()) {
        for (RecordComponent rc : current.getRecordComponents()) {
          Field ann = rc.getAnnotation(Field.class);
          if (ann == null) {
            try {
              java.lang.reflect.Field bf = current.getDeclaredField(rc.getName());
              ann = bf.getAnnotation(Field.class);
            } catch (NoSuchFieldException ignored) {
            }
          }
          if (ann == null) continue;

          // Skip if also has @SolrField (higher-priority factory handles it)
          if (rc.isAnnotationPresent(SolrField.class)) continue;

          String solrName = resolveName(ann, rc.getName(), naming);
          TypeAdapter<?> adapter = dobby.getAdapter(TypeToken.of(rc.getGenericType()));
          Method accessor = rc.getAccessor();
          accessor.setAccessible(true);
          result.add(
              new CompatBoundField(
                  rc.getName(),
                  solrName,
                  rc.getType(),
                  ann.child(),
                  adapter,
                  null,
                  null,
                  accessor));
        }
      } else {
        for (java.lang.reflect.Field f : current.getDeclaredFields()) {
          if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers()))
            continue;
          Field ann = f.getAnnotation(Field.class);
          if (ann == null) continue;
          if (f.isAnnotationPresent(SolrField.class)) continue;

          f.setAccessible(true);
          String solrName = resolveName(ann, f.getName(), naming);
          TypeAdapter<?> adapter = dobby.getAdapter(TypeToken.of(f.getGenericType()));
          result.add(
              new CompatBoundField(
                  f.getName(), solrName, f.getType(), ann.child(), adapter, f, null, null));
        }

        Map<String, Method> getters = BeanProperties.getterMap(current);
        for (Method m : current.getDeclaredMethods()) {
          if (Modifier.isStatic(m.getModifiers())) continue;
          Field ann = m.getAnnotation(Field.class);
          if (ann == null) continue;
          if (m.isAnnotationPresent(SolrField.class)) continue;
          if (m.getParameterCount() != 1) continue;

          m.setAccessible(true);
          String javaName = BeanProperties.propertyNameFromSetter(m);
          String solrName = resolveName(ann, javaName, naming);
          TypeAdapter<?> adapter = dobby.getAdapter(TypeToken.of(m.getGenericParameterTypes()[0]));
          Method getter = getters.get(javaName);
          result.add(
              new CompatBoundField(
                  javaName,
                  solrName,
                  m.getParameterTypes()[0],
                  ann.child(),
                  adapter,
                  null,
                  m,
                  getter));
        }
      }
      current = current.getSuperclass();
    }
    return result;
  }

  private static String resolveName(Field ann, String javaName, FieldNamingStrategy naming) {
    String v = ann.value();
    if (v == null || v.isEmpty() || SOLRJ_DEFAULT.equals(v)) {
      return naming.translateName(javaName);
    }
    return v;
  }

  private static Object defaultPrimitive(Class<?> type) {
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

  private record CompatBoundField(
      String javaName,
      String solrName,
      Class<?> type,
      boolean child,
      TypeAdapter<?> adapter,
      java.lang.reflect.Field field,
      Method setter,
      Method getter) {
    void set(Object target, Object value) {
      try {
        if (field != null) field.set(target, value);
        else if (setter != null) setter.invoke(target, value);
      } catch (Exception e) {
        throw new DobbyException(
            "Failed to set '" + javaName + "' on " + target.getClass().getName(), e);
      }
    }

    Object get(Object target) {
      try {
        if (field != null) return field.get(target);
        if (getter != null) return getter.invoke(target);
        throw new DobbyException(
            "No getter for '" + javaName + "' on " + target.getClass().getName());
      } catch (DobbyException e) {
        throw e;
      } catch (Exception e) {
        throw new DobbyException(
            "Failed to get '" + javaName + "' from " + target.getClass().getName(), e);
      }
    }
  }
}
