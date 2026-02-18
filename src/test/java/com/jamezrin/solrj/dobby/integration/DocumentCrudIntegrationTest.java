package com.jamezrin.solrj.dobby.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import org.junit.jupiter.api.Test;

import com.jamezrin.solrj.dobby.annotation.SolrField;

/**
 * End-to-end integration tests for document creation and retrieval using Dobby with a real Solr
 * instance.
 */
class DocumentCrudIntegrationTest extends AbstractSolrIntegrationTest {

  @Test
  void createAndRetrieveSimpleDocument() throws Exception {
    SimpleProduct product = new SimpleProduct("prod-1", "Test Product", 99.99);

    SolrInputDocument doc = getDobby().toDoc(product);
    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    SolrQuery query = new SolrQuery("id:prod-1");
    QueryResponse response = getSolrClient().query(getCollectionName(), query);
    SolrDocumentList results = response.getResults();

    assertEquals(1, results.getNumFound());
    SolrDocument solrDoc = results.get(0);

    SimpleProduct retrieved = getDobby().fromDoc(solrDoc, SimpleProduct.class);

    assertEquals(product.id(), retrieved.id());
    assertEquals(product.name(), retrieved.name());
    assertEquals(product.price(), retrieved.price(), 0.001);
  }

  @Test
  void createAndRetrieveMultipleDocuments() throws Exception {
    List<SimpleProduct> products =
        List.of(
            new SimpleProduct("prod-1", "Product One", 10.0),
            new SimpleProduct("prod-2", "Product Two", 20.0),
            new SimpleProduct("prod-3", "Product Three", 30.0));

    List<SolrInputDocument> docs = getDobby().toDocs(products);
    getSolrClient().add(getCollectionName(), docs);
    getSolrClient().commit(getCollectionName());

    SolrQuery query = new SolrQuery("*:*");
    query.setRows(10);
    QueryResponse response = getSolrClient().query(getCollectionName(), query);
    SolrDocumentList results = response.getResults();

    assertEquals(3, results.getNumFound());

    List<SimpleProduct> retrieved = getDobby().fromDocs(results, SimpleProduct.class);

    assertEquals(3, retrieved.size());
    assertTrue(retrieved.stream().anyMatch(p -> "prod-1".equals(p.id())));
    assertTrue(retrieved.stream().anyMatch(p -> "prod-2".equals(p.id())));
    assertTrue(retrieved.stream().anyMatch(p -> "prod-3".equals(p.id())));
  }

  @Test
  void roundTripWithComplexTypes() throws Exception {
    addSchemaField("created_at", "date", false);
    addSchemaField("tags", "string", true);
    addSchemaField("rating", "int", false);

    ComplexProduct product =
        new ComplexProduct(
            "complex-1",
            "Complex Product",
            149.99,
            Instant.parse("2025-01-15T10:30:00Z"),
            List.of("electronics", "premium", "sale"),
            5);

    SolrInputDocument doc = getDobby().toDoc(product);
    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    SolrQuery query = new SolrQuery("id:complex-1");
    QueryResponse response = getSolrClient().query(getCollectionName(), query);
    SolrDocumentList results = response.getResults();

    assertEquals(1, results.getNumFound());

    ComplexProduct retrieved = getDobby().fromDoc(results.get(0), ComplexProduct.class);

    assertEquals(product.id(), retrieved.id());
    assertEquals(product.name(), retrieved.name());
    assertEquals(product.price(), retrieved.price(), 0.001);
    assertEquals(product.createdAt(), retrieved.createdAt());
    assertEquals(product.tags(), retrieved.tags());
    assertEquals(product.rating(), retrieved.rating());
  }

  @Test
  void updateExistingDocument() throws Exception {
    SimpleProduct product = new SimpleProduct("update-1", "Original Name", 50.0);

    SolrInputDocument doc = getDobby().toDoc(product);
    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    SimpleProduct updatedProduct = new SimpleProduct("update-1", "Updated Name", 75.0);
    SolrInputDocument updatedDoc = getDobby().toDoc(updatedProduct);
    getSolrClient().add(getCollectionName(), updatedDoc);
    getSolrClient().commit(getCollectionName());

    SolrQuery query = new SolrQuery("id:update-1");
    QueryResponse response = getSolrClient().query(getCollectionName(), query);
    SolrDocumentList results = response.getResults();

    assertEquals(1, results.getNumFound());

    SimpleProduct retrieved = getDobby().fromDoc(results.get(0), SimpleProduct.class);
    assertEquals("Updated Name", retrieved.name());
    assertEquals(75.0, retrieved.price(), 0.001);
  }

  @Test
  void deleteDocument() throws Exception {
    SimpleProduct product = new SimpleProduct("delete-1", "To Delete", 25.0);

    SolrInputDocument doc = getDobby().toDoc(product);
    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    getSolrClient().deleteById(getCollectionName(), "delete-1");
    getSolrClient().commit(getCollectionName());

    SolrQuery query = new SolrQuery("id:delete-1");
    QueryResponse response = getSolrClient().query(getCollectionName(), query);
    SolrDocumentList results = response.getResults();

    assertEquals(0, results.getNumFound());
  }

  @Test
  void queryWithFilters() throws Exception {
    List<SimpleProduct> products =
        List.of(
            new SimpleProduct("filter-1", "Alpha Product", 10.0),
            new SimpleProduct("filter-2", "Beta Product", 20.0),
            new SimpleProduct("filter-3", "Alpha Premium", 30.0));

    List<SolrInputDocument> docs = getDobby().toDocs(products);
    getSolrClient().add(getCollectionName(), docs);
    getSolrClient().commit(getCollectionName());

    SolrQuery query = new SolrQuery("name:Alpha*");
    QueryResponse response = getSolrClient().query(getCollectionName(), query);
    SolrDocumentList results = response.getResults();

    assertEquals(2, results.getNumFound());

    List<SimpleProduct> retrieved = getDobby().fromDocs(results, SimpleProduct.class);
    assertTrue(retrieved.stream().allMatch(p -> p.name().startsWith("Alpha")));
  }

  @Test
  void partialUpdateWithOptionalFields() throws Exception {
    addSchemaField("description", "string", false);

    ProductWithOptional original =
        new ProductWithOptional("opt-1", "Original", Optional.of("Original description"));

    SolrInputDocument doc = getDobby().toDoc(original);
    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    SolrQuery query = new SolrQuery("id:opt-1");
    QueryResponse response = getSolrClient().query(getCollectionName(), query);
    SolrDocumentList results = response.getResults();

    ProductWithOptional retrieved = getDobby().fromDoc(results.get(0), ProductWithOptional.class);
    assertEquals("Original description", retrieved.description().orElse(null));
  }

  @Test
  void emptyResultReturnsEmptyList() throws Exception {
    SolrQuery query = new SolrQuery("id:nonexistent");
    QueryResponse response = getSolrClient().query(getCollectionName(), query);
    SolrDocumentList results = response.getResults();

    assertEquals(0, results.getNumFound());

    List<SimpleProduct> retrieved = getDobby().fromDocs(results, SimpleProduct.class);
    assertTrue(retrieved.isEmpty());
  }

  @Test
  void concurrentDocumentOperations() throws Exception {
    int count = 100;

    for (int i = 0; i < count; i++) {
      SimpleProduct product = new SimpleProduct("concurrent-" + i, "Product " + i, i * 1.0);
      SolrInputDocument doc = getDobby().toDoc(product);
      getSolrClient().add(getCollectionName(), doc);
    }
    getSolrClient().commit(getCollectionName());

    SolrQuery query = new SolrQuery("id:concurrent-*");
    query.setRows(count);
    QueryResponse response = getSolrClient().query(getCollectionName(), query);
    SolrDocumentList results = response.getResults();

    assertEquals(count, results.getNumFound());

    List<SimpleProduct> retrieved = getDobby().fromDocs(results, SimpleProduct.class);
    assertEquals(count, retrieved.size());

    for (int i = 0; i < count; i++) {
      final int index = i;
      assertTrue(
          retrieved.stream().anyMatch(p -> p.id().equals("concurrent-" + index)),
          "Should find product with id concurrent-" + index);
    }
  }

  // Test record classes

  public record SimpleProduct(
      @SolrField("id") String id,
      @SolrField("name") String name,
      @SolrField("price") double price) {}

  public record ComplexProduct(
      @SolrField("id") String id,
      @SolrField("name") String name,
      @SolrField("price") double price,
      @SolrField("created_at") Instant createdAt,
      @SolrField("tags") List<String> tags,
      @SolrField("rating") int rating) {}

  public record ProductWithOptional(
      @SolrField("id") String id,
      @SolrField("name") String name,
      @SolrField("description") Optional<String> description) {}
}
