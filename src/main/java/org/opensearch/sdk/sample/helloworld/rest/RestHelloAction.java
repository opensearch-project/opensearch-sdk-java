/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.rest;

import org.opensearch.OpenSearchParseException;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.sdk.BaseExtensionRestHandler;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.sdk.RouteHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
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
import static org.opensearch.rest.RestStatus.OK;

/**
 * Sample REST Handler (REST Action).
 * Extension REST handlers must implement {@link ExtensionRestHandler}.
 * Extending {@link BaseExtensionRestHandler} provides many convenience methods.
 */
public class RestHelloAction extends BaseExtensionRestHandler {

    private static final String TEXT_CONTENT_TYPE = "text/plain;charset=UTF-8";
    private static final String GREETING = "Hello, %s!";
    private static final String DEFAULT_NAME = "World";

    private String worldName = DEFAULT_NAME;
    private List<String> worldAdjectives = new ArrayList<>();
    private Random rand = new Random();

    @Override
    public List<RouteHandler> routeHandlers() {
        return List.of(
            new RouteHandler(GET, "/hello", this::handleGetRequest),
            new RouteHandler(POST, "/hello", this::handlePostRequest),
            new RouteHandler(PUT, "/hello/{name}", this::handlePutRequest),
            new RouteHandler(DELETE, "/goodbye", this::handleDeleteRequest)
        );
    }

    private ExtensionRestResponse handleGetRequest() {
        String worldNameWithRandomAdjective = worldAdjectives.isEmpty()
            ? worldName
            : String.join(" ", worldAdjectives.get(rand.nextInt(worldAdjectives.size())), worldName);
        return createResponse(OK, String.format(GREETING, worldNameWithRandomAdjective));
    }

    private ExtensionRestResponse handlePostRequest() {
        ExtensionRestRequest request = getRequest();
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
                    // Sample plain text response
                    return createResponse(BAD_REQUEST, "Unable to parse adjective from JSON");
                }
            } else {
                // Sample text response with content type
                return createResponse(NOT_ACCEPTABLE, TEXT_CONTENT_TYPE, "Only text and JSON content types are supported");
            }
            if (adjective != null && !adjective.isBlank()) {
                worldAdjectives.add(adjective.trim());
                // Sample JSON response with a builder
                try {
                    XContentBuilder builder = JsonXContent.contentBuilder()
                        .startObject()
                        .field("worldAdjectives", worldAdjectives)
                        .endObject();
                    return createResponse(OK, builder);
                } catch (IOException e) {
                    // Sample response for developer error
                    return unhandledRequest();
                }
            }
            byte[] noAdjective = "No adjective included with POST request".getBytes(StandardCharsets.UTF_8);
            // Sample binary response with content type
            return createResponse(BAD_REQUEST, TEXT_CONTENT_TYPE, noAdjective);
        }
        // Sample bytes reference response with content type
        BytesReference noContent = BytesReference.fromByteBuffer(
            ByteBuffer.wrap("No content included with POST request".getBytes(StandardCharsets.UTF_8))
        );
        return createResponse(BAD_REQUEST, TEXT_CONTENT_TYPE, noContent);
    }

    private ExtensionRestResponse handlePutRequest() {
        ExtensionRestRequest request = getRequest();
        String name = request.param("name");
        try {
            worldName = URLDecoder.decode(name, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return createResponse(BAD_REQUEST, e.getMessage());
        }
        return createResponse(OK, "Updated the world's name to " + worldName);
    }

    private ExtensionRestResponse handleDeleteRequest() {
        this.worldName = DEFAULT_NAME;
        this.worldAdjectives.clear();
        return createResponse(OK, "Goodbye, cruel world! Restored default values.");
    }
}
