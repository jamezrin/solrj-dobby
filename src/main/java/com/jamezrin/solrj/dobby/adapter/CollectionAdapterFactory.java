package com.jamezrin.solrj.dobby.adapter;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.DobbyException;
import com.jamezrin.solrj.dobby.TypeAdapter;
import com.jamezrin.solrj.dobby.TypeAdapterFactory;
import com.jamezrin.solrj.dobby.TypeToken;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Factory for adapters that handle {@link Collection} types:
 * {@link List}, {@link Set}, {@link Collection}, {@link ArrayList},
 * {@link HashSet}, and {@link LinkedHashSet}.
 *
 * <p>Delegates to the element adapter for each item in the collection.
 * Handles the common Solr case where a single value is returned instead
 * of a list for single-valued multi-value fields.
 */
public final class CollectionAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Dobby dobby, TypeToken<T> type) {
        Class<?> raw = type.getRawType();
        if (!Collection.class.isAssignableFrom(raw)) {
            return null;
        }

        Type elementType = getCollectionElementType(type);
        TypeAdapter<?> elementAdapter = dobby.getAdapter(TypeToken.of(elementType));

        if (List.class.isAssignableFrom(raw) || Collection.class == raw) {
            return (TypeAdapter<T>) new ListAdapter<>(elementAdapter);
        }
        if (Set.class.isAssignableFrom(raw)) {
            return (TypeAdapter<T>) new SetAdapter<>(elementAdapter);
        }

        return null;
    }

    private static Type getCollectionElementType(TypeToken<?> type) {
        Type fullType = type.getType();
        if (fullType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length == 1) {
                return args[0];
            }
        }
        // Fall back to Object if no generic info
        return Object.class;
    }

    private static final class ListAdapter<E> extends TypeAdapter<List<E>> {
        private final TypeAdapter<E> elementAdapter;

        @SuppressWarnings("unchecked")
        ListAdapter(TypeAdapter<?> elementAdapter) {
            this.elementAdapter = (TypeAdapter<E>) elementAdapter;
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<E> read(Object solrValue) {
            if (solrValue == null) return null;
            if (solrValue instanceof Collection<?> coll) {
                List<E> result = new ArrayList<>(coll.size());
                for (Object item : coll) {
                    result.add(elementAdapter.read(item));
                }
                return result;
            }
            // Solr sometimes returns a single value for a multi-valued field
            List<E> result = new ArrayList<>(1);
            result.add(elementAdapter.read(solrValue));
            return result;
        }

        @Override
        public Object write(List<E> value) {
            if (value == null) return null;
            List<Object> result = new ArrayList<>(value.size());
            for (E item : value) {
                result.add(elementAdapter.write(item));
            }
            return result;
        }

        @Override
        public String toString() {
            return "CollectionAdapter[List<" + elementAdapter + ">]";
        }
    }

    private static final class SetAdapter<E> extends TypeAdapter<Set<E>> {
        private final TypeAdapter<E> elementAdapter;

        @SuppressWarnings("unchecked")
        SetAdapter(TypeAdapter<?> elementAdapter) {
            this.elementAdapter = (TypeAdapter<E>) elementAdapter;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<E> read(Object solrValue) {
            if (solrValue == null) return null;
            if (solrValue instanceof Collection<?> coll) {
                Set<E> result = new LinkedHashSet<>(coll.size());
                for (Object item : coll) {
                    result.add(elementAdapter.read(item));
                }
                return result;
            }
            Set<E> result = new LinkedHashSet<>(1);
            result.add(elementAdapter.read(solrValue));
            return result;
        }

        @Override
        public Object write(Set<E> value) {
            if (value == null) return null;
            List<Object> result = new ArrayList<>(value.size());
            for (E item : value) {
                result.add(elementAdapter.write(item));
            }
            return result;
        }

        @Override
        public String toString() {
            return "CollectionAdapter[Set<" + elementAdapter + ">]";
        }
    }
}
