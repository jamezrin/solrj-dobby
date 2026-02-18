package com.jamezrin.solrj.dobby.integration;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.solr.SolrContainer;
import org.testcontainers.utility.DockerImageName;

import com.jamezrin.solrj.dobby.Dobby;

/**
 * Base class for integration tests that require a running Solr instance.
 *
 * <p>Uses Testcontainers to spin up a Solr Docker container. Tests can extend this class to get
 * access to:
 *
 * <ul>
 *   <li>A pre-configured {@link SolrClient}
 *   <li>A pre-configured {@link Dobby} instance
 *   <li>Utility methods for collection management
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * class MyIntegrationTest extends AbstractSolrIntegrationTest {
 *
 *   @Test
 *   void testSomething() throws Exception {
 *     // Collection is already created and configured
 *     solrClient.addBean(collectionName, myBean);
 *     solrClient.commit(collectionName);
 *
 *     // Use Dobby to convert results
 *     QueryResponse response = solrClient.query(collectionName, new SolrQuery("*:*"));
 *     List<MyBean> beans = dobby.fromDocs(response.getResults(), MyBean.class);
 *   }
 * }
 * }</pre>
 */
@Tag("integration")
@Testcontainers
public abstract class AbstractSolrIntegrationTest {

  private static final String SOLR_IMAGE = "solr:9";
  private static final String COLLECTION_NAME = "test_collection";

  @Container
  protected static final SolrContainer solrContainer =
      new SolrContainer(DockerImageName.parse(SOLR_IMAGE))
          .withCollection(COLLECTION_NAME)
          .withStartupTimeout(Duration.ofMinutes(2));

  protected static SolrClient solrClient;
  protected static Dobby dobby;

  @BeforeAll
  static void setUpClass() throws Exception {
    String solrUrl =
        "http://" + solrContainer.getHost() + ":" + solrContainer.getMappedPort(8983) + "/solr";
    solrClient = new Http2SolrClient.Builder(solrUrl).build();
    dobby = Dobby.builder().build();

    // Configure schema with single-valued fields for basic types
    configureBaseSchema();
  }

  private static void configureBaseSchema() throws Exception {
    // Add single-valued fields that tests expect
    // Use Solr's precision types: pdouble, pint, pfloat, pdate
    addSchemaFieldIfNotExists("name", "string", false);
    addSchemaFieldIfNotExists("price", "pdouble", false);
    addSchemaFieldIfNotExists("active", "boolean", false);
    addSchemaFieldIfNotExists("quantity", "pint", false);
    addSchemaFieldIfNotExists("weight", "pfloat", false);
    addSchemaFieldIfNotExists("rating", "pint", false);
    addSchemaFieldIfNotExists("description", "string", false);
    addSchemaFieldIfNotExists("created_at", "pdate", false);
    addSchemaFieldIfNotExists("tags", "string", true);
    // Fields for fieldNamingStrategy test
    addSchemaFieldIfNotExists("product_id", "string", false);
    addSchemaFieldIfNotExists("product_name", "string", false);
    addSchemaFieldIfNotExists("product_price", "pdouble", false);
    // Additional fields for comprehensive tests
    addSchemaFieldIfNotExists("count", "pint", false);
    addSchemaFieldIfNotExists("score", "pfloat", false);
    addSchemaFieldIfNotExists("big_number", "plong", false);
    addSchemaFieldIfNotExists("instant_field", "pdate", false);
    addSchemaFieldIfNotExists("local_date", "pdate", false);
    addSchemaFieldIfNotExists("local_date_time", "pdate", false);
    addSchemaFieldIfNotExists("zoned_date_time", "pdate", false);
    addSchemaFieldIfNotExists("status", "string", false);
    addSchemaFieldIfNotExists("priority", "string", false);
    addSchemaFieldIfNotExists("string_list", "string", true);
    addSchemaFieldIfNotExists("int_set", "pint", true);
    addSchemaFieldIfNotExists("string_array", "string", true);
    addSchemaFieldIfNotExists("present_field", "string", false);
    addSchemaFieldIfNotExists("empty_field", "string", false);
    addSchemaFieldIfNotExists("attributes", "string", true);
    addSchemaFieldIfNotExists("child_docs", "string", true);
    addSchemaFieldIfNotExists("money", "string", false);
    addSchemaFieldIfNotExists("user_name", "string", false);
    addSchemaFieldIfNotExists("email_address", "string", false);
    addSchemaFieldIfNotExists("solr_field", "string", false);
    addSchemaFieldIfNotExists("field_annotation", "string", false);
    addSchemaFieldIfNotExists("base_field", "string", false);
    addSchemaFieldIfNotExists("derived_field", "string", false);
    // Child document fields
    addSchemaFieldIfNotExists("child_id", "string", false);
    addSchemaFieldIfNotExists("child_name", "string", false);
    addSchemaFieldIfNotExists("child_value", "pint", false);
    // For SolrJ @Field annotation test
    addSchemaFieldIfNotExists("field_name", "string", false);
  }

  private static void addSchemaFieldIfNotExists(
      String fieldName, String fieldType, boolean multiValued) throws Exception {
    SchemaRequest.Fields fieldsRequest = new SchemaRequest.Fields();
    SchemaResponse.FieldsResponse response = fieldsRequest.process(solrClient, COLLECTION_NAME);

    boolean fieldExists =
        response.getFields().stream().anyMatch(field -> fieldName.equals(field.get("name")));

    if (!fieldExists) {
      Map<String, Object> fieldAttributes = new HashMap<>();
      fieldAttributes.put("name", fieldName);
      fieldAttributes.put("type", fieldType);
      fieldAttributes.put("stored", true);
      fieldAttributes.put("indexed", true);
      fieldAttributes.put("multiValued", multiValued);

      SchemaRequest.AddField addFieldRequest = new SchemaRequest.AddField(fieldAttributes);
      addFieldRequest.process(solrClient, COLLECTION_NAME);
    }
  }

  @AfterAll
  static void tearDownClass() throws Exception {
    if (solrClient != null) {
      solrClient.close();
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    // Clear all documents before each test
    solrClient.deleteByQuery(COLLECTION_NAME, "*:*");
    solrClient.commit(COLLECTION_NAME);

    // Re-configure base schema after clearing (schema persists, but let's ensure it)
    configureBaseSchema();
  }

  /**
   * Returns the name of the test collection.
   *
   * @return the collection name
   */
  protected String getCollectionName() {
    return COLLECTION_NAME;
  }

  /**
   * Returns the SolrClient for direct Solr operations.
   *
   * @return the Solr client
   */
  protected SolrClient getSolrClient() {
    return solrClient;
  }

  /**
   * Returns the Dobby instance for document conversion.
   *
   * @return the Dobby instance
   */
  protected Dobby getDobby() {
    return dobby;
  }

  /**
   * Adds a field to the collection schema if it doesn't exist.
   *
   * @param fieldName the name of the field
   * @param fieldType the Solr field type (e.g., "string", "int", "date")
   * @param multiValued whether the field can have multiple values
   * @throws Exception if an error occurs
   */
  protected void addSchemaField(String fieldName, String fieldType, boolean multiValued)
      throws Exception {
    addSchemaFieldIfNotExists(fieldName, fieldType, multiValued);
  }

  /**
   * Deletes the test collection and recreates it. Useful for tests that need a clean schema.
   *
   * @throws SolrServerException if Solr returns an error
   * @throws IOException if a communication error occurs
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  protected void recreateCollection()
      throws SolrServerException, IOException, InterruptedException {
    try {
      CollectionAdminRequest.Delete deleteRequest =
          CollectionAdminRequest.deleteCollection(COLLECTION_NAME);
      deleteRequest.process(solrClient);
    } catch (SolrServerException | IOException e) {
      // Collection might not exist, that's ok
    }

    // Wait a moment for the deletion to complete
    Thread.sleep(1000);

    CollectionAdminRequest.Create createRequest =
        CollectionAdminRequest.createCollection(COLLECTION_NAME, 1, 1);
    createRequest.process(solrClient);

    // Wait for the collection to be ready
    Thread.sleep(1000);
  }
}
