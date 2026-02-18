package com.jamezrin.solrj.dobby.adapter;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.DobbyUtils;
import com.jamezrin.solrj.dobby.TypeAdapter;
import com.jamezrin.solrj.dobby.TypeAdapterFactory;
import com.jamezrin.solrj.dobby.TypeToken;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Factory for adapters that handle {@link Optional}{@code <T>}.
 *
 * <p>Reading: a {@code null} Solr value becomes {@code Optional.empty()};
 * otherwise the inner adapter is used and wrapped in {@code Optional.of()}.
 *
 * <p>Writing: {@code Optional.empty()} becomes {@code null};
 * otherwise the inner value is written via the inner adapter.
 */
public final class OptionalAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Dobby dobby, TypeToken<T> type) {
        if (type.getRawType() != Optional.class) {
            return null;
        }

        Type innerType = getOptionalInnerType(type);
        TypeAdapter<?> innerAdapter = dobby.getAdapter(TypeToken.of(innerType));
        return DobbyUtils.uncheckedCast(new OptionalAdapter<>(innerAdapter));
    }

    private static Type getOptionalInnerType(TypeToken<?> type) {
        Type fullType = type.getType();
        if (fullType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length == 1) {
                return args[0];
            }
        }
        return Object.class;
    }

    private static final class OptionalAdapter<E> extends TypeAdapter<Optional<E>> {
        private final TypeAdapter<E> innerAdapter;

        OptionalAdapter(TypeAdapter<?> innerAdapter) {
            this.innerAdapter = DobbyUtils.uncheckedCast(innerAdapter);
        }

        @Override
        public Optional<E> read(Object solrValue) {
            if (solrValue == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(innerAdapter.read(solrValue));
        }

        @Override
        public Object write(Optional<E> value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return innerAdapter.write(value.get());
        }

        @Override
        public String toString() {
            return "OptionalAdapter[" + innerAdapter + "]";
        }
    }
}
