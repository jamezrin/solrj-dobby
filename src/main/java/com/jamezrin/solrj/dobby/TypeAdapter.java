package com.jamezrin.solrj.dobby;

/**
 * Converts between a Java type {@code T} and its Solr representation.
 *
 * <p>For <b>simple types</b> (String, Integer, Enum, Instant, etc.),
 * {@link #read(Object)} receives the raw Solr field value and {@link #write(Object)}
 * returns a raw value suitable for {@link org.apache.solr.common.SolrInputDocument#setField}.
 *
 * <p>For <b>bean/record types</b>, {@link #read(Object)} receives a
 * {@link org.apache.solr.common.SolrDocument} and {@link #write(Object)} returns a
 * {@link org.apache.solr.common.SolrInputDocument}. This uniformity is what makes the
 * adapter chain composable - a {@code ListTypeAdapter} wraps an element adapter, a
 * reflective adapter delegates to per-field adapters, etc.
 *
 * @param <T> the Java type this adapter handles
 */
public abstract class TypeAdapter<T> {

    /**
     * Converts a Solr value to a Java object of type {@code T}.
     *
     * @param solrValue the value from Solr (may be a raw field value or a {@code SolrDocument})
     * @return the converted Java object, or {@code null} if the input is {@code null}
     */
    public abstract T read(Object solrValue);

    /**
     * Converts a Java object of type {@code T} to a Solr value.
     *
     * @param value the Java object to convert
     * @return the Solr-compatible value (raw field value or {@code SolrInputDocument}),
     *         or {@code null} if the input is {@code null}
     */
    public abstract Object write(T value);

    /**
     * Returns a null-safe wrapper around this adapter.
     * The returned adapter passes through {@code null} without delegating.
     */
    public final TypeAdapter<T> nullSafe() {
        final TypeAdapter<T> delegate = this;
        return new TypeAdapter<>() {
            @Override
            public T read(Object solrValue) {
                return solrValue == null ? null : delegate.read(solrValue);
            }

            @Override
            public Object write(T value) {
                return value == null ? null : delegate.write(value);
            }

            @Override
            public String toString() {
                return delegate + ".nullSafe()";
            }
        };
    }
}
