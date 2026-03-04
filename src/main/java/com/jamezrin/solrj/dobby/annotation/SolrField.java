package com.jamezrin.solrj.dobby.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field, setter method, or record component as mapped to a Solr document field.
 *
 * <p>Example:
 *
 * <pre>{@code
 * public class Product {
 *     @SolrField("id")
 *     private String id;
 *
 *     @SolrField("name_s")
 *     private String name;
 *
 *     @SolrField(value = "variants", nested = true)
 *     private List<Variant> variants;
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface SolrField {

  /**
   * The Solr field name. If empty (the default), the name is derived from the Java field name (or
   * setter/record-component name) and the active {@link
   * com.jamezrin.solrj.dobby.FieldNamingStrategy}.
   */
  String value() default "";

  /**
   * Whether this field maps to nested/child documents rather than a simple Solr field value.
   *
   * <p>When reading, Dobby looks for both named field values that are {@code SolrDocument}
   * instances and entries in {@code getChildDocuments()}.
   *
   * <p>Unlike SolrJ's {@code @Field(child=true)}, multiple fields may be marked as nested.
   */
  boolean nested() default false;
}
