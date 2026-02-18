package com.jamezrin.solrj.dobby.compat;

import java.util.List;

import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import com.jamezrin.solrj.dobby.Dobby;

/**
 * A {@link DocumentObjectBinder} implementation that delegates to a Dobby instance.
 *
 * <p>This allows drop-in replacement of SolrJ's DocumentObjectBinder with Dobby's enhanced
 * capabilities (records, java.time, enums, custom adapters, etc.) without changing existing code
 * that depends on the DocumentObjectBinder API.
 *
 * <p>Example migration:
 *
 * <pre>{@code
 * // Before:
 * DocumentObjectBinder binder = new DocumentObjectBinder();
 *
 * // After (drop-in replacement):
 * DocumentObjectBinder binder = new DobbyDocumentObjectBinder();
 * }</pre>
 *
 * <p>For more control over the Dobby configuration, use the constructor that accepts a {@link
 * Dobby} instance:
 *
 * <pre>{@code
 * Dobby dobby = Dobby.builder()
 *     .registerAdapter(Money.class, new MoneyAdapter())
 *     .build();
 * DocumentObjectBinder binder = new DobbyDocumentObjectBinder(dobby);
 * }</pre>
 */
public class DobbyDocumentObjectBinder extends DocumentObjectBinder {

  private final Dobby dobby;

  /** Creates a binder using the default Dobby configuration. */
  public DobbyDocumentObjectBinder() {
    this(Dobby.builder().build());
  }

  /**
   * Creates a binder using the provided Dobby instance.
   *
   * @param dobby the Dobby instance to delegate to
   */
  public DobbyDocumentObjectBinder(Dobby dobby) {
    this.dobby = dobby;
  }

  /**
   * Converts a list of Solr documents to Java objects.
   *
   * @param clazz the target Java class
   * @param solrDocList the Solr documents
   * @param <T> the target type
   * @return a list of converted Java objects
   */
  @Override
  public <T> List<T> getBeans(Class<T> clazz, SolrDocumentList solrDocList) {
    return dobby.fromDocs(solrDocList, clazz);
  }

  /**
   * Converts a Solr document to a Java object.
   *
   * @param clazz the target Java class
   * @param solrDoc the Solr document
   * @param <T> the target type
   * @return the converted Java object
   */
  @Override
  public <T> T getBean(Class<T> clazz, SolrDocument solrDoc) {
    return dobby.fromDoc(solrDoc, clazz);
  }

  /**
   * Converts a Java object to a SolrInputDocument.
   *
   * @param obj the Java object to convert
   * @return the Solr input document
   */
  @Override
  public SolrInputDocument toSolrInputDocument(Object obj) {
    return dobby.toDoc(obj);
  }
}
