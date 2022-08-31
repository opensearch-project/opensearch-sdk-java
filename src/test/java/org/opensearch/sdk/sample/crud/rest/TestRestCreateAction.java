/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.crud.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.test.OpenSearchTestCase;

import java.util.List;

public class TestRestCreateAction extends OpenSearchTestCase {

    private ExtensionRestHandler restCreateAction;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        restCreateAction = new RestCreateAction();
    }

    @Test
    public void testRoutes() {
        List<Route> routes = restCreateAction.routes();
        assertEquals(1, routes.size());
        assertEquals(Method.PUT, routes.get(0).getMethod());
        assertEquals("/crud/create", routes.get(0).getPath());
    }

    @Test
    public void testHandleRequest() {
        assertEquals("PUT /create successful", restCreateAction.handleRequest(Method.PUT, "/crud/create"));
        assertNull(restCreateAction.handleRequest(Method.GET, "/crud/create"));
        assertNull(restCreateAction.handleRequest(Method.GET, "/crud/validate"));
    }

}
