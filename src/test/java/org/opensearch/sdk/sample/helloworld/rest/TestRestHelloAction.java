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
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.common.bytes.BytesArray;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.rest.RestResponse;
import org.opensearch.rest.RestStatus;
import org.opensearch.sdk.ExtensionRestHandler;
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

        ExtensionRestRequest getRequest = new ExtensionRestRequest(Method.GET, "/hello", params, null, new BytesArray(""), token);
        ExtensionRestRequest putRequest = new ExtensionRestRequest(
            Method.PUT,
            "/hello/Passing+Test",
            Map.of("name", "Passing+Test"),
            null,
            new BytesArray(""),
            token
        );
        ExtensionRestRequest postRequest = new ExtensionRestRequest(
            Method.POST,
            "/hello",
            params,
            XContentType.JSON,
            new BytesArray("{\"adjective\":\"testable\"}"),
            token
        );
        ExtensionRestRequest badPostRequest = new ExtensionRestRequest(
            Method.POST,
            "/hello",
            params,
            XContentType.JSON,
            new BytesArray("{\"adjective\":\"\"}"),
            token
        );
        ExtensionRestRequest noContentPostRequest = new ExtensionRestRequest(
            Method.POST,
            "/hello",
            params,
            null,
            new BytesArray(""),
            token
        );
        ExtensionRestRequest badContentTypePostRequest = new ExtensionRestRequest(
            Method.POST,
            "/hello",
            params,
            XContentType.YAML,
            new BytesArray("yaml:"),
            token
        );
        ExtensionRestRequest deleteRequest = new ExtensionRestRequest(Method.DELETE, "/goodbye", params, null, new BytesArray(""), token);
        ExtensionRestRequest badRequest = new ExtensionRestRequest(
            Method.PUT,
            "/hello/Bad%Request",
            Map.of("name", "Bad%Request"),
            null,
            new BytesArray(""),
            token
        );
        ExtensionRestRequest unhandledMethodRequest = new ExtensionRestRequest(Method.HEAD, "/hi", params, null, new BytesArray(""), token);
        ExtensionRestRequest unhandledPathRequest = new ExtensionRestRequest(Method.GET, "/hi", params, null, new BytesArray(""), token);
        ExtensionRestRequest unhandledPathLengthRequest = new ExtensionRestRequest(
            Method.DELETE,
            "/goodbye/cruel/world",
            params,
            null,
            new BytesArray(""),
            token
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

        // Unparseable
        response = restHelloAction.handleRequest(badRequest);
        assertEquals(RestStatus.BAD_REQUEST, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("Illegal hex characters in escape (%) pattern"));

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
}
