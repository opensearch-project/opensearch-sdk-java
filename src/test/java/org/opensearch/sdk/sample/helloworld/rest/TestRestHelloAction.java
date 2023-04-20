/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.rest;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.common.bytes.BytesArray;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.http.HttpRequest.HttpVersion;
import org.opensearch.rest.RestResponse;
import org.opensearch.rest.RestStatus;
import org.opensearch.sdk.rest.ExtensionRestHandler;
import org.opensearch.sdk.rest.TestSDKRestRequest;
import org.opensearch.test.OpenSearchTestCase;

public class TestRestHelloAction extends OpenSearchTestCase {

    private static final String TEXT_CONTENT_TYPE = "text/plain; charset=UTF-8";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    private ExtensionRestHandler restHelloAction;
    // Temporarily removed pending integration of feature/identity branch
    // private static final String EXTENSION_NAME = "hello-world";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        restHelloAction = new RestHelloAction();
    }

    @Test
    public void testRoutes() {
        List<Route> routes = restHelloAction.routes();
        assertEquals(4, routes.size());
        assertEquals(Method.GET, routes.get(0).getMethod());
        assertEquals("/hello", routes.get(0).getPath());
        assertEquals(Method.POST, routes.get(1).getMethod());
        assertEquals("/hello", routes.get(1).getPath());
        assertEquals(Method.PUT, routes.get(2).getMethod());
        assertEquals("/hello/{name}", routes.get(2).getPath());
        assertEquals(Method.DELETE, routes.get(3).getMethod());
        assertEquals("/goodbye", routes.get(3).getPath());
    }

    @Test
    public void testHandleRequest() {
        // Temporarily removed pending integration of feature/identity branch
        // Principal userPrincipal = () -> "user1";
        // String extensionTokenProcessor = "placeholder_extension_token_processor";
        String token = "placeholder_token";
        Map<String, String> params = Collections.emptyMap();

        RestRequest getRequest = TestSDKRestRequest.createTestRestRequest(
            Method.GET,
            "/hello",
            "/hello",
            params,
            headers(XContentType.JSON),
            XContentType.JSON,
            new BytesArray(""),
            token,
            HttpVersion.HTTP_1_1
        );
        RestRequest putRequest = TestSDKRestRequest.createTestRestRequest(
            Method.PUT,
            "/hello/Passing+Test",
            "/hello/Passing+Test",
            Map.of("name", "Passing+Test"),
            headers(XContentType.JSON),
            XContentType.JSON,
            new BytesArray(""),
            token,
            HttpVersion.HTTP_1_1
        );
        RestRequest postRequest = TestSDKRestRequest.createTestRestRequest(
            Method.POST,
            "/hello",
            "/hello",
            params,
            headers(XContentType.JSON),
            XContentType.JSON,
            new BytesArray("{\"adjective\":\"testable\"}"),
            token,
            HttpVersion.HTTP_1_1
        );
        RestRequest badPostRequest = TestSDKRestRequest.createTestRestRequest(
            Method.POST,
            "/hello",
            "/hello",
            params,
            headers(XContentType.JSON),
            XContentType.JSON,
            new BytesArray("{\"adjective\":\"\"}"),
            token,
            HttpVersion.HTTP_1_1
        );
        RestRequest noContentPostRequest = TestSDKRestRequest.createTestRestRequest(
            Method.POST,
            "/hello",
            "/hello",
            params,
            headers(null),
            null,
            new BytesArray(""),
            token,
            HttpVersion.HTTP_1_1
        );
        RestRequest badContentTypePostRequest = TestSDKRestRequest.createTestRestRequest(
            Method.POST,
            "/hello",
            "/hello",
            params,
            headers(XContentType.YAML),
            XContentType.YAML,
            new BytesArray("yaml:"),
            token,
            HttpVersion.HTTP_1_1
        );
        RestRequest deleteRequest = TestSDKRestRequest.createTestRestRequest(
            Method.DELETE,
            "/goodbye",
            "/goodbye",
            params,
            headers(XContentType.JSON),
            XContentType.JSON,
            new BytesArray(""),
            token,
            HttpVersion.HTTP_1_1
        );
        RestRequest unhandledMethodRequest = TestSDKRestRequest.createTestRestRequest(
            Method.HEAD,
            "/hi",
            "/hi",
            params,
            headers(XContentType.JSON),
            XContentType.JSON,
            new BytesArray(""),
            token,
            HttpVersion.HTTP_1_1
        );
        RestRequest unhandledPathRequest = TestSDKRestRequest.createTestRestRequest(
            Method.GET,
            "/hi",
            "/hi",
            params,
            headers(XContentType.JSON),
            XContentType.JSON,
            new BytesArray(""),
            token,
            HttpVersion.HTTP_1_1
        );
        RestRequest unhandledPathLengthRequest = TestSDKRestRequest.createTestRestRequest(
            Method.DELETE,
            "/goodbye/cruel/world",
            "/goodbye/cruel/world",
            params,
            headers(XContentType.JSON),
            XContentType.JSON,
            new BytesArray(""),
            token,
            HttpVersion.HTTP_1_1
        );

        // Initial default response
        RestResponse response = restHelloAction.handleRequest(getRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        String responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertEquals("Hello, World!", responseStr);

        // Change world's name
        response = restHelloAction.handleRequest(putRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertEquals("Updated the world's name to Passing Test", responseStr);

        response = restHelloAction.handleRequest(getRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertEquals("Hello, Passing Test!", responseStr);

        // Add an adjective
        response = restHelloAction.handleRequest(postRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals(JSON_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("testable"));

        response = restHelloAction.handleRequest(getRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertEquals("Hello, testable Passing Test!", responseStr);

        // Try to add a blank adjective
        response = restHelloAction.handleRequest(badPostRequest);
        assertEquals(RestStatus.BAD_REQUEST, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertEquals("No adjective included with POST request", responseStr);

        // Try to add no content
        response = restHelloAction.handleRequest(noContentPostRequest);
        assertEquals(RestStatus.BAD_REQUEST, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertEquals("No content included with POST request", responseStr);

        // Try to add bad content type
        response = restHelloAction.handleRequest(badContentTypePostRequest);
        assertEquals(RestStatus.NOT_ACCEPTABLE, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertEquals("Only text and JSON content types are supported", responseStr);

        // Remove the name and adjective
        response = restHelloAction.handleRequest(deleteRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("Goodbye, cruel world!"));

        response = restHelloAction.handleRequest(getRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertEquals("Hello, World!", responseStr);

        // Not registered, fails on method
        response = restHelloAction.handleRequest(unhandledMethodRequest);
        assertEquals(RestStatus.NOT_FOUND, response.status());
        assertEquals(JSON_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("/hi"));

        // Not registered, fails on path name
        response = restHelloAction.handleRequest(unhandledPathRequest);
        assertEquals(RestStatus.NOT_FOUND, response.status());
        assertEquals(JSON_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("/hi"));

        // Not registered, fails on path length
        response = restHelloAction.handleRequest(unhandledPathLengthRequest);
        assertEquals(RestStatus.NOT_FOUND, response.status());
        assertEquals(JSON_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("/goodbye"));
    }

    private static Map<String, List<String>> headers(XContentType type) {
        return type == null ? Collections.emptyMap() : Map.of("Content-Type", List.of(type.mediaType()));
    }
}
