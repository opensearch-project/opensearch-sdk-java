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
import java.util.Map.Entry;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.opensearch.common.bytes.BytesArray;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.http.HttpRequest;
import org.opensearch.http.HttpResponse;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;
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
            Collections.emptyMap(),
            null,
            new BytesArray(new byte[0]),
            ""
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
            Collections.emptyMap(),
            null,
            new BytesArray(new byte[0]),
            ""
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
        final String path,
        final Map<String, String> params,
        final XContentType xContentType,
        final BytesReference content,
        final String principalIdentifier
    ) {
        // Temporary code to create a RestRequest from the ExtensionRestRequest before header code added
        // Remove this and replace with SDKRestRequest being generated by this PR:
        // https://github.com/opensearch-project/opensearch-sdk-java/pull/605
        return RestRequest.request(null, new HttpRequest() {

            @Override
            public Method method() {
                return method;
            }

            @Override
            public String uri() {
                StringBuilder uri = new StringBuilder();
                for (Entry<String, String> param : params.entrySet()) {
                    uri.append(uri.length() == 0 ? '?' : '&').append(param.getKey()).append('=').append(param.getValue());
                }
                return path + uri.toString();
            }

            @Override
            public BytesReference content() {
                return content;
            }

            @Override
            public Map<String, List<String>> getHeaders() {
                return xContentType == null ? Collections.emptyMap() : Map.of("Content-Type", List.of(xContentType.mediaType()));
            }

            @Override
            public List<String> strictCookies() {
                return Collections.emptyList();
            }

            @Override
            public HttpVersion protocolVersion() {
                return null;
            }

            @Override
            public HttpRequest removeHeader(String header) {
                // we don't use
                return null;
            }

            @Override
            public HttpResponse createResponse(RestStatus status, BytesReference content) {
                return null;
            }

            @Override
            public Exception getInboundException() {
                return null;
            }

            @Override
            public void release() {}

            @Override
            public HttpRequest releaseAndCopy() {
                return null;
            }
        }, null);
    }
}
