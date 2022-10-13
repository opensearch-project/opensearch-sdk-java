/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.helloworld.rest;

import org.opensearch.OpenSearchParseException;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.sdk.ExtensionRestHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.opensearch.rest.RestRequest.Method.DELETE;
import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestRequest.Method.POST;
import static org.opensearch.rest.RestRequest.Method.PUT;
import static org.opensearch.rest.RestStatus.BAD_REQUEST;
import static org.opensearch.rest.RestStatus.NOT_ACCEPTABLE;
import static org.opensearch.rest.RestStatus.NOT_FOUND;
import static org.opensearch.rest.RestStatus.OK;

/**
 * Sample REST Handler (REST Action). Extension REST handlers must implement {@link ExtensionRestHandler}.
 */
public class RestHelloAction implements ExtensionRestHandler {

    private static final String GREETING = "Hello, %s!";
    private static final String DEFAULT_NAME = "World";

    private String worldName = DEFAULT_NAME;
    private List<String> worldAdjectives = new ArrayList<>();
    private Random rand = new Random();

    @Override
    public List<Route> routes() {
        return List.of(new Route(GET, "/hello"), new Route(POST, "/hello"), new Route(PUT, "/hello/{name}"), new Route(DELETE, "/goodbye"));
    }

    @Override
    public ExtensionRestResponse handleRequest(ExtensionRestRequest request) {
        Method method = request.method();

        if (Method.GET.equals(method)) {
            return handleGetRequest(request);
        } else if (Method.POST.equals(method)) {
            return handlePostRequest(request);
        } else if (Method.PUT.equals(method)) {
            return handlePutRequest(request);
        } else if (Method.DELETE.equals(method)) {
            return handleDeleteRequest(request);
        }
        return handleBadRequest(request);
    }

    private ExtensionRestResponse handleGetRequest(ExtensionRestRequest request) {
        String worldNameWithRandomAdjective = worldAdjectives.isEmpty()
            ? worldName
            : String.join(" ", worldAdjectives.get(rand.nextInt(worldAdjectives.size())), worldName);
        return new ExtensionRestResponse(request, OK, String.format(GREETING, worldNameWithRandomAdjective));
    }

    private ExtensionRestResponse handlePostRequest(ExtensionRestRequest request) {
        if (request.hasContent()) {
            String adjective = "";
            XContentType contentType = request.getXContentType();
            if (contentType == null) {
                // Plain text
                adjective = request.content().utf8ToString();
            } else if (contentType.equals(XContentType.JSON)) {
                try {
                    adjective = request.contentParser(NamedXContentRegistry.EMPTY).mapStrings().get("adjective");
                } catch (IOException | OpenSearchParseException e) {
                    return new ExtensionRestResponse(request, BAD_REQUEST, "Unable to parse adjective from JSON");
                }
            } else {
                return new ExtensionRestResponse(request, NOT_ACCEPTABLE, "Only text and JSON content types are supported");
            }
            if (!adjective.isBlank()) {
                worldAdjectives.add(adjective);
                return new ExtensionRestResponse(request, OK, "Added " + adjective + " to words that describe the world!");
            }
            return new ExtensionRestResponse(request, BAD_REQUEST, "No adjective included with POST request");
        }
        return new ExtensionRestResponse(request, BAD_REQUEST, "No content included with POST request");
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

    private ExtensionRestResponse handleDeleteRequest(ExtensionRestRequest request) {
        this.worldName = DEFAULT_NAME;
        this.worldAdjectives.clear();
        return new ExtensionRestResponse(request, OK, "Goodbye, cruel world! Restored default values.");
    }

    private ExtensionRestResponse handleBadRequest(ExtensionRestRequest request) {
        return new ExtensionRestResponse(request, NOT_FOUND, "Extension REST action improperly configured to handle " + request.toString());
    }
}
