/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.helloworld.rest;

import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.sdk.ExtensionRestResponse;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestRequest.Method.PUT;
import static org.opensearch.rest.RestStatus.BAD_REQUEST;
import static org.opensearch.rest.RestStatus.NOT_FOUND;
import static org.opensearch.rest.RestStatus.OK;

/**
 * Sample REST Handler (REST Action). Extension REST handlers must implement {@link ExtensionRestHandler}.
 */
public class RestHelloAction implements ExtensionRestHandler {

    private static final String GREETING = "Hello, %s!";
    private String worldName = "World";

    @Override
    public List<Route> routes() {
        return List.of(new Route(GET, "/hello"), new Route(PUT, "/hello/{name}"));
    }

    @Override
    public ExtensionRestResponse handleRequest(ExtensionRestRequest request) {
        Method method = request.method();

        if (Method.GET.equals(method)) {
            return handleGetRequest(request);
        } else if (Method.PUT.equals(method)) {
            return handlePutRequest(request);
        }
        return handleBadRequest(request);
    }

    private ExtensionRestResponse handleGetRequest(ExtensionRestRequest request) {
        return new ExtensionRestResponse(request, OK, String.format(GREETING, worldName));
    }

    private ExtensionRestResponse handlePutRequest(ExtensionRestRequest request) {
        String name = request.param("name");
        try {
            worldName = URLDecoder.decode(name, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return new ExtensionRestResponse(request, BAD_REQUEST, e.getMessage());
        }
        return new ExtensionRestResponse(request, OK, "Updated the world's name to " + worldName);
    }

    private ExtensionRestResponse handleBadRequest(ExtensionRestRequest request) {
        return new ExtensionRestResponse(request, NOT_FOUND, "Extension REST action improperly configured to handle " + request.toString());
    }

}
