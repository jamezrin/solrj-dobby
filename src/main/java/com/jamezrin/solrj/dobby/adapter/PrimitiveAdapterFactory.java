package com.jamezrin.solrj.dobby.adapter;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.DobbyException;
import com.jamezrin.solrj.dobby.TypeAdapter;
import com.jamezrin.solrj.dobby.TypeAdapterFactory;
import com.jamezrin.solrj.dobby.TypeToken;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

/**
 * Factory for adapters that handle Java primitive types, their wrappers,
 * {@code String}, {@code byte[]}, and {@code ByteBuffer}.
 *
 * <p>Solr already returns most of these types natively, so these adapters
 * mainly perform type coercion (e.g. Solr returns {@code Long} but your field is {@code Integer}).
 */
public final class PrimitiveAdapterFactory implements TypeAdapterFactory {

    private static final Set<Class<?>> SUPPORTED = Set.of(
            Object.class,
            String.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            short.class, Short.class,
            byte[].class,
            ByteBuffer.class
    );

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Dobby dobby, TypeToken<T> type) {
        Class<?> raw = type.getRawType();
        if (!SUPPORTED.contains(raw)) {
            return null;
        }
        return (TypeAdapter<T>) createAdapter(raw);
    }

    private TypeAdapter<?> createAdapter(Class<?> raw) {
        if (raw == Object.class) return OBJECT_ADAPTER;
        if (raw == String.class) return STRING_ADAPTER;
        if (raw == int.class || raw == Integer.class) return INT_ADAPTER;
        if (raw == long.class || raw == Long.class) return LONG_ADAPTER;
        if (raw == float.class || raw == Float.class) return FLOAT_ADAPTER;
        if (raw == double.class || raw == Double.class) return DOUBLE_ADAPTER;
        if (raw == boolean.class || raw == Boolean.class) return BOOLEAN_ADAPTER;
        if (raw == byte.class || raw == Byte.class) return BYTE_ADAPTER;
        if (raw == short.class || raw == Short.class) return SHORT_ADAPTER;
        if (raw == byte[].class) return BYTE_ARRAY_ADAPTER;
        if (raw == ByteBuffer.class) return BYTE_BUFFER_ADAPTER;
        return null;
    }

    // --- Adapter instances ---

    /**
     * Pass-through adapter for {@code Object.class}. Used when the value type
     * is not known at compile time (e.g. {@code Map<String, Object>}).
     * Returns values as-is without any conversion.
     */
    private static final TypeAdapter<Object> OBJECT_ADAPTER = new TypeAdapter<Object>() {
        @Override
        public Object read(Object solrValue) {
            return solrValue;
        }

        @Override
        public Object write(Object value) {
            return value;
        }

        @Override
        public String toString() {
            return "PrimitiveAdapter[Object]";
        }
    };

    private static final TypeAdapter<String> STRING_ADAPTER = new TypeAdapter<String>() {
        @Override
        public String read(Object solrValue) {
            if (solrValue == null) return null;
            return solrValue.toString();
        }

        @Override
        public Object write(String value) {
            return value;
        }

        @Override
        public String toString() {
            return "PrimitiveAdapter[String]";
        }
    };

    private static final TypeAdapter<Integer> INT_ADAPTER = new TypeAdapter<Integer>() {
        @Override
        public Integer read(Object solrValue) {
            if (solrValue == null) return null;
            if (solrValue instanceof Number n) return n.intValue();
            if (solrValue instanceof String s) return Integer.parseInt(s);
            throw new DobbyException("Cannot convert " + solrValue.getClass().getName() + " to Integer");
        }

        @Override
        public Object write(Integer value) {
            return value;
        }

        @Override
        public String toString() {
            return "PrimitiveAdapter[Integer]";
        }
    };

    private static final TypeAdapter<Long> LONG_ADAPTER = new TypeAdapter<Long>() {
        @Override
        public Long read(Object solrValue) {
            if (solrValue == null) return null;
            if (solrValue instanceof Number n) return n.longValue();
            if (solrValue instanceof String s) return Long.parseLong(s);
            throw new DobbyException("Cannot convert " + solrValue.getClass().getName() + " to Long");
        }

        @Override
        public Object write(Long value) {
            return value;
        }

        @Override
        public String toString() {
            return "PrimitiveAdapter[Long]";
        }
    };

    private static final TypeAdapter<Float> FLOAT_ADAPTER = new TypeAdapter<Float>() {
        @Override
        public Float read(Object solrValue) {
            if (solrValue == null) return null;
            if (solrValue instanceof Number n) return n.floatValue();
            if (solrValue instanceof String s) return Float.parseFloat(s);
            throw new DobbyException("Cannot convert " + solrValue.getClass().getName() + " to Float");
        }

        @Override
        public Object write(Float value) {
            return value;
        }

        @Override
        public String toString() {
            return "PrimitiveAdapter[Float]";
        }
    };

    private static final TypeAdapter<Double> DOUBLE_ADAPTER = new TypeAdapter<Double>() {
        @Override
        public Double read(Object solrValue) {
            if (solrValue == null) return null;
            if (solrValue instanceof Number n) return n.doubleValue();
            if (solrValue instanceof String s) return Double.parseDouble(s);
            throw new DobbyException("Cannot convert " + solrValue.getClass().getName() + " to Double");
        }

        @Override
        public Object write(Double value) {
            return value;
        }

        @Override
        public String toString() {
            return "PrimitiveAdapter[Double]";
        }
    };

    private static final TypeAdapter<Boolean> BOOLEAN_ADAPTER = new TypeAdapter<Boolean>() {
        @Override
        public Boolean read(Object solrValue) {
            if (solrValue == null) return null;
            if (solrValue instanceof Boolean b) return b;
            if (solrValue instanceof String s) return Boolean.parseBoolean(s);
            if (solrValue instanceof Number n) return n.intValue() != 0;
            throw new DobbyException("Cannot convert " + solrValue.getClass().getName() + " to Boolean");
        }

        @Override
        public Object write(Boolean value) {
            return value;
        }

        @Override
        public String toString() {
            return "PrimitiveAdapter[Boolean]";
        }
    };

    private static final TypeAdapter<Byte> BYTE_ADAPTER = new TypeAdapter<Byte>() {
        @Override
        public Byte read(Object solrValue) {
            if (solrValue == null) return null;
            if (solrValue instanceof Number n) return n.byteValue();
            if (solrValue instanceof String s) return Byte.parseByte(s);
            throw new DobbyException("Cannot convert " + solrValue.getClass().getName() + " to Byte");
        }

        @Override
        public Object write(Byte value) {
            return value;
        }

        @Override
        public String toString() {
            return "PrimitiveAdapter[Byte]";
        }
    };

    private static final TypeAdapter<Short> SHORT_ADAPTER = new TypeAdapter<Short>() {
        @Override
        public Short read(Object solrValue) {
            if (solrValue == null) return null;
            if (solrValue instanceof Number n) return n.shortValue();
            if (solrValue instanceof String s) return Short.parseShort(s);
            throw new DobbyException("Cannot convert " + solrValue.getClass().getName() + " to Short");
        }

        @Override
        public Object write(Short value) {
            return value;
        }

        @Override
        public String toString() {
            return "PrimitiveAdapter[Short]";
        }
    };

    private static final TypeAdapter<byte[]> BYTE_ARRAY_ADAPTER = new TypeAdapter<byte[]>() {
        @Override
        public byte[] read(Object solrValue) {
            if (solrValue == null) return null;
            if (solrValue instanceof byte[] b) return b;
            if (solrValue instanceof ByteBuffer bb) {
                byte[] arr = new byte[bb.remaining()];
                bb.get(arr);
                return arr;
            }
            throw new DobbyException("Cannot convert " + solrValue.getClass().getName() + " to byte[]");
        }

        @Override
        public Object write(byte[] value) {
            return value;
        }

        @Override
        public String toString() {
            return "PrimitiveAdapter[byte[]]";
        }
    };

    private static final TypeAdapter<ByteBuffer> BYTE_BUFFER_ADAPTER = new TypeAdapter<ByteBuffer>() {
        @Override
        public ByteBuffer read(Object solrValue) {
            if (solrValue == null) return null;
            if (solrValue instanceof ByteBuffer bb) return bb;
            if (solrValue instanceof byte[] b) return ByteBuffer.wrap(b);
            throw new DobbyException("Cannot convert " + solrValue.getClass().getName() + " to ByteBuffer");
        }

        @Override
        public Object write(ByteBuffer value) {
            return value;
        }

        @Override
        public String toString() {
            return "PrimitiveAdapter[ByteBuffer]";
        }
    };
}
