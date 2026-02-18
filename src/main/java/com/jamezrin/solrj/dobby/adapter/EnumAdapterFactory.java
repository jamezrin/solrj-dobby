package com.jamezrin.solrj.dobby.adapter;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.DobbyException;
import com.jamezrin.solrj.dobby.TypeAdapter;
import com.jamezrin.solrj.dobby.TypeAdapterFactory;
import com.jamezrin.solrj.dobby.TypeToken;

/**
 * Factory for adapters that handle {@link Enum} types. Converts between enum constants and their
 * {@link Enum#name()} strings.
 */
public final class EnumAdapterFactory implements TypeAdapterFactory {

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <T> TypeAdapter<T> create(Dobby dobby, TypeToken<T> type) {
    Class<?> raw = type.getRawType();
    if (!raw.isEnum()) {
      return null;
    }
    return (TypeAdapter<T>) new EnumAdapter(raw);
  }

  private static final class EnumAdapter<E extends Enum<E>> extends TypeAdapter<E> {
    private final Class<E> enumClass;

    EnumAdapter(Class<E> enumClass) {
      this.enumClass = enumClass;
    }

    @Override
    public E read(Object solrValue) {
      if (solrValue == null) return null;
      if (enumClass.isInstance(solrValue)) return enumClass.cast(solrValue);
      if (solrValue instanceof String s) {
        try {
          return Enum.valueOf(enumClass, s);
        } catch (IllegalArgumentException e) {
          throw new DobbyException("No enum constant " + enumClass.getName() + "." + s, e);
        }
      }
      // Try ordinal
      if (solrValue instanceof Number n) {
        E[] constants = enumClass.getEnumConstants();
        int ordinal = n.intValue();
        if (ordinal < 0 || ordinal >= constants.length) {
          throw new DobbyException(
              "Enum ordinal " + ordinal + " out of range for " + enumClass.getName());
        }
        return constants[ordinal];
      }
      throw new DobbyException(
          "Cannot convert " + solrValue.getClass().getName() + " to " + enumClass.getName());
    }

    @Override
    public Object write(E value) {
      return value == null ? null : value.name();
    }

    @Override
    public String toString() {
      return "EnumAdapter[" + enumClass.getSimpleName() + "]";
    }
  }
}
