/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.rest;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.RestHandler.DeprecatedRoute;
import org.opensearch.rest.RestHandler.ReplacedRoute;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.sdk.rest.BaseExtensionRestHandler.ExtensionDeprecationRestHandler;
import org.opensearch.test.OpenSearchTestCase;

public class TestExtensionRestPathRegistry extends OpenSearchTestCase {

    private ExtensionRestPathRegistry extensionRestPathRegistry = new ExtensionRestPathRegistry();

    private ExtensionRestHandler fooHandler = new ExtensionRestHandler() {
        @Override
        public List<Route> routes() {
            return List.of(new Route(Method.GET, "/foo"));
        }

        @Override
        public List<DeprecatedRoute> deprecatedRoutes() {
            return List.of(new DeprecatedRoute(Method.POST, "/deprecated/foo", "It's deprecated!"));
        }

        @Override
        public ExtensionRestResponse handleRequest(RestRequest request) {
            return null;
        }
    };
    private BaseExtensionRestHandler replacedFooHandler = new BaseExtensionRestHandler() {
        @Override
        public List<ReplacedRoute> replacedRoutes() {
            return List.of(
                new ReplacedRouteHandler(Method.GET, "/new/foo", Method.GET, "/old/foo", r -> { return null; }),
                new ReplacedRouteHandler(Method.PUT, "/new/put/foo", "/old/put/foo", r -> {
                    return null;
                }),
                new ReplacedRouteHandler(new Route(Method.POST, "/foo"), "/new", "/old", r -> { return null; })
            );
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
        List<ExtensionRestHandler> handlerList = List.of(fooHandler, replacedFooHandler, barHandler, bazHandler);
        super.setUp();
        for (ExtensionRestHandler handler : handlerList) {
            extensionRestPathRegistry.registerHandler(handler);
        }
    }

    @Test
    public void testRegisterConflicts() {
        // Can't register same exact name
        ExtensionRestHandler duplicateFooHandler = new ExtensionRestHandler() {
            @Override
            public List<Route> routes() {
                return List.of(new Route(Method.GET, "/foo"));
            }

            @Override
            public ExtensionRestResponse handleRequest(RestRequest request) {
                return null;
            }
        };
        assertThrows(IllegalArgumentException.class, () -> extensionRestPathRegistry.registerHandler(duplicateFooHandler));
        // Can't register conflicting named wildcards, even if method is different
        ExtensionRestHandler barNoneHandler = new ExtensionRestHandler() {
            @Override
            public List<Route> routes() {
                return List.of(new Route(Method.GET, "/bar/{none}"));
            }

            @Override
            public ExtensionRestResponse handleRequest(RestRequest request) {
                return null;
            }
        };
        assertThrows(IllegalArgumentException.class, () -> extensionRestPathRegistry.registerHandler(barNoneHandler));
    }

    @Test
    public void testGetHandler() {
        assertEquals(fooHandler, extensionRestPathRegistry.getHandler(Method.GET, "/foo"));
        assertNull(extensionRestPathRegistry.getHandler(Method.PUT, "/foo"));

        // Deprecated handler wraps the original
        ExtensionRestHandler deprecationHandler = extensionRestPathRegistry.getHandler(Method.POST, "/deprecated/foo");
        assertTrue(deprecationHandler instanceof ExtensionDeprecationRestHandler);
        assertEquals(fooHandler, ((ExtensionDeprecationRestHandler) deprecationHandler).getHandler());
        assertEquals("It's deprecated!", ((ExtensionDeprecationRestHandler) deprecationHandler).getDeprecationMessage());

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
    public void testGetRegisteredDeprecatedPaths() {
        List<String> registeredDeprecatedPaths = extensionRestPathRegistry.getRegisteredDeprecatedPaths();
        assertEquals(0, registeredDeprecatedPaths.size() % 2);
        int index = registeredDeprecatedPaths.indexOf("POST /deprecated/foo");
        assertTrue(index >= 0);
        assertEquals("It's deprecated!", registeredDeprecatedPaths.get(index + 1));
    }

    @Test
    public void testGetRegisteredReplacedPaths() {
        List<String> registeredPaths = extensionRestPathRegistry.getRegisteredPaths();
        assertTrue(registeredPaths.contains("GET /new/foo"));
        assertTrue(registeredPaths.contains("PUT /new/put/foo"));
        assertTrue(registeredPaths.contains("POST /new/foo"));

        List<String> registeredDeprecatedPaths = extensionRestPathRegistry.getRegisteredDeprecatedPaths();
        assertTrue(registeredDeprecatedPaths.contains("GET /old/foo"));
        int index = registeredDeprecatedPaths.indexOf("GET /old/foo");
        assertTrue(index >= 0);
        assertEquals("[GET /old/foo] is deprecated! Use [GET /new/foo] instead.", registeredDeprecatedPaths.get(index + 1));

        assertTrue(registeredDeprecatedPaths.contains("PUT /old/put/foo"));
        index = registeredDeprecatedPaths.indexOf("PUT /old/put/foo");
        assertTrue(index >= 0);
        assertEquals("[PUT /old/put/foo] is deprecated! Use [PUT /new/put/foo] instead.", registeredDeprecatedPaths.get(index + 1));

        assertTrue(registeredDeprecatedPaths.contains("POST /old/foo"));
        index = registeredDeprecatedPaths.indexOf("POST /old/foo");
        assertTrue(index >= 0);
        assertEquals("[POST /old/foo] is deprecated! Use [POST /new/foo] instead.", registeredDeprecatedPaths.get(index + 1));
    }

    @Test
    public void testRestPathToString() {
        assertEquals("GET /foo", ExtensionRestPathRegistry.restPathToString(Method.GET, "/foo", Optional.empty()));
    }

    @Test
    public void testRestPathWithNameToString() {
        assertEquals("GET /foo foo", ExtensionRestPathRegistry.restPathToString(Method.GET, "/foo", Optional.of("foo")));
    }
}
