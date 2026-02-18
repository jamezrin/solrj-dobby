package com.jamezrin.solrj.dobby;

import com.jamezrin.solrj.dobby.annotation.SolrField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DobbyTest {

    @Test
    void builderProducesInstance() {
        Dobby dobby = Dobby.builder().build();
        assertNotNull(dobby);
    }

    @Test
    void builderWithoutSolrJCompat() {
        Dobby dobby = Dobby.builder().enableSolrJCompat(false).build();
        assertNotNull(dobby);
    }

    @Test
    void fromDocReadsSimplePojo() {
        Dobby dobby = Dobby.builder().build();

        SolrDocument doc = new SolrDocument();
        doc.setField("id", "123");
        doc.setField("name", "Test");

        SimpleBean bean = dobby.fromDoc(doc, SimpleBean.class);
        assertEquals("123", bean.id);
        assertEquals("Test", bean.name);
    }

    @Test
    void toDocWritesSimplePojo() {
        Dobby dobby = Dobby.builder().build();

        SimpleBean bean = new SimpleBean();
        bean.id = "456";
        bean.name = "Hello";

        SolrInputDocument doc = dobby.toDoc(bean);
        assertEquals("456", doc.getFieldValue("id"));
        assertEquals("Hello", doc.getFieldValue("name"));
    }

    @Test
    void fromDocsConvertsMultiple() {
        Dobby dobby = Dobby.builder().build();

        SolrDocument doc1 = new SolrDocument();
        doc1.setField("id", "1");
        doc1.setField("name", "A");

        SolrDocument doc2 = new SolrDocument();
        doc2.setField("id", "2");
        doc2.setField("name", "B");

        List<SimpleBean> beans = dobby.fromDocs(List.of(doc1, doc2), SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("1", beans.get(0).id);
        assertEquals("2", beans.get(1).id);
    }

    @Test
    void fromDocsReturnsEmptyListForEmptyInput() {
        Dobby dobby = Dobby.builder().build();
        List<SimpleBean> beans = dobby.fromDocs(List.of(), SimpleBean.class);
        assertNotNull(beans);
        assertTrue(beans.isEmpty());
    }

    @Test
    void roundTripPreservesData() {
        Dobby dobby = Dobby.builder().build();

        SimpleBean original = new SimpleBean();
        original.id = "rt-1";
        original.name = "RoundTrip";

        SolrInputDocument doc = dobby.toDoc(original);

        // Simulate Solr returning the data
        SolrDocument solrDoc = new SolrDocument();
        solrDoc.setField("id", doc.getFieldValue("id"));
        solrDoc.setField("name", doc.getFieldValue("name"));

        SimpleBean restored = dobby.fromDoc(solrDoc, SimpleBean.class);
        assertEquals(original.id, restored.id);
        assertEquals(original.name, restored.name);
    }

    @Test
    void getAdapterThrowsForUnknownType() {
        Dobby dobby = Dobby.builder().build();
        assertThrows(DobbyException.class, () -> dobby.getAdapter(UnannotatedBean.class));
    }

    @Test
    void customAdapterOverridesBuiltIn() {
        TypeAdapter<String> uppercaseAdapter = new TypeAdapter<>() {
            @Override
            public String read(Object solrValue) {
                return solrValue == null ? null : solrValue.toString().toUpperCase();
            }

            @Override
            public Object write(String value) {
                return value == null ? null : value.toLowerCase();
            }
        };

        Dobby dobby = Dobby.builder()
                .registerAdapter(String.class, uppercaseAdapter)
                .build();

        TypeAdapter<String> adapter = dobby.getAdapter(String.class);
        assertEquals("HELLO", adapter.read("hello"));
        assertEquals("hello", adapter.write("HELLO"));
    }

    @Test
    void fieldNamingStrategyApplied() {
        Dobby dobby = Dobby.builder()
                .fieldNamingStrategy(FieldNamingStrategy.LOWER_UNDERSCORE)
                .build();

        SolrDocument doc = new SolrDocument();
        doc.setField("my_field", "works");

        NamingBean bean = dobby.fromDoc(doc, NamingBean.class);
        assertEquals("works", bean.myField);
    }

    @Test
    void nullSafeAdapter() {
        TypeAdapter<String> adapter = new TypeAdapter<>() {
            @Override
            public String read(Object solrValue) {
                return solrValue.toString(); // would NPE on null
            }

            @Override
            public Object write(String value) {
                return value.length(); // would NPE on null
            }
        };

        TypeAdapter<String> safe = adapter.nullSafe();
        assertNull(safe.read(null));
        assertNull(safe.write(null));
        assertEquals("hello", safe.read("hello"));
    }

    @Test
    void toDocsConvertsMultipleObjects() {
        Dobby dobby = Dobby.builder().build();

        SimpleBean bean1 = new SimpleBean();
        bean1.id = "1";
        bean1.name = "A";

        SimpleBean bean2 = new SimpleBean();
        bean2.id = "2";
        bean2.name = "B";

        List<SolrInputDocument> docs = dobby.toDocs(List.of(bean1, bean2));
        assertEquals(2, docs.size());
        assertEquals("1", docs.get(0).getFieldValue("id"));
        assertEquals("A", docs.get(0).getFieldValue("name"));
        assertEquals("2", docs.get(1).getFieldValue("id"));
        assertEquals("B", docs.get(1).getFieldValue("name"));
    }

    @Test
    void toDocsReturnsEmptyListForEmptyInput() {
        Dobby dobby = Dobby.builder().build();
        List<SolrInputDocument> docs = dobby.toDocs(List.of());
        assertNotNull(docs);
        assertTrue(docs.isEmpty());
    }

    public static class SimpleBean {
        @SolrField("id")
        public String id;

        @SolrField("name")
        public String name;
    }

    public static class UnannotatedBean {
        public String id;
    }

    public static class NamingBean {
        @SolrField // no explicit name - naming strategy applies
        public String myField;
    }
}
