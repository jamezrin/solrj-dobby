package com.jamezrin.solrj.dobby.adapter;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.DobbyException;
import com.jamezrin.solrj.dobby.DobbyUtils;
import com.jamezrin.solrj.dobby.TypeAdapter;
import com.jamezrin.solrj.dobby.TypeAdapterFactory;
import com.jamezrin.solrj.dobby.TypeToken;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factory for adapters that handle {@link Map}{@code <String, V>} types.
 *
 * <p>This is primarily useful for Solr dynamic fields, where the field names
 * are not known at compile time and are captured in a map.
 *
 * <p>Only maps with {@code String} keys are supported (Solr field names are always strings).
 */
public final class MapAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Dobby dobby, TypeToken<T> type) {
        Class<?> raw = type.getRawType();
        if (!Map.class.isAssignableFrom(raw)) {
            return null;
        }

        Type[] keyValueTypes = getMapKeyValueTypes(type);
        Type keyType = keyValueTypes[0];
        Type valueType = keyValueTypes[1];

        // Only String keys are supported
        if (TypeToken.getRawType(keyType) != String.class) {
            throw new DobbyException(
                    "Dobby only supports Map<String, V> for Solr field mapping, but got key type: " + keyType);
        }

        TypeAdapter<?> valueAdapter = dobby.getAdapter(TypeToken.of(valueType));
        return DobbyUtils.uncheckedCast(new MapAdapter<>(valueAdapter));
    }

    private static Type[] getMapKeyValueTypes(TypeToken<?> type) {
        Type fullType = type.getType();
        if (fullType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length == 2) {
                return args;
            }
        }
        return new Type[]{String.class, Object.class};
    }

    private static final class MapAdapter<V> extends TypeAdapter<Map<String, V>> {
        private final TypeAdapter<V> valueAdapter;

        MapAdapter(TypeAdapter<?> valueAdapter) {
            this.valueAdapter = DobbyUtils.uncheckedCast(valueAdapter);
        }

        @Override
        public Map<String, V> read(Object solrValue) {
            if (solrValue == null) return null;
            if (solrValue instanceof Map<?, ?> map) {
                Map<String, V> result = new LinkedHashMap<>(map.size());
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String key = entry.getKey().toString();
                    V value = valueAdapter.read(entry.getValue());
                    result.put(key, value);
                }
                return result;
            }
            throw new DobbyException("Cannot convert " + solrValue.getClass().getName() + " to Map");
        }

        @Override
        public Object write(Map<String, V> value) {
            if (value == null) return null;
            Map<String, Object> result = new LinkedHashMap<>(value.size());
            for (Map.Entry<String, V> entry : value.entrySet()) {
                result.put(entry.getKey(), valueAdapter.write(entry.getValue()));
            }
            return result;
        }

        @Override
        public String toString() {
            return "MapAdapter[String -> " + valueAdapter + "]";
        }
    }
}
