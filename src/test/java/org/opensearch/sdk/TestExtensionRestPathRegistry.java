/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.sdk.rest.ExtensionRestPathRegistry;
import org.opensearch.test.OpenSearchTestCase;

public class TestExtensionRestPathRegistry extends OpenSearchTestCase {

    private ExtensionRestPathRegistry extensionRestPathRegistry = new ExtensionRestPathRegistry();

    private ExtensionRestHandler fooHandler = new ExtensionRestHandler() {
        @Override
        public List<Route> routes() {
            return List.of(new Route(Method.GET, "/foo"));
        }

        @Override
        public ExtensionRestResponse handleRequest(RestRequest request) {
            return null;
        }
    };
    private ExtensionRestHandler barHandler = new ExtensionRestHandler() {
        @Override
        public List<Route> routes() {
            return List.of(new Route(Method.PUT, "/bar/{planet}"));
        }

        @Override
        public ExtensionRestResponse handleRequest(RestRequest request) {
            return null;
        }
    };
    private ExtensionRestHandler bazHandler = new ExtensionRestHandler() {
        @Override
        public List<Route> routes() {
            return List.of(new Route(Method.POST, "/baz/{moon}/qux"), new Route(Method.PUT, "/bar/baz"));
        }

        @Override
        public ExtensionRestResponse handleRequest(RestRequest request) {
            return null;
        }
    };

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        List<ExtensionRestHandler> handlerList = List.of(fooHandler, barHandler, bazHandler);
        super.setUp();
        for (ExtensionRestHandler handler : handlerList) {
            for (Route route : handler.routes()) {
                extensionRestPathRegistry.registerHandler(route.getMethod(), route.getPath(), handler);
            }
        }
    }

    @Test
    public void testRegisterConflicts() {
        // Can't register same exact name
        assertThrows(IllegalArgumentException.class, () -> extensionRestPathRegistry.registerHandler(Method.GET, "/foo", fooHandler));
        // Can't register conflicting named wildcards, even if method is different
        assertThrows(
            IllegalArgumentException.class,
            () -> extensionRestPathRegistry.registerHandler(Method.GET, "/bar/{none}", barHandler)
        );
    }

    @Test
    public void testGetHandler() {
        assertEquals(fooHandler, extensionRestPathRegistry.getHandler(Method.GET, "/foo"));
        assertNull(extensionRestPathRegistry.getHandler(Method.PUT, "/foo"));

        // Exact match and wildcard match can overlap, exact takes priority
        assertEquals(barHandler, extensionRestPathRegistry.getHandler(Method.PUT, "/bar/mars"));
        assertEquals(bazHandler, extensionRestPathRegistry.getHandler(Method.PUT, "/bar/baz"));
        assertNull(extensionRestPathRegistry.getHandler(Method.PUT, "/bar/mars/bar"));

        assertEquals(bazHandler, extensionRestPathRegistry.getHandler(Method.POST, "/baz/europa/qux"));
        assertNull(extensionRestPathRegistry.getHandler(Method.POST, "/bar/europa"));
    }

    @Test
    public void testGetRegisteredPaths() {
        List<String> registeredPaths = extensionRestPathRegistry.getRegisteredPaths();
        assertTrue(registeredPaths.contains("GET /foo"));
        assertTrue(registeredPaths.contains("PUT /bar/{planet}"));
        assertTrue(registeredPaths.contains("PUT /bar/baz"));
        assertTrue(registeredPaths.contains("POST /baz/{moon}/qux"));
    }

    @Test
    public void testRestPathToString() {
        assertEquals("GET /foo", ExtensionRestPathRegistry.restPathToString(Method.GET, "/foo"));
    }
}
