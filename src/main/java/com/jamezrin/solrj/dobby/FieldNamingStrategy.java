package com.jamezrin.solrj.dobby;

import java.util.Locale;

/**
 * Strategy for translating a Java field name to a Solr field name. Applied only when no explicit
 * name is given in the annotation.
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

  /** Uses the Java field name as-is. */
  FieldNamingStrategy IDENTITY = fieldName -> fieldName;

  /**
   * Converts camelCase to lower_underscore (snake_case), correctly handling acronyms.
   *
   * <p>An underscore is inserted before an uppercase letter when:
   *
   * <ul>
   *   <li>the preceding character is lowercase (start of a new word), or
   *   <li>the following character is lowercase and the preceding character is uppercase (transition
   *       from an acronym to a new word, e.g. the {@code F} in {@code URLField}).
   * </ul>
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>{@code createdAt} → {@code created_at}
   *   <li>{@code myURLField} → {@code my_url_field}
   *   <li>{@code HTTPSRequest} → {@code https_request}
   *   <li>{@code URL} → {@code url}
   * </ul>
   */
  FieldNamingStrategy LOWER_UNDERSCORE =
      fieldName -> {
        if (fieldName == null || fieldName.isEmpty()) return fieldName;
        StringBuilder sb = new StringBuilder();
        int len = fieldName.length();
        for (int i = 0; i < len; i++) {
          char c = fieldName.charAt(i);
          if (Character.isUpperCase(c)) {
            boolean prevLower = i > 0 && Character.isLowerCase(fieldName.charAt(i - 1));
            boolean prevUpper = i > 0 && Character.isUpperCase(fieldName.charAt(i - 1));
            boolean nextLower = i + 1 < len && Character.isLowerCase(fieldName.charAt(i + 1));
            if (prevLower || (prevUpper && nextLower)) {
              sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
          } else {
            sb.append(c);
          }
        }
        return sb.toString();
      };

  /** Converts the field name to lower case. */
  FieldNamingStrategy LOWER_CASE = fieldName -> fieldName.toLowerCase(Locale.ROOT);
}
