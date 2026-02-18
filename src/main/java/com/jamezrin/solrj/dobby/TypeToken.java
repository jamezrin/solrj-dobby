package com.jamezrin.solrj.dobby;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Objects;

/**
 * Represents a generic type {@code T}. Because Java erases generic type information at runtime,
 * this class uses the technique of creating an anonymous subclass to capture the type argument.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * TypeToken<List<String>> listType = new TypeToken<List<String>>() {};
 * }</pre>
 *
 * @param <T> the type this token represents
 */
public class TypeToken<T> {

  private final Class<? super T> rawType;
  private final Type type;
  private final int hashCode;

  /**
   * Constructs a new type token. Derives the represented type from the type parameter.
   *
   * <p>Clients should create an anonymous subclass: {@code new TypeToken<List<String>>() {}}
   */
  protected TypeToken() {
    this.type = getSuperclassTypeParameter(getClass());
    this.rawType = DobbyUtils.uncheckedCast(getRawType(this.type));
    this.hashCode = this.type.hashCode();
  }

  /** Creates a type token for the given {@code Class}. */
  private TypeToken(Type type) {
    this.type = canonicalize(type);
    this.rawType = DobbyUtils.uncheckedCast(getRawType(this.type));
    this.hashCode = this.type.hashCode();
  }

  /** Returns a type token for the given {@code Class}. */
  public static <T> TypeToken<T> of(Class<T> type) {
    return new TypeToken<>(type);
  }

  /** Returns a type token for the given {@code Type}. */
  public static TypeToken<?> of(Type type) {
    return new TypeToken<>(type);
  }

  /** Returns the raw (erased) type. */
  public Class<? super T> getRawType() {
    return rawType;
  }

  /** Returns the full type, including generic parameters. */
  public Type getType() {
    return type;
  }

  /** Checks if this type token represents the given raw type. */
  public boolean isAssignableFrom(Class<?> cls) {
    return rawType.isAssignableFrom(cls);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TypeToken<?> that)) return false;
    return typeEquals(this.type, that.type);
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public String toString() {
    return typeToString(type);
  }

  // --- Internal helpers ---

  private static Type getSuperclassTypeParameter(Class<?> subclass) {
    Type superclass = subclass.getGenericSuperclass();
    if (superclass instanceof Class) {
      throw new IllegalArgumentException(
          "TypeToken must be created with a type parameter: new TypeToken<T>() {}");
    }
    ParameterizedType parameterized = (ParameterizedType) superclass;
    return canonicalize(parameterized.getActualTypeArguments()[0]);
  }

  /** Returns the raw type of a given {@code Type}. */
  public static Class<?> getRawType(Type type) {
    Objects.requireNonNull(type, "type");
    if (type instanceof Class<?> cls) {
      return cls;
    } else if (type instanceof ParameterizedType pt) {
      Type rawType = pt.getRawType();
      if (rawType instanceof Class<?> cls) {
        return cls;
      }
      throw new IllegalArgumentException("Expected a Class, but got: " + rawType);
    } else if (type instanceof GenericArrayType gat) {
      Type componentType = gat.getGenericComponentType();
      return Array.newInstance(getRawType(componentType), 0).getClass();
    } else if (type instanceof WildcardType wt) {
      return getRawType(wt.getUpperBounds()[0]);
    } else {
      throw new IllegalArgumentException(
          "Expected a Class, ParameterizedType, GenericArrayType, or WildcardType, but got: "
              + type.getClass().getName());
    }
  }

  static Type canonicalize(Type type) {
    if (type instanceof Class<?> cls && cls.isArray()) {
      return new GenericArrayTypeImpl(canonicalize(cls.getComponentType()));
    } else if (type instanceof ParameterizedType pt) {
      return new ParameterizedTypeImpl(
          pt.getOwnerType(), pt.getRawType(), pt.getActualTypeArguments());
    } else if (type instanceof GenericArrayType gat) {
      return new GenericArrayTypeImpl(gat.getGenericComponentType());
    } else if (type instanceof WildcardType wt) {
      return new WildcardTypeImpl(wt.getUpperBounds(), wt.getLowerBounds());
    }
    return type;
  }

  static boolean typeEquals(Type a, Type b) {
    if (a == b) return true;
    if (a == null || b == null) return false;
    if (a instanceof Class<?>) return a.equals(b);
    if (a instanceof ParameterizedType pa && b instanceof ParameterizedType pb) {
      return Objects.equals(pa.getOwnerType(), pb.getOwnerType())
          && pa.getRawType().equals(pb.getRawType())
          && java.util.Arrays.equals(pa.getActualTypeArguments(), pb.getActualTypeArguments());
    }
    if (a instanceof GenericArrayType ga && b instanceof GenericArrayType gb) {
      return typeEquals(ga.getGenericComponentType(), gb.getGenericComponentType());
    }
    if (a instanceof WildcardType wa && b instanceof WildcardType wb) {
      return java.util.Arrays.equals(wa.getUpperBounds(), wb.getUpperBounds())
          && java.util.Arrays.equals(wa.getLowerBounds(), wb.getLowerBounds());
    }
    return false;
  }

  static String typeToString(Type type) {
    return type instanceof Class<?> cls ? cls.getName() : type.toString();
  }

  // --- Internal type implementations ---

  private record ParameterizedTypeImpl(Type ownerType, Type rawType, Type[] actualTypeArguments)
      implements ParameterizedType {

    ParameterizedTypeImpl(Type ownerType, Type rawType, Type[] actualTypeArguments) {
      this.ownerType = ownerType;
      this.rawType = rawType;
      this.actualTypeArguments = actualTypeArguments.clone();
    }

    @Override
    public Type[] getActualTypeArguments() {
      return actualTypeArguments.clone();
    }

    @Override
    public Type getRawType() {
      return rawType;
    }

    @Override
    public Type getOwnerType() {
      return ownerType;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ParameterizedType that)) return false;
      return TypeToken.typeEquals(this, that);
    }

    @Override
    public int hashCode() {
      return java.util.Arrays.hashCode(actualTypeArguments)
          ^ rawType.hashCode()
          ^ Objects.hashCode(ownerType);
    }
  }

  private record GenericArrayTypeImpl(Type genericComponentType) implements GenericArrayType {

    @Override
    public Type getGenericComponentType() {
      return genericComponentType;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof GenericArrayType that)) return false;
      return TypeToken.typeEquals(genericComponentType, that.getGenericComponentType());
    }

    @Override
    public int hashCode() {
      return genericComponentType.hashCode();
    }
  }

  private record WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) implements WildcardType {

    WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
      this.upperBounds = upperBounds.clone();
      this.lowerBounds = lowerBounds.clone();
    }

    @Override
    public Type[] getUpperBounds() {
      return upperBounds.clone();
    }

    @Override
    public Type[] getLowerBounds() {
      return lowerBounds.clone();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof WildcardType that)) return false;
      return java.util.Arrays.equals(upperBounds, that.getUpperBounds())
          && java.util.Arrays.equals(lowerBounds, that.getLowerBounds());
    }

    @Override
    public int hashCode() {
      return java.util.Arrays.hashCode(upperBounds) ^ java.util.Arrays.hashCode(lowerBounds);
    }
  }
}
