package com.jamezrin.solrj.dobby.integration;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.params.SolrParams;

/** SolrJ 9 types that moved or were replaced in SolrJ 10. */
final class SolrTestSupport {

  private SolrTestSupport() {}

  static SolrClient newClient(String solrUrl) {
    return new Http2SolrClient.Builder(solrUrl).build();
  }

  static SolrParams query(String q) {
    return new SolrQuery(q);
  }

  static SolrParams query(String q, int rows) {
    return new SolrQuery(q).setRows(rows);
  }
}
