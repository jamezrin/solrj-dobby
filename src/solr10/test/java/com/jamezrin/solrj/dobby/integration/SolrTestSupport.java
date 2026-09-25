package com.jamezrin.solrj.dobby.integration;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.common.params.SolrParams;

/** SolrJ 10 replacements for types that differ from SolrJ 9. */
final class SolrTestSupport {

  private SolrTestSupport() {}

  static SolrClient newClient(String solrUrl) {
    return new HttpJdkSolrClient.Builder(solrUrl).build();
  }

  static SolrParams query(String q) {
    return new SolrQuery(q);
  }

  static SolrParams query(String q, int rows) {
    return new SolrQuery(q).setRows(rows);
  }
}
