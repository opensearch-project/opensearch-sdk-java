/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.rest;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.Before;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestClient;
import org.opensearch.rest.RestStatus;
import org.opensearch.test.rest.OpenSearchRestTestCase;

public class TestHelloWorldIT extends OpenSearchRestTestCase {
    private static final Logger logger = LogManager.getLogger(TestHelloWorldIT.class);

    public static boolean initialized = false;
    public static final String EXTENSION_INIT_URI = "/_extensions/initialize/";
    public static final String HELLO_WORLD_EXTENSION_BASE_URI = "/_extensions/_hello-world";
    public static final String HELLO_BASE_URI = HELLO_WORLD_EXTENSION_BASE_URI + "/hello";
    public static final String HELLO_NAME_URI = HELLO_BASE_URI + "/%s";
    public static final String GOODBYE_URI = HELLO_WORLD_EXTENSION_BASE_URI + "/goodbye";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        if (!initialized) {
            // Send initialization request
            String helloWorldInitRequestBody = "{\"name\":\"hello-world\""
                + ",\"uniqueId\":\"hello-world\""
                + ",\"hostAddress\":\"127.0.0.1\""
                + ",\"port\":\"4500\""
                + ",\"version\":\"1.0\""
                + ",\"opensearchVersion\":\"3.0.0\""
                + ",\"minimumCompatibleVersion\":\"3.0.0\"}";
            Response response = makeRequest(client(), "POST", EXTENSION_INIT_URI, null, toHttpEntity(helloWorldInitRequestBody));

            assertEquals(RestStatus.ACCEPTED, restStatus(response));
            Map<String, Object> responseMap = entityAsMap(response);
            String initializationResponse = (String) responseMap.get("success");
            assertEquals("A request to initialize an extension has been sent.", initializationResponse);
            initialized = true;

            // Wait for extension settings/rest handlers to complete registration before any subsequent requests
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        }
    }

    /**
     * Retrieves the REST status from a response
     *
     * @param response the REST response
     * @return the RestStatus of the response
     *
     */
    private static RestStatus restStatus(Response response) {
        return RestStatus.fromCode(response.getStatusLine().getStatusCode());
    }

    /**
     * Converts a JSON string into an HttpEntity
     *
     * @param jsonString The JSON string
     * @return the HttpEntity
     *
     */
    private static HttpEntity toHttpEntity(String jsonString) throws IOException {
        return new StringEntity(jsonString, ContentType.APPLICATION_JSON);
    }

    /**
     * Helper method to send a REST Request
     *
     * @param client The REST client
     * @param method The request method
     * @param endpoint The REST endpoint
     * @param params The request parameters
     * @param entity The request body
     * @return the REST response
     *
     */
    private static Response makeRequest(RestClient client, String method, String endpoint, Map<String, String> params, HttpEntity entity)
        throws IOException {

        // Create request
        Request request = new Request(method, endpoint);
        if (params != null) {
            params.entrySet().forEach(it -> request.addParameter(it.getKey(), it.getValue()));
        }
        if (entity != null) {
            request.setEntity(entity);
        }
        return client.performRequest(request);
    }

    /**
     * Invokes the callable method and asserts that the expected exception is thrown with the given exception message
     *
     * @param clazz The exception class
     * @param message The exception message
     * @param callable The callable request method
     *
     */
    private static <S, T> void assertFailWith(Class<S> clazz, String message, Callable<T> callable) throws Exception {
        try {
            callable.call();
        } catch (Throwable e) {
            if (e.getClass() != clazz) {
                throw e;
            }
            logger.info("TESTING MESSAGE : " + e.getMessage());
            if (message != null && !e.getMessage().contains(message)) {
                throw e;
            }
        }
    }
}
