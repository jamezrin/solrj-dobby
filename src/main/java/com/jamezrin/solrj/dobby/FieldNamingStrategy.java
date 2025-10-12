package com.jamezrin.solrj.dobby;

import java.util.Locale;

/**
 * Strategy for translating a Java field name to a Solr field name.
 * Applied only when no explicit name is given in the annotation.
 */
@FunctionalInterface
public interface FieldNamingStrategy {

    /**
     * Translates a Java field name to the corresponding Solr field name.
     *
     * @param fieldName the Java field or record-component name
     * @return the Solr field name
     */
    String translateName(String fieldName);

    /**
     * Uses the Java field name as-is.
     */
    FieldNamingStrategy IDENTITY = fieldName -> fieldName;

    /**
     * Converts camelCase to lower_underscore (snake_case).
     * <p>Example: {@code createdAt} → {@code created_at}
     */
    FieldNamingStrategy LOWER_UNDERSCORE = fieldName -> {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    };

    /**
     * Converts the field name to lower case.
     */
    FieldNamingStrategy LOWER_CASE = fieldName -> fieldName.toLowerCase(Locale.ROOT);
}
