package com.jamezrin.solrj.dobby.compat;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.annotation.SolrField;

import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DobbyDocumentObjectBinderTest {

    @Test
    void defaultConstructorCreatesWorkingBinder() {
        DobbyDocumentObjectBinder binder = new DobbyDocumentObjectBinder();
        assertNotNull(binder);
    }

    @Test
    void getBeanConvertsSolrDocumentToPojo() {
        DobbyDocumentObjectBinder binder = new DobbyDocumentObjectBinder();

        SolrDocument doc = new SolrDocument();
        doc.setField("id", "123");
        doc.setField("name", "Test Product");

        Product product = binder.getBean(Product.class, doc);
        assertEquals("123", product.id);
        assertEquals("Test Product", product.name);
    }

    @Test
    void getBeansConvertsSolrDocumentListToListOfPojos() {
        DobbyDocumentObjectBinder binder = new DobbyDocumentObjectBinder();

        SolrDocumentList docs = new SolrDocumentList();
        SolrDocument doc1 = new SolrDocument();
        doc1.setField("id", "1");
        doc1.setField("name", "Product One");
        SolrDocument doc2 = new SolrDocument();
        doc2.setField("id", "2");
        doc2.setField("name", "Product Two");
        docs.add(doc1);
        docs.add(doc2);

        List<Product> products = binder.getBeans(Product.class, docs);
        assertEquals(2, products.size());
        assertEquals("1", products.get(0).id);
        assertEquals("Product One", products.get(0).name);
        assertEquals("2", products.get(1).id);
        assertEquals("Product Two", products.get(1).name);
    }

    @Test
    void toSolrInputDocumentConvertsPojoToDocument() {
        DobbyDocumentObjectBinder binder = new DobbyDocumentObjectBinder();

        Product product = new Product();
        product.id = "456";
        product.name = "Widget";

        SolrInputDocument doc = binder.toSolrInputDocument(product);
        assertEquals("456", doc.getFieldValue("id"));
        assertEquals("Widget", doc.getFieldValue("name"));
    }

    @Test
    void supportsRecords() {
        DobbyDocumentObjectBinder binder = new DobbyDocumentObjectBinder();

        SolrDocument doc = new SolrDocument();
        doc.setField("id", "789");
        // Solr stores dates as java.util.Date
        doc.setField("createdAt", Date.from(Instant.parse("2024-01-15T10:30:00Z")));

        Order order = binder.getBean(Order.class, doc);
        assertEquals("789", order.id());
        assertEquals(Instant.parse("2024-01-15T10:30:00Z"), order.createdAt());
    }

    @Test
    void supportsSolrJFieldAnnotation() {
        DobbyDocumentObjectBinder binder = new DobbyDocumentObjectBinder();

        SolrDocument doc = new SolrDocument();
        doc.setField("legacy_id", "legacy123");
        doc.setField("product_name_s", "Legacy Product");

        LegacyBean bean = binder.getBean(LegacyBean.class, doc);
        assertEquals("legacy123", bean.id);
        assertEquals("Legacy Product", bean.name);
    }

    @Test
    void customDobbyInstanceConstructor() {
        Dobby customDobby = Dobby.builder()
                .enableSolrJCompat(false)
                .build();
        DobbyDocumentObjectBinder binder = new DobbyDocumentObjectBinder(customDobby);

        SolrDocument doc = new SolrDocument();
        doc.setField("id", "999");
        doc.setField("name", "Test");

        Product product = binder.getBean(Product.class, doc);
        assertEquals("999", product.id);
        assertEquals("Test", product.name);
    }

    public static class Product {
        @SolrField("id")
        public String id;

        @SolrField("name")
        public String name;
    }

    public record Order(
            @SolrField("id") String id,
            @SolrField("createdAt") Instant createdAt
    ) {}

    public static class LegacyBean {
        @Field("legacy_id")
        public String id;

        @Field("product_name_s")
        public String name;
    }
}
