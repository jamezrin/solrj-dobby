package com.jamezrin.solrj.dobby.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import org.junit.jupiter.api.Test;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.annotation.SolrField;
import com.jamezrin.solrj.dobby.compat.DobbyDocumentObjectBinder;

/**
 * Integration tests for SolrJ compatibility features, comparing Dobby with the standard
 * DocumentObjectBinder.
 */
class SolrJCompatibilityIntegrationTest extends AbstractSolrIntegrationTest {

  @Test
  void dobbyProducesCompatibleOutputWithDocumentObjectBinder() throws Exception {
    ProductBean product = new ProductBean("compat-1", "Compatible Product", 123.45);

    Dobby dobby = Dobby.builder().build();
    SolrInputDocument dobbyDoc = dobby.toDoc(product);

    DocumentObjectBinder standardBinder = new DocumentObjectBinder();
    SolrInputDocument standardDoc = standardBinder.toSolrInputDocument(product);

    assertEquals(standardDoc.getFieldValue("id"), dobbyDoc.getFieldValue("id"));
    assertEquals(standardDoc.getFieldValue("name"), dobbyDoc.getFieldValue("name"));
    assertEquals(standardDoc.getFieldValue("price"), dobbyDoc.getFieldValue("price"));
  }

  @Test
  void dobbyDocumentObjectBinderIntegration() throws Exception {
    Dobby dobby = Dobby.builder().build();
    DobbyDocumentObjectBinder dobbyBinder = new DobbyDocumentObjectBinder(dobby);

    ProductBean product = new ProductBean("dobby-binder-1", "Dobby Binder Test", 99.99);

    SolrInputDocument doc = dobbyBinder.toSolrInputDocument(product);
    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    SolrQuery query = new SolrQuery("id:dobby-binder-1");
    QueryResponse response = getSolrClient().query(getCollectionName(), query);
    SolrDocumentList results = response.getResults();

    assertEquals(1, results.getNumFound());

    List<ProductBean> beans = dobbyBinder.getBeans(ProductBean.class, results);

    assertEquals(1, beans.size());
    ProductBean retrieved = beans.get(0);
    assertEquals(product.getId(), retrieved.getId());
    assertEquals(product.getName(), retrieved.getName());
    assertEquals(product.getPrice(), retrieved.getPrice(), 0.001);
  }

  @Test
  void fieldNamingStrategyAppliedInE2E() throws Exception {
    Dobby dobby = Dobby.builder().fieldNamingStrategy(name -> name).build();

    ProductBeanWithExplicitFields product =
        new ProductBeanWithExplicitFields("naming-1", "Naming Test", 77.77);

    SolrInputDocument doc = dobby.toDoc(product);

    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    SolrQuery query = new SolrQuery("product_id:naming-1");
    QueryResponse response = getSolrClient().query(getCollectionName(), query);
    SolrDocumentList results = response.getResults();

    assertEquals(1, results.getNumFound());
    assertEquals("Naming Test", results.get(0).getFieldValue("product_name"));
  }

  @Test
  void beanWithDifferentFieldTypes() throws Exception {
    addSchemaField("active", "boolean", false);
    addSchemaField("quantity", "int", false);
    addSchemaField("weight", "float", false);

    ProductWithVariousTypes product =
        new ProductWithVariousTypes("types-1", "Type Test", true, 100, 5.5f);

    SolrInputDocument doc = getDobby().toDoc(product);
    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    SolrQuery query = new SolrQuery("id:types-1");
    QueryResponse response = getSolrClient().query(getCollectionName(), query);
    SolrDocumentList results = response.getResults();

    assertEquals(1, results.getNumFound());

    ProductWithVariousTypes retrieved =
        getDobby().fromDoc(results.get(0), ProductWithVariousTypes.class);

    assertEquals(product.id(), retrieved.id());
    assertEquals(product.name(), retrieved.name());
    assertEquals(product.active(), retrieved.active());
    assertEquals(product.quantity(), retrieved.quantity());
    assertEquals(product.weight(), retrieved.weight(), 0.001);
  }

  @Test
  void multipleBeansWithDobbyBinder() throws Exception {
    Dobby dobby = Dobby.builder().build();
    DobbyDocumentObjectBinder binder = new DobbyDocumentObjectBinder(dobby);

    List<ProductBean> products =
        List.of(
            new ProductBean("multi-1", "Product One", 10.0),
            new ProductBean("multi-2", "Product Two", 20.0),
            new ProductBean("multi-3", "Product Three", 30.0));

    List<SolrInputDocument> docs = products.stream().map(binder::toSolrInputDocument).toList();
    getSolrClient().add(getCollectionName(), docs);
    getSolrClient().commit(getCollectionName());

    SolrQuery query = new SolrQuery("id:multi-*");
    query.setRows(10);
    QueryResponse response = getSolrClient().query(getCollectionName(), query);
    SolrDocumentList results = response.getResults();

    assertEquals(3, results.getNumFound());

    List<ProductBean> retrieved = binder.getBeans(ProductBean.class, results);
    assertEquals(3, retrieved.size());
    assertTrue(retrieved.stream().anyMatch(p -> "multi-1".equals(p.getId())));
    assertTrue(retrieved.stream().anyMatch(p -> "multi-2".equals(p.getId())));
    assertTrue(retrieved.stream().anyMatch(p -> "multi-3".equals(p.getId())));
  }

  // Test bean classes (using traditional beans for SolrJ compatibility)

  public static class ProductBean {
    @Field("id")
    private String id;

    @Field("name")
    private String name;

    @Field("price")
    private double price;

    public ProductBean() {}

    public ProductBean(String id, String name, double price) {
      this.id = id;
      this.name = name;
      this.price = price;
    }

    // JavaBean-style getters for SolrJ DocumentObjectBinder compatibility
    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public double getPrice() {
      return price;
    }

    public void setPrice(double price) {
      this.price = price;
    }
  }

  public static class ProductBeanWithExplicitFields {
    @SolrField("product_id")
    private String id;

    @SolrField("product_name")
    private String name;

    @SolrField("product_price")
    private double price;

    public ProductBeanWithExplicitFields() {}

    public ProductBeanWithExplicitFields(String id, String name, double price) {
      this.id = id;
      this.name = name;
      this.price = price;
    }

    public String id() {
      return id;
    }

    public String name() {
      return name;
    }

    public double price() {
      return price;
    }
  }

  public record ProductWithVariousTypes(
      @SolrField("id") String id,
      @SolrField("name") String name,
      @SolrField("active") boolean active,
      @SolrField("quantity") int quantity,
      @SolrField("weight") float weight) {}
}
