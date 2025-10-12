package com.jamezrin.solrj.dobby;

import com.jamezrin.solrj.dobby.adapter.ArrayAdapterFactory;
import com.jamezrin.solrj.dobby.adapter.CollectionAdapterFactory;
import com.jamezrin.solrj.dobby.adapter.EnumAdapterFactory;
import com.jamezrin.solrj.dobby.adapter.JavaTimeAdapterFactory;
import com.jamezrin.solrj.dobby.adapter.MapAdapterFactory;
import com.jamezrin.solrj.dobby.adapter.OptionalAdapterFactory;
import com.jamezrin.solrj.dobby.adapter.PrimitiveAdapterFactory;
import com.jamezrin.solrj.dobby.adapter.ReflectiveAdapterFactory;
import com.jamezrin.solrj.dobby.compat.SolrJCompatAdapterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for creating {@link Dobby} instances.
 *
 * <p>Example:
 * <pre>{@code
 * Dobby dobby = Dobby.builder()
 *     .registerAdapter(Money.class, new MoneyAdapter())
 *     .registerAdapterFactory(new CustomFactory())
 *     .fieldNamingStrategy(FieldNamingStrategy.LOWER_UNDERSCORE)
 *     .build();
 * }</pre>
 */
public final class DobbyBuilder {

    private final List<TypeAdapterFactory> userFactories = new ArrayList<>();
    private FieldNamingStrategy fieldNamingStrategy = FieldNamingStrategy.IDENTITY;
    private boolean solrJCompat = true;

    DobbyBuilder() {
    }

    /**
     * Registers a {@link TypeAdapter} for a specific type.
     * User-registered adapters take highest priority.
     *
     * @param type    the Java class this adapter handles
     * @param adapter the adapter
     * @param <T>     the Java type
     * @return this builder
     */
    public <T> DobbyBuilder registerAdapter(Class<T> type, TypeAdapter<T> adapter) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(adapter, "adapter");
        return registerAdapterFactory(singleTypeFactory(TypeToken.of(type), adapter));
    }

    /**
     * Registers a {@link TypeAdapter} for a specific generic type.
     *
     * @param type    the type token
     * @param adapter the adapter
     * @return this builder
     */
    public DobbyBuilder registerAdapter(TypeToken<?> type, TypeAdapter<?> adapter) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(adapter, "adapter");
        return registerAdapterFactory(singleTypeFactory(type, adapter));
    }

    /**
     * Registers a {@link TypeAdapterFactory}.
     * User-registered factories are consulted before built-in ones, in registration order.
     *
     * @param factory the factory
     * @return this builder
     */
    public DobbyBuilder registerAdapterFactory(TypeAdapterFactory factory) {
        Objects.requireNonNull(factory, "factory");
        userFactories.add(factory);
        return this;
    }

    /**
     * Sets the {@link FieldNamingStrategy} used when no explicit Solr field name
     * is provided in the annotation. Defaults to {@link FieldNamingStrategy#IDENTITY}.
     *
     * @param strategy the naming strategy
     * @return this builder
     */
    public DobbyBuilder fieldNamingStrategy(FieldNamingStrategy strategy) {
        this.fieldNamingStrategy = Objects.requireNonNull(strategy, "strategy");
        return this;
    }

    /**
     * Enables or disables automatic support for SolrJ's {@code @Field} annotation.
     * Enabled by default.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @return this builder
     */
    public DobbyBuilder solrJCompat(boolean enabled) {
        this.solrJCompat = enabled;
        return this;
    }

    /**
     * Builds an immutable {@link Dobby} instance with the configured adapters and settings.
     *
     * @return a new Dobby instance
     */
    public Dobby build() {
        List<TypeAdapterFactory> factories = new ArrayList<>();

        // 1. User-registered adapters/factories (highest priority)
        factories.addAll(userFactories);

        // 2. Built-in adapter factories
        factories.add(new PrimitiveAdapterFactory());
        factories.add(new JavaTimeAdapterFactory());
        factories.add(new EnumAdapterFactory());
        factories.add(new CollectionAdapterFactory());
        factories.add(new ArrayAdapterFactory());
        factories.add(new MapAdapterFactory());
        factories.add(new OptionalAdapterFactory());

        // 3. Reflective adapter for @SolrField-annotated POJOs and records
        factories.add(new ReflectiveAdapterFactory());

        // 4. SolrJ @Field compat (lowest priority among reflective)
        if (solrJCompat) {
            factories.add(new SolrJCompatAdapterFactory());
        }

        return new Dobby(factories, fieldNamingStrategy);
    }

    /**
     * Creates a factory that matches a single exact type.
     */
    private static TypeAdapterFactory singleTypeFactory(TypeToken<?> typeToken, TypeAdapter<?> adapter) {
        return new TypeAdapterFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> TypeAdapter<T> create(Dobby dobby, TypeToken<T> type) {
                if (type.equals(typeToken)) {
                    return (TypeAdapter<T>) adapter;
                }
                // Also match raw class for convenience (e.g., registering for Integer.class should match int)
                if (type.getRawType() == typeToken.getRawType() && typeToken.getType() instanceof Class) {
                    return (TypeAdapter<T>) adapter;
                }
                return null;
            }
        };
    }
}
