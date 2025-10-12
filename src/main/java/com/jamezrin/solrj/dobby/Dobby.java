package com.jamezrin.solrj.dobby;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The main entry point for converting between Java objects and Solr documents.
 *
 * <p>A {@code Dobby} instance is <b>immutable and thread-safe</b>. Create one via
 * {@link #builder()} and reuse it throughout your application.
 *
 * <p>Example:
 * <pre>{@code
 * Dobby dobby = Dobby.builder()
 *     .registerAdapter(Money.class, new MoneyAdapter())
 *     .build();
 *
 * Product p = dobby.fromDoc(solrDoc, Product.class);
 * SolrInputDocument doc = dobby.toDoc(p);
 * }</pre>
 */
public final class Dobby {

    private final List<TypeAdapterFactory> factories;
    private final FieldNamingStrategy fieldNamingStrategy;
    private final Map<TypeToken<?>, TypeAdapter<?>> adapterCache = new ConcurrentHashMap<>();

    Dobby(List<TypeAdapterFactory> factories, FieldNamingStrategy fieldNamingStrategy) {
        this.factories = Collections.unmodifiableList(new ArrayList<>(factories));
        this.fieldNamingStrategy = Objects.requireNonNull(fieldNamingStrategy);
    }

    /**
     * Creates a new {@link DobbyBuilder}.
     */
    public static DobbyBuilder builder() {
        return new DobbyBuilder();
    }

    /**
     * Converts a {@link SolrDocument} to a Java object of the given type.
     *
     * @param doc  the Solr document
     * @param type the target Java class
     * @param <T>  the target type
     * @return the converted Java object
     * @throws DobbyException if conversion fails
     */
    public <T> T fromDoc(SolrDocument doc, Class<T> type) {
        Objects.requireNonNull(doc, "doc");
        Objects.requireNonNull(type, "type");
        TypeAdapter<T> adapter = getAdapter(type);
        return adapter.read(doc);
    }

    /**
     * Converts a collection of {@link SolrDocument}s to a list of Java objects.
     *
     * @param docs the Solr documents (typically a {@link SolrDocumentList})
     * @param type the target Java class
     * @param <T>  the target type
     * @return a list of converted Java objects
     * @throws DobbyException if conversion fails
     */
    public <T> List<T> fromDocs(Collection<SolrDocument> docs, Class<T> type) {
        Objects.requireNonNull(docs, "docs");
        Objects.requireNonNull(type, "type");
        TypeAdapter<T> adapter = getAdapter(type);
        List<T> result = new ArrayList<>(docs.size());
        for (SolrDocument doc : docs) {
            result.add(adapter.read(doc));
        }
        return result;
    }

    /**
     * Converts a Java object to a {@link SolrInputDocument}.
     *
     * @param obj the Java object to convert
     * @return the Solr input document
     * @throws DobbyException if conversion fails
     */
    public SolrInputDocument toDoc(Object obj) {
        Objects.requireNonNull(obj, "obj");
        @SuppressWarnings("unchecked")
        TypeAdapter<Object> adapter = (TypeAdapter<Object>) getAdapter(TypeToken.of(obj.getClass()));
        Object result = adapter.write(obj);
        if (result instanceof SolrInputDocument sid) {
            return sid;
        }
        throw new DobbyException(
                "TypeAdapter for " + obj.getClass().getName()
                        + " did not produce a SolrInputDocument (got " + (result == null ? "null" : result.getClass().getName()) + ")."
                        + " Only bean/record types can be converted to SolrInputDocument.");
    }

    /**
     * Returns the {@link TypeAdapter} for the given class.
     *
     * @param type the Java class
     * @param <T>  the Java type
     * @return the adapter
     * @throws DobbyException if no adapter is found
     */
    public <T> TypeAdapter<T> getAdapter(Class<T> type) {
        return getAdapter(TypeToken.of(type));
    }

    /**
     * Returns the {@link TypeAdapter} for the given type token.
     *
     * @param type the type token
     * @param <T>  the Java type
     * @return the adapter
     * @throws DobbyException if no adapter is found
     */
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> getAdapter(TypeToken<T> type) {
        Objects.requireNonNull(type, "type");

        TypeAdapter<?> cached = adapterCache.get(type);
        if (cached != null) {
            return (TypeAdapter<T>) cached;
        }

        // To handle recursive types, we use a FutureTypeAdapter that will be resolved later.
        // This prevents infinite recursion when, e.g., a POJO has a field of its own type.
        FutureTypeAdapter<T> future = new FutureTypeAdapter<>();
        adapterCache.put(type, future);

        try {
            for (TypeAdapterFactory factory : factories) {
                TypeAdapter<T> adapter = factory.create(this, type);
                if (adapter != null) {
                    future.setDelegate(adapter);
                    adapterCache.put(type, adapter);
                    return adapter;
                }
            }
        } catch (Exception e) {
            adapterCache.remove(type);
            if (e instanceof DobbyException de) throw de;
            throw new DobbyException("Failed to create adapter for " + type, e);
        }

        adapterCache.remove(type);
        throw new DobbyException("No TypeAdapter found for " + type
                + ". Register one via DobbyBuilder.registerAdapter() or DobbyBuilder.registerAdapterFactory().");
    }

    /**
     * Returns the active {@link FieldNamingStrategy}.
     */
    public FieldNamingStrategy getFieldNamingStrategy() {
        return fieldNamingStrategy;
    }

    /**
     * Returns the ordered list of factories.
     */
    List<TypeAdapterFactory> getFactories() {
        return factories;
    }

    /**
     * A placeholder adapter that delegates to a real adapter once resolved.
     * Used to break circular references during adapter creation.
     */
    private static final class FutureTypeAdapter<T> extends TypeAdapter<T> {
        private TypeAdapter<T> delegate;

        void setDelegate(TypeAdapter<T> delegate) {
            this.delegate = delegate;
        }

        private TypeAdapter<T> delegate() {
            if (delegate == null) {
                throw new DobbyException("TypeAdapter is not yet resolved - circular dependency?");
            }
            return delegate;
        }

        @Override
        public T read(Object solrValue) {
            return delegate().read(solrValue);
        }

        @Override
        public Object write(T value) {
            return delegate().write(value);
        }
    }
}
