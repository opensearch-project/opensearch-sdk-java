/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.rest;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.sdk.ExtensionAction;
import org.opensearch.test.OpenSearchTestCase;

public class TestRestHelloAction extends OpenSearchTestCase {

    private ExtensionAction helloAction;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        helloAction = new RestHelloAction();
    }

    @Test
    public void testRoutes() {
        List<Route> routes = helloAction.routes();
        assertEquals(1, routes.size());
        assertEquals(Method.GET, routes.get(0).getMethod());
        assertEquals("/hello", routes.get(0).getPath());
    }

    @Test
    public void testExtensioResponse() {
        assertEquals("Hello, World!", helloAction.getExtensionResponse(Method.GET, "/hello"));
        assertNull(helloAction.getExtensionResponse(Method.PUT, "/hello"));
        assertNull(helloAction.getExtensionResponse(Method.GET, "/goodbye"));
    }

}
