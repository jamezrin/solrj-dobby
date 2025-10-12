package com.jamezrin.solrj.dobby;

/**
 * Creates {@link TypeAdapter} instances for specific types.
 *
 * <p>Factories are consulted in registration order by {@link Dobby#getAdapter(TypeToken)}.
 * A factory should return {@code null} from {@link #create} if it does not handle
 * the requested type - the next factory in the chain will then be tried.
 *
 * <p>Factories may call {@code dobby.getAdapter()} to obtain adapters for component types
 * (e.g., a list adapter obtains the adapter for its element type).
 */
@FunctionalInterface
public interface TypeAdapterFactory {

    /**
     * Creates a {@link TypeAdapter} for the given type, or returns {@code null}
     * if this factory does not support it.
     *
     * @param dobby the Dobby instance (for looking up adapters of other types)
     * @param type  the type to create an adapter for
     * @param <T>   the Java type
     * @return a TypeAdapter, or {@code null} to pass to the next factory
     */
    <T> TypeAdapter<T> create(Dobby dobby, TypeToken<T> type);
}
