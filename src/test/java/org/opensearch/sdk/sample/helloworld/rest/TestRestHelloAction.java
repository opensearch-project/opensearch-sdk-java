/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.helloworld.rest;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.identity.ExtensionTokenProcessor;
import org.opensearch.identity.PrincipalIdentifierToken;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestResponse;
import org.opensearch.rest.RestStatus;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.test.OpenSearchTestCase;

public class TestRestHelloAction extends OpenSearchTestCase {

    private ExtensionRestHandler restHelloAction;
    private static final String EXTENSION_NAME = "hello-world";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        restHelloAction = new RestHelloAction();
    }

    @Test
    public void testRoutes() {
        List<Route> routes = restHelloAction.routes();
        assertEquals(2, routes.size());
        assertEquals(Method.GET, routes.get(0).getMethod());
        assertEquals("/hello", routes.get(0).getPath());
        assertEquals(Method.PUT, routes.get(1).getMethod());
        assertEquals("/hello/{name}", routes.get(1).getPath());
    }

    @Test
    public void testHandleRequest() {
        Principal userPrincipal = () -> "user1";
        ExtensionTokenProcessor extensionTokenProcessor = new ExtensionTokenProcessor(EXTENSION_NAME);
        PrincipalIdentifierToken token = extensionTokenProcessor.generateToken(userPrincipal);
        Map<String, String> params = Collections.emptyMap();

        ExtensionRestRequest getRequest = new ExtensionRestRequest(Method.GET, "/hello", params, token);
        ExtensionRestRequest putRequest = new ExtensionRestRequest(Method.PUT, "/hello", params, token);
        ExtensionRestRequest updateRequest = new ExtensionRestRequest(Method.PUT, "/hello/Passing+Test", params, token);
        ExtensionRestRequest badRequest = new ExtensionRestRequest(Method.PUT, "/hello/Bad%Request", params, token);
        ExtensionRestRequest unsuccessfulRequest = new ExtensionRestRequest(Method.GET, "/goodbye", params, token);

        RestResponse response = restHelloAction.handleRequest(getRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals(BytesRestResponse.TEXT_CONTENT_TYPE, response.contentType());
        String responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertEquals("Hello, World!", responseStr);

        response = restHelloAction.handleRequest(putRequest);
        assertEquals(RestStatus.NOT_FOUND, response.status());
        assertEquals(BytesRestResponse.TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("PUT"));

        response = restHelloAction.handleRequest(updateRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals(BytesRestResponse.TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertEquals("Updated the world's name to Passing Test", responseStr);

        response = restHelloAction.handleRequest(getRequest);
        assertEquals(RestStatus.OK, response.status());
        assertEquals(BytesRestResponse.TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertEquals("Hello, Passing Test!", responseStr);

        response = restHelloAction.handleRequest(badRequest);
        assertEquals(RestStatus.BAD_REQUEST, response.status());
        assertEquals(BytesRestResponse.TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("Illegal hex characters in escape (%) pattern"));

        response = restHelloAction.handleRequest(unsuccessfulRequest);
        assertEquals(RestStatus.NOT_FOUND, response.status());
        assertEquals(BytesRestResponse.TEXT_CONTENT_TYPE, response.contentType());
        responseStr = new String(BytesReference.toBytes(response.content()), StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("/goodbye"));
    }

}
