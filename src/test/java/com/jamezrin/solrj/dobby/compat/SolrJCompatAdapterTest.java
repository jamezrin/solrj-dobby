package com.jamezrin.solrj.dobby.compat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.annotation.SolrField;

class SolrJCompatAdapterTest {

  private Dobby dobby;

  @BeforeEach
  void setUp() {
    dobby = Dobby.builder().build();
  }

  @Nested
  class BasicCompatTests {

    @Test
    void readsPojoWithSolrJAnnotation() {
      SolrDocument doc = new SolrDocument();
      doc.setField("id", "c1");
      doc.setField("title", "SolrJ Bean");

      SolrJBean bean = dobby.fromDoc(doc, SolrJBean.class);
      assertEquals("c1", bean.id);
      assertEquals("SolrJ Bean", bean.title);
    }

    @Test
    void writesPojoWithSolrJAnnotation() {
      SolrJBean bean = new SolrJBean();
      bean.id = "c2";
      bean.title = "Written";

      SolrInputDocument doc = dobby.toDoc(bean);
      assertEquals("c2", doc.getFieldValue("id"));
      assertEquals("Written", doc.getFieldValue("title"));
    }

    @Test
    void roundTrip() {
      SolrJBean original = new SolrJBean();
      original.id = "c3";
      original.title = "RoundTrip";

      SolrInputDocument written = dobby.toDoc(original);

      SolrDocument solrDoc = new SolrDocument();
      solrDoc.setField("id", written.getFieldValue("id"));
      solrDoc.setField("title", written.getFieldValue("title"));

      SolrJBean restored = dobby.fromDoc(solrDoc, SolrJBean.class);
      assertEquals(original.id, restored.id);
      assertEquals(original.title, restored.title);
    }
  }

  @Nested
  class ExplicitNameTests {
    @Test
    void readsWithExplicitFieldName() {
      SolrDocument doc = new SolrDocument();
      doc.setField("product_name_s", "Named");

      ExplicitNameBean bean = dobby.fromDoc(doc, ExplicitNameBean.class);
      assertEquals("Named", bean.name);
    }
  }

  @Nested
  class ChildDocTests {
    @Test
    void readsChildDocumentsViaSolrJAnnotation() {
      SolrDocument child = new SolrDocument();
      child.setField("id", "child1");
      child.setField("title", "Child");

      SolrDocument parent = new SolrDocument();
      parent.setField("id", "parent1");
      parent.setField("title", "Parent");
      parent.addChildDocument(child);

      SolrJParentBean bean = dobby.fromDoc(parent, SolrJParentBean.class);
      assertEquals("parent1", bean.id);
      assertNotNull(bean.children);
      assertEquals(1, bean.children.size());
      assertEquals("child1", bean.children.get(0).id);
    }

    @Test
    void writesChildDocumentsViaSolrJAnnotation() {
      SolrJBean child = new SolrJBean();
      child.id = "ch1";
      child.title = "Ch";

      SolrJParentBean parent = new SolrJParentBean();
      parent.id = "par1";
      parent.title = "Par";
      parent.children = List.of(child);

      SolrInputDocument doc = dobby.toDoc(parent);
      assertEquals("par1", doc.getFieldValue("id"));
      assertNotNull(doc.getChildDocuments());
      assertEquals(1, doc.getChildDocuments().size());
      assertEquals("ch1", doc.getChildDocuments().get(0).getFieldValue("id"));
    }
  }

  @Nested
  class PriorityTests {
    @Test
    void solrFieldAnnotationTakesPriority() {
      SolrDocument doc = new SolrDocument();
      doc.setField("dobby_id", "d1");

      MixedBean bean = dobby.fromDoc(doc, MixedBean.class);
      assertEquals("d1", bean.id);
    }
  }

  @Nested
  class DisabledCompatTests {
    @Test
    void solrJAnnotationIgnoredWhenDisabled() {
      Dobby noCompat = Dobby.builder().enableSolrJCompat(false).build();
      assertThrows(Exception.class, () -> noCompat.fromDoc(new SolrDocument(), SolrJBean.class));
    }
  }

  @Nested
  class SetterWithoutGetterTests {

    @Test
    void setterWithoutGetterThrowsOnWrite() {
      SolrJSetterOnlyBean bean = new SolrJSetterOnlyBean();
      bean.id = "s1";
      bean.setInStock(false);
      bean.setAaa("test");

      // Writing should fail because setInStock has no corresponding getter
      assertThrows(Exception.class, () -> dobby.toDoc(bean));
    }

    @Test
    void setterWithGetterWorksOnWrite() {
      SolrJSetterOnlyBean bean = new SolrJSetterOnlyBean();
      bean.id = "s2";
      bean.setAaa("hello");

      // This should work partially - aaa has a getter
      // but inStock doesn't, so it should fail
      assertThrows(Exception.class, () -> dobby.toDoc(bean));
    }
  }

  @Nested
  class NestedWithoutChildFlagTests {

    @Test
    void readsNestedDocsFromFieldValuesWithoutChildFlag() {
      // SolrJ's @Field without child=true, but the Solr response contains
      // List<SolrDocument> as a field value (modern Solr nested doc support).
      // Our compat layer should handle this through the CollectionAdapter chain.
      SolrDocument child1 = new SolrDocument();
      child1.setField("id", "c1");
      child1.setField("title", "Child One");

      SolrDocument child2 = new SolrDocument();
      child2.setField("id", "c2");
      child2.setField("title", "Child Two");

      SolrDocument parent = new SolrDocument();
      parent.setField("id", "parent");
      parent.setField("title", "Parent");
      parent.setField("items", List.of(child1, child2));

      SolrJParentNoChildFlag bean = dobby.fromDoc(parent, SolrJParentNoChildFlag.class);
      assertEquals("parent", bean.id);
      assertNotNull(bean.items);
      assertEquals(2, bean.items.size());
      assertEquals("c1", bean.items.get(0).id);
      assertEquals("c2", bean.items.get(1).id);
    }
  }

  public static class SolrJBean {
    @Field public String id;

    @Field public String title;
  }

  public static class ExplicitNameBean {
    @Field("product_name_s")
    public String name;
  }

  public static class SolrJParentBean {
    @Field public String id;

    @Field public String title;

    @Field(child = true)
    public List<SolrJBean> children;
  }

  /**
   * When both @SolrField and @Field are present, @SolrField wins because ReflectiveAdapterFactory
   * runs before SolrJCompatAdapterFactory.
   */
  public static class MixedBean {
    @SolrField("dobby_id")
    @Field("solrj_id")
    public String id;
  }

  /** Mimics SolrJ's NotGettableItem: setter-annotated methods without getters. */
  public static class SolrJSetterOnlyBean {
    @Field public String id;

    private boolean inStock;
    private String aaa;

    @Field
    public void setInStock(Boolean b) {
      inStock = b;
    }

    // Deliberately no getter for inStock

    public String getAaa() {
      return aaa;
    }

    @Field
    public void setAaa(String aaa) {
      this.aaa = aaa;
    }
  }

  /**
   * Parent with @Field-annotated child list but WITHOUT child=true. Modern Solr can return
   * List<SolrDocument> as a field value.
   */
  public static class SolrJParentNoChildFlag {
    @Field public String id;

    @Field public String title;

    @Field("items")
    public List<SolrJBean> items;
  }
}
