package com.jamezrin.solrj.dobby;

import java.util.Collection;

/**
 * Utility class for unchecked type casts.
 *
 * <p>Centralizes {@code @SuppressWarnings("unchecked")} annotations to minimize
 * their spread throughout the codebase.
 */
public final class DobbyUtils {

    private DobbyUtils() {
        // Utility class
    }

    /**
     * Performs an unchecked cast.
     *
     * <p>This method is intended for internal use where type safety is already
     * guaranteed by other means (e.g., TypeToken, Class.cast(), or runtime checks).
     *
     * @param object the object to cast
     * @param <T>    the target type
     * @return the cast object
     */
    @SuppressWarnings("unchecked")
    public static <T> T uncheckedCast(Object object) {
        return (T) object;
    }

    /**
     * Returns the class of the first non-null element in a collection.
     *
     * @param objects the collection to inspect
     * @param <T>     the element type
     * @return the class of the first non-null element, or null if all elements are null
     */
    public static <T> Class<T> getElementType(Collection<T> objects) {
        for (T obj : objects) {
            if (obj != null) {
                return uncheckedCast(obj.getClass());
            }
        }
        return null;
    }
}
