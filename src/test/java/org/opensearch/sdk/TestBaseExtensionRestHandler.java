/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.opensearch.common.bytes.BytesArray;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.rest.RestStatus;
import org.opensearch.rest.extensions.ExtensionRestRequest;
import org.opensearch.rest.extensions.ExtensionRestResponse;
import org.opensearch.rest.extensions.RouteHandler;
import org.opensearch.test.OpenSearchTestCase;

public class TestBaseExtensionRestHandler extends OpenSearchTestCase {

    private final BaseExtensionRestHandler handler = new BaseExtensionRestHandler() {
        @Override
        public List<RouteHandler> routeHandlers() {
            return List.of(new RouteHandler(Method.GET, "foo", handleFoo));
        }

        private Function<ExtensionRestRequest, ExtensionRestResponse> handleFoo = (request) -> {
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
    public void testJsonErrorResponse() {
        ExtensionRestRequest successfulRequest = new ExtensionRestRequest(
            Method.GET,
            "foo",
            Collections.emptyMap(),
            null,
            new BytesArray("bar".getBytes(StandardCharsets.UTF_8)),
            ""
        );
        ExtensionRestResponse response = handler.handleRequest(successfulRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals("{\"success\":\"bar\"}", response.content().utf8ToString());
    }

    @Test
    public void testErrorResponseOnException() {
        ExtensionRestRequest exceptionalRequest = new ExtensionRestRequest(
            Method.GET,
            "foo",
            Collections.emptyMap(),
            null,
            new BytesArray("baz".getBytes(StandardCharsets.UTF_8)),
            ""
        );
        ExtensionRestResponse response = handler.handleRequest(exceptionalRequest);
        assertEquals(RestStatus.INTERNAL_SERVER_ERROR, response.status());
        assertEquals("{\"error\":\"Request failed with exception: [no bar]\"}", response.content().utf8ToString());
    }

    @Test
    public void testErrorResponseOnUnhandled() {
        ExtensionRestRequest unhandledRequestMethod = new ExtensionRestRequest(
            Method.PUT,
            "foo",
            Collections.emptyMap(),
            null,
            new BytesArray(new byte[0]),
            ""
        );
        ExtensionRestResponse response = handler.handleRequest(unhandledRequestMethod);
        assertEquals(RestStatus.NOT_FOUND, response.status());
        assertEquals(
            "{\"error\":\"Extension REST action improperly configured to handle: [" + unhandledRequestMethod + "]\"}",
            response.content().utf8ToString()
        );

        ExtensionRestRequest unhandledRequestPath = new ExtensionRestRequest(
            Method.GET,
            "foobar",
            Collections.emptyMap(),
            null,
            new BytesArray(new byte[0]),
            ""
        );
        response = handler.handleRequest(unhandledRequestPath);
        assertEquals(RestStatus.NOT_FOUND, response.status());
        assertEquals(
            "{\"error\":\"Extension REST action improperly configured to handle: [" + unhandledRequestPath + "]\"}",
            response.content().utf8ToString()
        );

    }
}
