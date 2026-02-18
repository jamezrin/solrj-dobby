package com.jamezrin.solrj.dobby.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import org.junit.jupiter.api.Test;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.TypeAdapter;
import com.jamezrin.solrj.dobby.annotation.SolrField;

/** Integration tests for Dobby type adapters covering all supported types. */
class DobbyTypeAdaptersIntegrationTest extends AbstractSolrIntegrationTest {

  @Test
  void recordsWithAllPrimitiveTypes() throws Exception {
    addSchemaField("count", "pint", false);
    addSchemaField("active", "boolean", false);
    addSchemaField("score", "pfloat", false);
    addSchemaField("big_number", "plong", false);

    PrimitiveRecord record = new PrimitiveRecord("prim-1", 42, true, 3.14f, 9999999999L);

    SolrInputDocument doc = getDobby().toDoc(record);
    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    QueryResponse response = getSolrClient().query(getCollectionName(), new SolrQuery("id:prim-1"));
    PrimitiveRecord retrieved =
        getDobby().fromDoc(response.getResults().get(0), PrimitiveRecord.class);

    assertEquals(record.id(), retrieved.id());
    assertEquals(record.count(), retrieved.count());
    assertEquals(record.active(), retrieved.active());
    assertEquals(record.score(), retrieved.score(), 0.001f);
    assertEquals(record.bigNumber(), retrieved.bigNumber());
  }

  @Test
  void javaTimeTypesRoundTrip() throws Exception {
    addSchemaField("instant_field", "pdate", false);
    addSchemaField("local_date", "pdate", false);
    addSchemaField("local_date_time", "pdate", false);
    addSchemaField("zoned_date_time", "pdate", false);

    Instant now = Instant.now();
    LocalDate today = LocalDate.now();
    LocalDateTime dateTime = LocalDateTime.now();
    ZonedDateTime zoned = ZonedDateTime.now();

    JavaTimeRecord record = new JavaTimeRecord("time-1", now, today, dateTime, zoned);

    SolrInputDocument doc = getDobby().toDoc(record);
    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    QueryResponse response = getSolrClient().query(getCollectionName(), new SolrQuery("id:time-1"));
    JavaTimeRecord retrieved =
        getDobby().fromDoc(response.getResults().get(0), JavaTimeRecord.class);

    assertEquals(record.id(), retrieved.id());
    // Instant should be preserved
    assertNotNull(retrieved.instantField());
  }

  @Test
  void enumHandling() throws Exception {
    addSchemaField("status", "string", false);
    addSchemaField("priority", "string", false);

    EnumRecord record = new EnumRecord("enum-1", Status.ACTIVE, Priority.HIGH);

    SolrInputDocument doc = getDobby().toDoc(record);
    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    QueryResponse response = getSolrClient().query(getCollectionName(), new SolrQuery("id:enum-1"));
    EnumRecord retrieved = getDobby().fromDoc(response.getResults().get(0), EnumRecord.class);

    assertEquals(record.status(), retrieved.status());
    assertEquals(record.priority(), retrieved.priority());
  }

  @Test
  void collectionsAndArrays() throws Exception {
    addSchemaField("string_list", "string", true);
    addSchemaField("int_set", "pint", true);
    addSchemaField("string_array", "string", true);

    CollectionRecord record =
        new CollectionRecord(
            "coll-1", List.of("a", "b", "c"), Set.of(1, 2, 3), new String[] {"x", "y", "z"});

    SolrInputDocument doc = getDobby().toDoc(record);
    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    QueryResponse response = getSolrClient().query(getCollectionName(), new SolrQuery("id:coll-1"));
    CollectionRecord retrieved =
        getDobby().fromDoc(response.getResults().get(0), CollectionRecord.class);

    assertEquals(record.stringList().size(), retrieved.stringList().size());
    assertTrue(retrieved.stringList().containsAll(record.stringList()));
    assertEquals(record.intSet().size(), retrieved.intSet().size());
    assertEquals(record.stringArray().length, retrieved.stringArray().length);
  }

  @Test
  void optionalFields() throws Exception {
    addSchemaField("present_field", "string", false);
    addSchemaField("empty_field", "string", false);

    OptionalRecord withValue =
        new OptionalRecord("opt-1", Optional.of("I exist"), Optional.empty());
    OptionalRecord empty = new OptionalRecord("opt-2", Optional.empty(), Optional.of("Other"));

    getSolrClient().add(getCollectionName(), getDobby().toDoc(withValue));
    getSolrClient().add(getCollectionName(), getDobby().toDoc(empty));
    getSolrClient().commit(getCollectionName());

    QueryResponse response = getSolrClient().query(getCollectionName(), new SolrQuery("id:opt-1"));
    OptionalRecord retrieved =
        getDobby().fromDoc(response.getResults().get(0), OptionalRecord.class);

    assertTrue(retrieved.presentField().isPresent());
    assertEquals("I exist", retrieved.presentField().get());
    assertTrue(retrieved.emptyField().isEmpty());
  }

  @Test
  void customTypeAdapter() throws Exception {
    addSchemaField("money", "string", false);

    // Register custom adapter for Money type
    TypeAdapter<Money> moneyAdapter =
        new TypeAdapter<>() {
          @Override
          public Money read(Object value) {
            if (value == null) return null;
            String[] parts = value.toString().split(" ");
            return new Money(Double.parseDouble(parts[0]), parts[1]);
          }

          @Override
          public Object write(Money value) {
            return value.amount() + " " + value.currency();
          }
        };

    Dobby customDobby = Dobby.builder().registerAdapter(Money.class, moneyAdapter).build();

    MoneyRecord record = new MoneyRecord("money-1", new Money(99.99, "USD"));

    SolrInputDocument doc = customDobby.toDoc(record);
    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    QueryResponse response =
        getSolrClient().query(getCollectionName(), new SolrQuery("id:money-1"));
    MoneyRecord retrieved = customDobby.fromDoc(response.getResults().get(0), MoneyRecord.class);

    assertEquals(record.money().amount(), retrieved.money().amount(), 0.001);
    assertEquals(record.money().currency(), retrieved.money().currency());
  }

  @Test
  void toDocsAndFromDocsBulkOperations() throws Exception {
    List<SimpleProduct> products =
        List.of(
            new SimpleProduct("bulk-1", "Product 1", 10.0),
            new SimpleProduct("bulk-2", "Product 2", 20.0),
            new SimpleProduct("bulk-3", "Product 3", 30.0));

    // Bulk write
    List<SolrInputDocument> docs = getDobby().toDocs(products);
    getSolrClient().add(getCollectionName(), docs);
    getSolrClient().commit(getCollectionName());

    // Bulk read
    QueryResponse response = getSolrClient().query(getCollectionName(), new SolrQuery("id:bulk-*"));
    SolrDocumentList results = response.getResults();

    assertEquals(3, results.getNumFound());

    List<SimpleProduct> retrieved = getDobby().fromDocs(results, SimpleProduct.class);
    assertEquals(3, retrieved.size());
  }

  @Test
  void solrJFieldAnnotationSupport() throws Exception {
    addSchemaField("field_name", "string", false);

    SolrJFieldRecord record = new SolrJFieldRecord("solrj-1", "Field Value");

    SolrInputDocument doc = getDobby().toDoc(record);
    getSolrClient().add(getCollectionName(), doc);
    getSolrClient().commit(getCollectionName());

    QueryResponse response =
        getSolrClient().query(getCollectionName(), new SolrQuery("id:solrj-1"));
    SolrDocument solrDoc = response.getResults().get(0);

    assertEquals("Field Value", solrDoc.getFieldValue("field_name"));

    SolrJFieldRecord retrieved = getDobby().fromDoc(solrDoc, SolrJFieldRecord.class);
    assertEquals(record.fieldValue(), retrieved.fieldValue());
  }

  // Test record classes

  public record SimpleProduct(
      @SolrField("id") String id,
      @SolrField("name") String name,
      @SolrField("price") double price) {}

  public record PrimitiveRecord(
      @SolrField("id") String id,
      @SolrField("count") int count,
      @SolrField("active") boolean active,
      @SolrField("score") float score,
      @SolrField("big_number") long bigNumber) {}

  public record JavaTimeRecord(
      @SolrField("id") String id,
      @SolrField("instant_field") Instant instantField,
      @SolrField("local_date") LocalDate localDate,
      @SolrField("local_date_time") LocalDateTime localDateTime,
      @SolrField("zoned_date_time") ZonedDateTime zonedDateTime) {}

  public enum Status {
    ACTIVE,
    INACTIVE,
    PENDING
  }

  public enum Priority {
    LOW,
    MEDIUM,
    HIGH
  }

  public record EnumRecord(
      @SolrField("id") String id,
      @SolrField("status") Status status,
      @SolrField("priority") Priority priority) {}

  public record CollectionRecord(
      @SolrField("id") String id,
      @SolrField("string_list") List<String> stringList,
      @SolrField("int_set") Set<Integer> intSet,
      @SolrField("string_array") String[] stringArray) {}

  public record OptionalRecord(
      @SolrField("id") String id,
      @SolrField("present_field") Optional<String> presentField,
      @SolrField("empty_field") Optional<String> emptyField) {}

  public record MapRecord(
      @SolrField("id") String id, @SolrField("attributes") Map<String, String> attributes) {}

  public record Money(double amount, String currency) {}

  public record MoneyRecord(@SolrField("id") String id, @SolrField("money") Money money) {}

  // Record using SolrJ's @Field annotation
  public record SolrJFieldRecord(
      @org.apache.solr.client.solrj.beans.Field("id") String id,
      @org.apache.solr.client.solrj.beans.Field("field_name") String fieldValue) {}
}
