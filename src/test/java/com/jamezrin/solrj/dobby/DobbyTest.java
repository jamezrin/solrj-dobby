package com.jamezrin.solrj.dobby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DobbyTest {

    @Test
    void constructorAcceptsNullClient() {
        var dobby = new Dobby(null);
        assertNull(dobby.getSolrClient());
    }
}
