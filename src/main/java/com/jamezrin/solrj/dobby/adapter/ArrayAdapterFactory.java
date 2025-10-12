package com.jamezrin.solrj.dobby.adapter;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.TypeAdapter;
import com.jamezrin.solrj.dobby.TypeAdapterFactory;
import com.jamezrin.solrj.dobby.TypeToken;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Factory for adapters that handle array types ({@code T[]}).
 * Delegates to the component-type adapter for each element.
 *
 * <p>Does <b>not</b> handle {@code byte[]} - that is covered by
 * {@link PrimitiveAdapterFactory}.
 */
public final class ArrayAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Dobby dobby, TypeToken<T> type) {
        Class<?> raw = type.getRawType();
        if (!raw.isArray() || raw == byte[].class) {
            return null;
        }

        Type componentType = getComponentType(type);
        Class<?> componentClass = TypeToken.getRawType(componentType);
        TypeAdapter<?> componentAdapter = dobby.getAdapter(TypeToken.of(componentType));

        return (TypeAdapter<T>) new ArrayAdapter<>(componentAdapter, componentClass);
    }

    private static Type getComponentType(TypeToken<?> type) {
        Type fullType = type.getType();
        if (fullType instanceof GenericArrayType gat) {
            return gat.getGenericComponentType();
        }
        return type.getRawType().getComponentType();
    }

    private static final class ArrayAdapter<E> extends TypeAdapter<Object> {
        private final TypeAdapter<E> componentAdapter;
        private final Class<?> componentClass;

        @SuppressWarnings("unchecked")
        ArrayAdapter(TypeAdapter<?> componentAdapter, Class<?> componentClass) {
            this.componentAdapter = (TypeAdapter<E>) componentAdapter;
            this.componentClass = componentClass;
        }

        @Override
        public Object read(Object solrValue) {
            if (solrValue == null) return null;

            List<?> list;
            if (solrValue instanceof Collection<?> coll) {
                list = coll instanceof List<?> l ? l : new ArrayList<>(coll);
            } else if (solrValue.getClass().isArray()) {
                // Already an array - convert element by element
                int len = Array.getLength(solrValue);
                Object result = Array.newInstance(componentClass, len);
                for (int i = 0; i < len; i++) {
                    Array.set(result, i, componentAdapter.read(Array.get(solrValue, i)));
                }
                return result;
            } else {
                list = List.of(solrValue);
            }

            Object result = Array.newInstance(componentClass, list.size());
            for (int i = 0; i < list.size(); i++) {
                Array.set(result, i, componentAdapter.read(list.get(i)));
            }
            return result;
        }

        @Override
        public Object write(Object value) {
            if (value == null) return null;
            int len = Array.getLength(value);
            List<Object> result = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                @SuppressWarnings("unchecked")
                E element = (E) Array.get(value, i);
                result.add(componentAdapter.write(element));
            }
            return result;
        }

        @Override
        public String toString() {
            return "ArrayAdapter[" + componentClass.getSimpleName() + "[]]";
        }
    }
}
