/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.rest;

import org.opensearch.common.bytes.BytesArray;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.rest.RestStatus;
import org.opensearch.test.OpenSearchTestCase;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class TestBaseExtensionRestHandler extends OpenSearchTestCase {

    private final BaseExtensionRestHandler handler = new BaseExtensionRestHandler() {
        @Override
        public List<RouteHandler> routeHandlers() {
            return List.of(new RouteHandler(Method.GET, "/foo", handleFoo));
        }

        @Override
        public List<DeprecatedRouteHandler> deprecatedRouteHandlers() {
            return List.of(new DeprecatedRouteHandler(Method.GET, "/deprecated/foo", "It's deprecated", handleFoo));
        }

        @Override
        public List<ReplacedRouteHandler> replacedRouteHandlers() {
            return List.of(
                new ReplacedRouteHandler(Method.GET, "/new/foo", Method.GET, "/old/foo", handleFoo),
                new ReplacedRouteHandler(Method.PUT, "/new/put/foo", "/old/put/foo", handleFoo),
                new ReplacedRouteHandler(new Route(Method.POST, "/foo"), "/new", "/old", handleFoo)
            );
        }

        private final Function<RestRequest, ExtensionRestResponse> handleFoo = (request) -> {
            try {
                if ("bar".equals(request.content().utf8ToString())) {
                    return createJsonResponse(request, RestStatus.OK, "success", "bar");
                }
                throw new IllegalArgumentException("no bar");
            } catch (Exception e) {
                return exceptionalRequest(request, e);
            }
        };
    };

    @Test
    public void testHandlerDefaultRoutes() {
        BaseExtensionRestHandler defaultHandler = new BaseExtensionRestHandler() {
        };
        assertTrue(defaultHandler.routes().isEmpty());
        assertTrue(defaultHandler.routeHandlers().isEmpty());
    }

    @Test
    public void testJsonResponse() {
        RestRequest successfulRequest = TestSDKRestRequest.createTestRestRequest(
            Method.GET,
            "/foo",
            "/foo",
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            new BytesArray("bar".getBytes(StandardCharsets.UTF_8)),
            "",
            null
        );
        ExtensionRestResponse response = handler.handleRequest(successfulRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals("{\"success\":\"bar\"}", response.content().utf8ToString());
    }

    @Test
    public void testJsonDeprecatedResponse() {
        RestRequest successfulRequest = TestSDKRestRequest.createTestRestRequest(
            Method.GET,
            "/deprecated/foo",
            "/deprecated/foo",
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            new BytesArray("bar".getBytes(StandardCharsets.UTF_8)),
            "",
            null
        );
        ExtensionRestResponse response = handler.handleRequest(successfulRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals("{\"success\":\"bar\"}", response.content().utf8ToString());
    }

    @Test
    public void testJsonReplacedResponseGet() {
        RestRequest successfulRequestOld = TestSDKRestRequest.createTestRestRequest(
            Method.GET,
            "/old/foo",
            "/old/foo",
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            new BytesArray("bar".getBytes(StandardCharsets.UTF_8)),
            "",
            null
        );
        RestRequest successfulRequestNew = TestSDKRestRequest.createTestRestRequest(
            Method.GET,
            "/new/foo",
            "/new/foo",
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            new BytesArray("bar".getBytes(StandardCharsets.UTF_8)),
            "",
            null
        );
        ExtensionRestResponse response = handler.handleRequest(successfulRequestOld);
        assertEquals(RestStatus.OK, response.status());
        assertEquals("{\"success\":\"bar\"}", response.content().utf8ToString());

        response = handler.handleRequest(successfulRequestNew);
        assertEquals(RestStatus.OK, response.status());
        assertEquals("{\"success\":\"bar\"}", response.content().utf8ToString());
    }

    @Test
    public void testJsonReplacedResponsePut() {
        RestRequest successfulRequestOld = TestSDKRestRequest.createTestRestRequest(
            Method.PUT,
            "/old/put/foo",
            "/old/put/foo",
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            new BytesArray("bar".getBytes(StandardCharsets.UTF_8)),
            "",
            null
        );
        RestRequest successfulRequestNew = TestSDKRestRequest.createTestRestRequest(
            Method.PUT,
            "/new/put/foo",
            "/new/put/foo",
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            new BytesArray("bar".getBytes(StandardCharsets.UTF_8)),
            "",
            null
        );
        ExtensionRestResponse response = handler.handleRequest(successfulRequestOld);
        assertEquals(RestStatus.OK, response.status());
        assertEquals("{\"success\":\"bar\"}", response.content().utf8ToString());

        response = handler.handleRequest(successfulRequestNew);
        assertEquals(RestStatus.OK, response.status());
        assertEquals("{\"success\":\"bar\"}", response.content().utf8ToString());
    }

    @Test
    public void testJsonReplacedResponsePost() {
        RestRequest successfulRequestOld = TestSDKRestRequest.createTestRestRequest(
            Method.POST,
            "/old/foo",
            "/old/foo",
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            new BytesArray("bar".getBytes(StandardCharsets.UTF_8)),
            "",
            null
        );
        RestRequest successfulRequestNew = TestSDKRestRequest.createTestRestRequest(
            Method.POST,
            "/new/foo",
            "/new/foo",
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            new BytesArray("bar".getBytes(StandardCharsets.UTF_8)),
            "",
            null
        );
        ExtensionRestResponse response = handler.handleRequest(successfulRequestOld);
        assertEquals(RestStatus.OK, response.status());
        assertEquals("{\"success\":\"bar\"}", response.content().utf8ToString());

        response = handler.handleRequest(successfulRequestNew);
        assertEquals(RestStatus.OK, response.status());
        assertEquals("{\"success\":\"bar\"}", response.content().utf8ToString());
    }

    @Test
    public void testErrorResponseOnException() {
        RestRequest exceptionalRequest = TestSDKRestRequest.createTestRestRequest(
            Method.GET,
            "/foo",
            "/foo",
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            new BytesArray("baz".getBytes(StandardCharsets.UTF_8)),
            "",
            null
        );
        ExtensionRestResponse response = handler.handleRequest(exceptionalRequest);
        assertEquals(RestStatus.INTERNAL_SERVER_ERROR, response.status());
        assertEquals("{\"error\":\"Request failed with exception: [no bar]\"}", response.content().utf8ToString());
    }

    @Test
    public void testErrorResponseOnUnhandled() {
        RestRequest unhandledRequestMethod = TestSDKRestRequest.createTestRestRequest(
            Method.PUT,
            "/foo",
            "/foo",
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            new BytesArray(new byte[0]),
            "",
            null
        );
        ExtensionRestResponse response = handler.handleRequest(unhandledRequestMethod);
        assertEquals(RestStatus.NOT_FOUND, response.status());
        assertEquals(
            "{\"error\":\"Extension REST action improperly configured to handle: ["
                + unhandledRequestMethod.method()
                + " "
                + unhandledRequestMethod.uri()
                + "]\"}",
            response.content().utf8ToString()
        );

        RestRequest unhandledRequestPath = TestSDKRestRequest.createTestRestRequest(
            Method.GET,
            "foobar",
            "foobar",
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            new BytesArray(new byte[0]),
            "",
            null
        );
        response = handler.handleRequest(unhandledRequestPath);
        assertEquals(RestStatus.NOT_FOUND, response.status());
        assertEquals(
            "{\"error\":\"Extension REST action improperly configured to handle: ["
                + unhandledRequestPath.method()
                + " "
                + unhandledRequestPath.uri()
                + "]\"}",
            response.content().utf8ToString()
        );
    }

    @Test
    public void testCreateEmptyJsonResponse() {
        BaseExtensionRestHandler handlerWithEmptyJsonResponse = new BaseExtensionRestHandler() {
            @Override
            public List<RouteHandler> routeHandlers() {
                return List.of(new RouteHandler(Method.GET, "/emptyJsonResponse", handleEmptyJsonResponse));
            }

            private final Function<RestRequest, ExtensionRestResponse> handleEmptyJsonResponse = (request) -> createEmptyJsonResponse(
                request,
                RestStatus.OK
            );
        };

        RestRequest emptyJsonResponseRequest = TestSDKRestRequest.createTestRestRequest(
            Method.GET,
            "/emptyJsonResponse",
            "/emptyJsonResponse",
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            new BytesArray(new byte[0]),
            "",
            null
        );
        ExtensionRestResponse response = handlerWithEmptyJsonResponse.handleRequest(emptyJsonResponseRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals("{}", response.content().utf8ToString());
    }
}
