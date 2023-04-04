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
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.opensearch.common.bytes.BytesArray;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.http.HttpRequest;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.sdk.rest.SDKRestRequest;
import org.opensearch.rest.RestStatus;
import org.opensearch.test.OpenSearchTestCase;

public class TestBaseExtensionRestHandler extends OpenSearchTestCase {

    private final BaseExtensionRestHandler handler = new BaseExtensionRestHandler() {
        @Override
        public List<RouteHandler> routeHandlers() {
            return List.of(new RouteHandler(Method.GET, "foo", handleFoo));
        }

        private Function<RestRequest, ExtensionRestResponse> handleFoo = (request) -> {
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
        RestRequest successfulRequest = createTestRestRequest(
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
        RestRequest exceptionalRequest = createTestRestRequest(
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
        RestRequest unhandledRequestMethod = createTestRestRequest(
            Method.PUT,
            "foo",
            "foo",
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

        RestRequest unhandledRequestPath = createTestRestRequest(
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

    public static RestRequest createTestRestRequest(
        final Method method,
        final String uri,
        final String path,
        final Map<String, String> params,
        final Map<String, String> headers,
        final XContentType xContentType,
        final BytesReference content,
        final String principalIdentifier,
        final HttpRequest.HttpVersion httpVersion
    ) {
        ExtensionRestRequest request = new ExtensionRestRequest(
            method,
            uri,
            path,
            params,
            headers,
            xContentType,
            content,
            principalIdentifier,
            httpVersion
        );
        return new SDKRestRequest(null, request.params(), request.path(), request.headers(), new SDKHttpRequest(request), null);
    }
}
