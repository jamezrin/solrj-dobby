package com.jamezrin.solrj.dobby;

import org.apache.solr.client.solrj.SolrClient;

/**
 * Entry point for the solrj-dobby library.
 */
public class Dobby {

    private final SolrClient solrClient;

    public Dobby(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public SolrClient getSolrClient() {
        return solrClient;
    }
}
