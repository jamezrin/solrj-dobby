package com.jamezrin.solrj.dobby.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.DobbyException;
import com.jamezrin.solrj.dobby.FieldNamingStrategy;
import com.jamezrin.solrj.dobby.annotation.SolrField;

class ReflectiveAdapterTest {

  private Dobby dobby;

  @BeforeEach
  void setUp() {
    dobby = Dobby.builder().build();
  }

  @Nested
  class PojoTests {
    @Test
    void readsSimplePojo() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "p1");
      doc.setField("name", "Widget");
      doc.setField("price", 19.99);

      Product p = dobby.fromDoc(doc, Product.class);
      assertEquals("p1", p.id);
      assertEquals("Widget", p.name);
      assertEquals(19.99, p.price);
    }

    @Test
    void writesSimplePojo() {
      Product p = new Product();
      p.id = "p2";
      p.name = "Gadget";
      p.price = 29.99;

      SolrInputDocument doc = dobby.toDoc(p);
      assertEquals("p2", doc.getFieldValue("id"));
      assertEquals("Gadget", doc.getFieldValue("name"));
      assertEquals(29.99, doc.getFieldValue("price"));
    }

    @Test
    void handlesNullFields() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "p3");
      // name and price are missing

      Product p = dobby.fromDoc(doc, Product.class);
      assertEquals("p3", p.id);
      assertNull(p.name);
      assertEquals(0.0, p.price); // primitive default
    }

    @Test
    void handlesEnumField() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "1");
      doc.setField("status", "ACTIVE");

      StatusBean bean = dobby.fromDoc(doc, StatusBean.class);
      assertEquals(Status.ACTIVE, bean.status);
    }

    @Test
    void handlesEnumFieldWriteRead() {
      StatusBean bean = new StatusBean();
      bean.id = "1";
      bean.status = Status.INACTIVE;

      SolrInputDocument doc = dobby.toDoc(bean);
      assertEquals("INACTIVE", doc.getFieldValue("status"));
    }

    @Test
    void handlesInstantField() {
      Date now = new Date();
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "1");
      doc.setField("created_at", now);

      TimestampBean bean = dobby.fromDoc(doc, TimestampBean.class);
      assertEquals(now.toInstant(), bean.createdAt);
    }

    @Test
    void handlesListField() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "1");
      doc.setField("tags", List.of("a", "b", "c"));

      TaggedBean bean = dobby.fromDoc(doc, TaggedBean.class);
      assertEquals(List.of("a", "b", "c"), bean.tags);
    }

    @Test
    void handlesOptionalField() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "1");
      doc.setField("description", "hello");

      OptionalBean bean = dobby.fromDoc(doc, OptionalBean.class);
      assertEquals(Optional.of("hello"), bean.description);
    }

    @Test
    void handlesOptionalFieldMissing() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "1");

      OptionalBean bean = dobby.fromDoc(doc, OptionalBean.class);
      assertEquals(Optional.empty(), bean.description);
    }

    @Test
    void handlesSetterBasedBinding() {
      SolrDocument doc = new SolrDocument();
      doc.setField("value", "test");

      SetterBean bean = dobby.fromDoc(doc, SetterBean.class);
      assertEquals("test", bean.getValue());
    }

    @Test
    void writesSetterBasedBinding() {
      SetterBean bean = new SetterBean();
      bean.setValue("out");

      SolrInputDocument doc = dobby.toDoc(bean);
      assertEquals("out", doc.getFieldValue("value"));
    }
  }

  @Nested
  class InheritanceTests {
    @Test
    void readsInheritedFields() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "1");
      doc.setField("name", "Base");
      doc.setField("extra", "Extended");

      ExtendedBean bean = dobby.fromDoc(doc, ExtendedBean.class);
      assertEquals("1", bean.id);
      assertEquals("Base", bean.name);
      assertEquals("Extended", bean.extra);
    }
  }

  @Nested
  class RecordTests {
    @Test
    void readsRecord() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "r1");
      doc.setField("title", "Record Title");
      doc.setField("score", 95.5);

      SimpleRecord rec = dobby.fromDoc(doc, SimpleRecord.class);
      assertEquals("r1", rec.id());
      assertEquals("Record Title", rec.title());
      assertEquals(95.5, rec.score());
    }

    @Test
    void writesRecord() {
      SimpleRecord rec = new SimpleRecord("r2", "Written", 88.0);
      SolrInputDocument doc = dobby.toDoc(rec);
      assertEquals("r2", doc.getFieldValue("id"));
      assertEquals("Written", doc.getFieldValue("title"));
      assertEquals(88.0, doc.getFieldValue("score"));
    }

    @Test
    void recordRoundTrip() {
      SimpleRecord original = new SimpleRecord("r3", "RoundTrip", 77.7);
      SolrInputDocument written = dobby.toDoc(original);

      SolrDocument solrDoc = new SolrDocument();
      solrDoc.setField("id", written.getFieldValue("id"));
      solrDoc.setField("title", written.getFieldValue("title"));
      solrDoc.setField("score", written.getFieldValue("score"));

      SimpleRecord restored = dobby.fromDoc(solrDoc, SimpleRecord.class);
      assertEquals(original, restored);
    }

    @Test
    void recordWithDefaultPrimitives() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "r4");
      // title and score are missing

      SimpleRecord rec = dobby.fromDoc(doc, SimpleRecord.class);
      assertEquals("r4", rec.id());
      assertNull(rec.title());
      assertEquals(0.0, rec.score());
    }
  }

  @Nested
  class NestedDocumentTests {

    @Test
    void readsNestedFromFieldValues() {
      // Simulate nested docs as field values (modern Solr nested doc support)
      SolrDocument childDoc = new SolrDocument();
      childDoc.setField("sku", "V1");
      childDoc.setField("color", "red");

      SolrDocument parentDoc = new SolrDocument();
      parentDoc.setField("id", "p1");
      parentDoc.setField("name", "Shirt");
      parentDoc.setField("variants", List.of(childDoc));

      ProductWithVariants product = dobby.fromDoc(parentDoc, ProductWithVariants.class);
      assertEquals("p1", product.id);
      assertEquals("Shirt", product.name);
      assertNotNull(product.variants);
      assertEquals(1, product.variants.size());
      assertEquals("V1", product.variants.get(0).sku);
      assertEquals("red", product.variants.get(0).color);
    }

    @Test
    void readsNestedFromChildDocuments() {
      SolrDocument childDoc = new SolrDocument();
      childDoc.setField("sku", "V2");
      childDoc.setField("color", "blue");

      SolrDocument parentDoc = new SolrDocument();
      parentDoc.setField("id", "p2");
      parentDoc.setField("name", "Pants");
      parentDoc.addChildDocument(childDoc);

      ProductWithVariants product = dobby.fromDoc(parentDoc, ProductWithVariants.class);
      assertEquals("p2", product.id);
      assertNotNull(product.variants);
      assertEquals(1, product.variants.size());
      assertEquals("V2", product.variants.get(0).sku);
    }

    @Test
    void writesNestedAsChildDocuments() {
      ProductWithVariants product = new ProductWithVariants();
      product.id = "p3";
      product.name = "Jacket";
      Variant v = new Variant();
      v.sku = "V3";
      v.color = "green";
      product.variants = List.of(v);

      SolrInputDocument doc = dobby.toDoc(product);
      assertEquals("p3", doc.getFieldValue("id"));
      assertNotNull(doc.getChildDocuments());
      assertEquals(1, doc.getChildDocuments().size());
      assertEquals("V3", doc.getChildDocuments().get(0).getFieldValue("sku"));
      assertEquals("green", doc.getChildDocuments().get(0).getFieldValue("color"));
    }

    @Test
    void readsSingleNestedObject() {
      SolrDocument childDoc = new SolrDocument();
      childDoc.setField("sku", "V4");
      childDoc.setField("color", "black");

      SolrDocument parentDoc = new SolrDocument();
      parentDoc.setField("id", "p4");
      parentDoc.setField("primary_variant", childDoc);

      ProductWithSingleVariant product = dobby.fromDoc(parentDoc, ProductWithSingleVariant.class);
      assertEquals("p4", product.id);
      assertNotNull(product.primaryVariant);
      assertEquals("V4", product.primaryVariant.sku);
    }

    @Test
    void writesSingleNestedObject() {
      ProductWithSingleVariant product = new ProductWithSingleVariant();
      product.id = "p5";
      product.primaryVariant = new Variant();
      product.primaryVariant.sku = "V5";
      product.primaryVariant.color = "white";

      SolrInputDocument doc = dobby.toDoc(product);
      assertNotNull(doc.getChildDocuments());
      assertEquals(1, doc.getChildDocuments().size());
      assertEquals("V5", doc.getChildDocuments().get(0).getFieldValue("sku"));
    }

    @Test
    void nestedWithNoChildrenReturnsNull() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "p6");
      doc.setField("name", "Solo");

      ProductWithVariants product = dobby.fromDoc(doc, ProductWithVariants.class);
      assertNull(product.variants);
    }
  }

  @Nested
  class ArrayNestedDocumentTests {

    @Test
    void readsNestedArrayFromChildDocuments() {
      SolrDocument child1 = new SolrDocument();
      child1.setField("sku", "A1");
      child1.setField("color", "red");

      SolrDocument child2 = new SolrDocument();
      child2.setField("sku", "A2");
      child2.setField("color", "blue");

      SolrDocument parent = new SolrDocument();
      parent.setField("id", "arr1");
      parent.addChildDocument(child1);
      parent.addChildDocument(child2);

      ProductWithArrayVariants product = dobby.fromDoc(parent, ProductWithArrayVariants.class);
      assertEquals("arr1", product.id);
      assertNotNull(product.variants);
      assertEquals(2, product.variants.length);
      assertEquals("A1", product.variants[0].sku);
      assertEquals("red", product.variants[0].color);
      assertEquals("A2", product.variants[1].sku);
      assertEquals("blue", product.variants[1].color);
    }

    @Test
    void writesNestedArrayAsChildDocuments() {
      ProductWithArrayVariants product = new ProductWithArrayVariants();
      product.id = "arr2";
      Variant v1 = new Variant();
      v1.sku = "A3";
      v1.color = "green";
      Variant v2 = new Variant();
      v2.sku = "A4";
      v2.color = "white";
      product.variants = new Variant[] {v1, v2};

      SolrInputDocument doc = dobby.toDoc(product);
      assertEquals("arr2", doc.getFieldValue("id"));
      assertNotNull(doc.getChildDocuments());
      assertEquals(2, doc.getChildDocuments().size());
      assertEquals("A3", doc.getChildDocuments().get(0).getFieldValue("sku"));
      assertEquals("A4", doc.getChildDocuments().get(1).getFieldValue("sku"));
    }

    @Test
    void nestedArrayRoundTrip() {
      ProductWithArrayVariants in = new ProductWithArrayVariants();
      in.id = "arr3";
      Variant v1 = new Variant();
      v1.sku = "RT1";
      v1.color = "black";
      Variant v2 = new Variant();
      v2.sku = "RT2";
      v2.color = "orange";
      in.variants = new Variant[] {v1, v2};

      SolrInputDocument solrInputDoc = dobby.toDoc(in);
      SolrDocument solrDoc = toSolrDocument(solrInputDoc);

      assertEquals(2, solrInputDoc.getChildDocuments().size());
      assertEquals(2, solrDoc.getChildDocuments().size());

      ProductWithArrayVariants out = dobby.fromDoc(solrDoc, ProductWithArrayVariants.class);
      assertEquals(in.id, out.id);
      assertEquals(in.variants[0].sku, out.variants[0].sku);
      assertEquals(in.variants[0].color, out.variants[0].color);
      assertEquals(in.variants[1].sku, out.variants[1].sku);
      assertEquals(in.variants[1].color, out.variants[1].color);
    }
  }

  @Nested
  class NestedRoundTripTests {

    @Test
    void singleNestedRoundTrip() {
      ProductWithSingleVariant in = new ProductWithSingleVariant();
      in.id = "s1";
      in.primaryVariant = new Variant();
      in.primaryVariant.sku = "SV1";
      in.primaryVariant.color = "navy";

      SolrInputDocument solrInputDoc = dobby.toDoc(in);
      SolrDocument solrDoc = toSolrDocument(solrInputDoc);

      assertEquals(1, solrInputDoc.getChildDocuments().size());
      assertEquals(1, solrDoc.getChildDocuments().size());

      ProductWithSingleVariant out = dobby.fromDoc(solrDoc, ProductWithSingleVariant.class);
      assertEquals(in.id, out.id);
      assertEquals(in.primaryVariant.sku, out.primaryVariant.sku);
      assertEquals(in.primaryVariant.color, out.primaryVariant.color);
    }

    @Test
    void listNestedRoundTrip() {
      ProductWithVariants in = new ProductWithVariants();
      in.id = "l1";
      in.name = "Sneakers";
      Variant v1 = new Variant();
      v1.sku = "LV1";
      v1.color = "red";
      Variant v2 = new Variant();
      v2.sku = "LV2";
      v2.color = "blue";
      in.variants = List.of(v1, v2);

      SolrInputDocument solrInputDoc = dobby.toDoc(in);
      SolrDocument solrDoc = toSolrDocument(solrInputDoc);

      assertEquals(2, solrInputDoc.getChildDocuments().size());
      assertEquals(2, solrDoc.getChildDocuments().size());

      ProductWithVariants out = dobby.fromDoc(solrDoc, ProductWithVariants.class);
      assertEquals(in.id, out.id);
      assertEquals(in.name, out.name);
      assertEquals(2, out.variants.size());
      assertEquals(in.variants.get(0).sku, out.variants.get(0).sku);
      assertEquals(in.variants.get(1).sku, out.variants.get(1).sku);
    }
  }

  @Nested
  class EmptyNestedListTests {

    @Test
    void emptyNestedListFieldReturnsEmptyList() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "e1");
      doc.setField("name", "EmptyParent");
      doc.setField("variants", new ArrayList<>());

      ProductWithVariants product = dobby.fromDoc(doc, ProductWithVariants.class);
      assertEquals("e1", product.id);
      assertNotNull(product.variants);
      assertTrue(product.variants.isEmpty());
    }
  }

  @Nested
  class NestedWithoutFlagTests {

    @Test
    void readsNestedListFromFieldValuesWithoutFlag() {
      // When a field is typed as List<SolrDocument-backed bean> and the Solr field
      // value is List<SolrDocument>, our CollectionAdapter + ReflectiveAdapter
      // should handle it even without nested=true.
      SolrDocument child1 = new SolrDocument();
      child1.setField("sku", "NF1");
      child1.setField("color", "red");

      SolrDocument child2 = new SolrDocument();
      child2.setField("sku", "NF2");
      child2.setField("color", "blue");

      SolrDocument parent = new SolrDocument();
      parent.setField("id", "p-nf");
      parent.setField("name", "NoFlag");
      parent.setField("variants_no_flag", List.of(child1, child2));

      ProductWithVariantsNoFlag product = dobby.fromDoc(parent, ProductWithVariantsNoFlag.class);
      assertEquals("p-nf", product.id);
      assertNotNull(product.variantsNoFlag);
      assertEquals(2, product.variantsNoFlag.size());
      assertEquals("NF1", product.variantsNoFlag.get(0).sku);
      assertEquals("NF2", product.variantsNoFlag.get(1).sku);
    }
  }

  @Nested
  class SetterWithoutGetterTests {

    @Test
    void setterWithoutGetterThrowsOnWrite() {
      SetterOnlyBean bean = new SetterOnlyBean();
      bean.setInStock(true);

      assertThrows(DobbyException.class, () -> dobby.toDoc(bean));
    }

    @Test
    void setterWithoutGetterReadsSuccessfully() {
      SolrDocument doc = new SolrDocument();
      doc.setField("in_stock", true);

      SetterOnlyBean bean = dobby.fromDoc(doc, SetterOnlyBean.class);
      // We can't read inStock back because there is no getter,
      // but the read itself shouldn't fail
      assertNotNull(bean);
    }
  }

  @Nested
  class StringArrayFieldTests {

    @Test
    void writesStringArrayAsMultiValuedField() {
      ItemWithCategories item = new ItemWithCategories();
      item.id = "i1";
      item.categories = new String[] {"electronics", "gadget", "sale"};

      SolrInputDocument doc = dobby.toDoc(item);
      assertEquals("i1", doc.getFieldValue("id"));
      SolrInputField catField = doc.getField("cat");
      assertNotNull(catField);
      assertEquals(3, catField.getValueCount());
      @SuppressWarnings("unchecked")
      List<String> catValues = (List<String>) catField.getValue();
      assertEquals("electronics", catValues.get(0));
      assertEquals("gadget", catValues.get(1));
      assertEquals("sale", catValues.get(2));
    }

    @Test
    void readsSingleValueIntoStringArray() {
      // When Solr returns a single value for a multi-valued field,
      // it comes as a plain String instead of a List
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "i2");
      doc.setField("cat", "single-category");

      ItemWithCategories item = dobby.fromDoc(doc, ItemWithCategories.class);
      assertEquals("i2", item.id);
      assertNotNull(item.categories);
      assertEquals(1, item.categories.length);
      assertEquals("single-category", item.categories[0]);
    }

    @Test
    void readsMultiValuedIntoStringArray() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "i3");
      doc.setField("cat", List.of("a", "b", "c"));

      ItemWithCategories item = dobby.fromDoc(doc, ItemWithCategories.class);
      assertNotNull(item.categories);
      assertEquals(3, item.categories.length);
      assertEquals("a", item.categories[0]);
      assertEquals("b", item.categories[1]);
      assertEquals("c", item.categories[2]);
    }
  }

  @Nested
  class MixedListTypeTests {

    @Test
    void handlesMixedPrimitiveAndNestedLists() {
      SolrDocument childDoc = new SolrDocument();
      childDoc.setField("sku", "M1");
      childDoc.setField("color", "red");

      SolrDocument parent = new SolrDocument();
      parent.setField("id", "mix1");
      parent.setField("tags", List.of("a", "b", "c"));
      parent.setField("nested_items", List.of(childDoc));

      MixedListBean bean = dobby.fromDoc(parent, MixedListBean.class);
      assertEquals("mix1", bean.id);
      assertEquals(List.of("a", "b", "c"), bean.tags);
      assertNotNull(bean.nestedItems);
      assertEquals(1, bean.nestedItems.size());
      assertEquals("M1", bean.nestedItems.get(0).sku);
    }

    @Test
    void writesMixedPrimitiveAndNestedLists() {
      MixedListBean bean = new MixedListBean();
      bean.id = "mix2";
      bean.tags = List.of("x", "y");
      Variant v = new Variant();
      v.sku = "M2";
      v.color = "blue";
      bean.nestedItems = List.of(v);

      SolrInputDocument doc = dobby.toDoc(bean);
      assertEquals("mix2", doc.getFieldValue("id"));
      // getFieldValue returns the first value; use getField().getValue() for the full list
      assertEquals(List.of("x", "y"), doc.getField("tags").getValue());
      assertNotNull(doc.getChildDocuments());
      assertEquals(1, doc.getChildDocuments().size());
      assertEquals("M2", doc.getChildDocuments().get(0).getFieldValue("sku"));
    }
  }

  private static SolrDocument toSolrDocument(SolrInputDocument d) {
    SolrDocument doc = new SolrDocument();
    for (SolrInputField field : d) {
      doc.setField(field.getName(), field.getValue());
    }
    if (d.getChildDocuments() != null) {
      for (SolrInputDocument child : d.getChildDocuments()) {
        doc.addChildDocument(toSolrDocument(child));
      }
    }
    return doc;
  }

  @Nested
  class DynamicMapFieldTests {
    @Test
    void readsDynamicMapField() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "1");
      // The map field value as Solr would return it
      Map<String, Object> attrs = Map.of("size_s", "L", "weight_d", 2.5);
      doc.setField("attributes", attrs);

      DynamicBean bean = dobby.fromDoc(doc, DynamicBean.class);
      assertNotNull(bean.attributes);
      assertEquals("L", bean.attributes.get("size_s"));
    }

    @Test
    void writesDynamicMapAsIndividualFields() {
      DynamicBean bean = new DynamicBean();
      bean.id = "1";
      bean.attributes = Map.of("size_s", "M", "weight_d", 3.0);

      SolrInputDocument doc = dobby.toDoc(bean);
      // Dynamic maps are spread into individual fields
      assertEquals("M", doc.getFieldValue("size_s"));
      assertEquals(3.0, doc.getFieldValue("weight_d"));
    }
  }

  @Nested
  class NamingStrategyTests {
    @Test
    void lowerUnderscoreStrategy() {
      Dobby snakeDobby =
          Dobby.builder().fieldNamingStrategy(FieldNamingStrategy.LOWER_UNDERSCORE).build();

      SolrDocument doc = new SolrDocument();
      doc.setField("first_name", "John");
      doc.setField("last_name", "Doe");

      PersonBean bean = snakeDobby.fromDoc(doc, PersonBean.class);
      assertEquals("John", bean.firstName);
      assertEquals("Doe", bean.lastName);
    }

    @Test
    void explicitNameOverridesStrategy() {
      Dobby snakeDobby =
          Dobby.builder().fieldNamingStrategy(FieldNamingStrategy.LOWER_UNDERSCORE).build();

      SolrDocument doc = new SolrDocument();
      doc.setField("custom_id", "X");

      ExplicitNameBean bean = snakeDobby.fromDoc(doc, ExplicitNameBean.class);
      assertEquals("X", bean.myId);
    }
  }

  public static class Product {
    @SolrField("id")
    public String id;

    @SolrField("name")
    public String name;

    @SolrField("price")
    public double price;
  }

  public enum Status {
    ACTIVE,
    INACTIVE
  }

  public static class StatusBean {
    @SolrField("id")
    public String id;

    @SolrField("status")
    public Status status;
  }

  public static class TimestampBean {
    @SolrField("id")
    public String id;

    @SolrField("created_at")
    public Instant createdAt;
  }

  public static class TaggedBean {
    @SolrField("id")
    public String id;

    @SolrField("tags")
    public List<String> tags;
  }

  public static class OptionalBean {
    @SolrField("id")
    public String id;

    @SolrField("description")
    public Optional<String> description;
  }

  public static class SetterBean {
    private String value;

    public String getValue() {
      return value;
    }

    @SolrField("value")
    public void setValue(String value) {
      this.value = value;
    }
  }

  public static class BaseBean {
    @SolrField("id")
    public String id;

    @SolrField("name")
    public String name;
  }

  public static class ExtendedBean extends BaseBean {
    @SolrField("extra")
    public String extra;
  }

  public record SimpleRecord(
      @SolrField("id") String id,
      @SolrField("title") String title,
      @SolrField("score") double score) {}

  public static class Variant {
    @SolrField("sku")
    public String sku;

    @SolrField("color")
    public String color;
  }

  public static class ProductWithVariants {
    @SolrField("id")
    public String id;

    @SolrField("name")
    public String name;

    @SolrField(value = "variants", nested = true)
    public List<Variant> variants;
  }

  public static class ProductWithSingleVariant {
    @SolrField("id")
    public String id;

    @SolrField(value = "primary_variant", nested = true)
    public Variant primaryVariant;
  }

  public static class DynamicBean {
    @SolrField("id")
    public String id;

    @SolrField("attributes")
    public Map<String, Object> attributes;
  }

  public static class PersonBean {
    @SolrField public String firstName;
    @SolrField public String lastName;
  }

  public static class ExplicitNameBean {
    @SolrField("custom_id")
    public String myId;
  }

  public static class ProductWithArrayVariants {
    @SolrField("id")
    public String id;

    @SolrField(value = "variants", nested = true)
    public Variant[] variants;
  }

  public static class ProductWithVariantsNoFlag {
    @SolrField("id")
    public String id;

    @SolrField("name")
    public String name;

    // No nested=true - but the field value can still contain List<SolrDocument>
    @SolrField("variants_no_flag")
    public List<Variant> variantsNoFlag;
  }

  public static class SetterOnlyBean {
    private boolean inStock;

    @SolrField("in_stock")
    public void setInStock(boolean b) {
      this.inStock = b;
    }
    // Deliberately no getter
  }

  public static class ItemWithCategories {
    @SolrField("id")
    public String id;

    @SolrField("cat")
    public String[] categories;
  }

  public static class MixedListBean {
    @SolrField("id")
    public String id;

    @SolrField("tags")
    public List<String> tags;

    @SolrField(value = "nested_items", nested = true)
    public List<Variant> nestedItems;
  }
}
