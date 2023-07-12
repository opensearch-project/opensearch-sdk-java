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
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.NamedRoute;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestResponse;
import org.opensearch.sdk.rest.BaseExtensionRestHandler;
import org.opensearch.sdk.rest.ExtensionRestHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import static org.opensearch.rest.RestRequest.Method.DELETE;
import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestRequest.Method.POST;
import static org.opensearch.rest.RestRequest.Method.PUT;
import static org.opensearch.core.rest.RestStatus.BAD_REQUEST;
import static org.opensearch.core.rest.RestStatus.NOT_ACCEPTABLE;
import static org.opensearch.core.rest.RestStatus.OK;

/**
 * Sample REST Handler (REST Action).
 * Extension REST handlers must implement {@link ExtensionRestHandler}.
 * Extending {@link BaseExtensionRestHandler} provides many convenience methods.
 */
public class RestHelloAction extends BaseExtensionRestHandler {

    private static final String TEXT_CONTENT_TYPE = "text/plain; charset=UTF-8";
    private static final String GREETING = "Hello, %s!";
    private static final String DEFAULT_NAME = "World";

    private String worldName = DEFAULT_NAME;
    private List<String> worldAdjectives = new ArrayList<>();
    private Random rand = new Random();

    /**
     * Instantiate this action
     */
    public RestHelloAction() {}

    @Override
    public List<NamedRoute> routes() {
        return List.of(
            new NamedRoute.Builder().method(GET)
                .path("/hello")
                .handler(handleGetRequest)
                .uniqueName(addRouteNamePrefix("greet"))
                .legacyActionNames(Set.of("cluster:admin/opensearch/hw/greet"))
                .build(),
            new NamedRoute.Builder().method(POST)
                .path("/hello")
                .handler(handlePostRequest)
                .uniqueName(addRouteNamePrefix("greet_with_adjective"))
                .legacyActionNames(Collections.emptySet())
                .build(),
            new NamedRoute.Builder().method(PUT)
                .path("/hello/{name}")
                .handler(handlePutRequest)
                .uniqueName(addRouteNamePrefix("greet_with_name"))
                .legacyActionNames(Set.of("cluster:admin/opensearch/hw/greet_with_name"))
                .build(),
            new NamedRoute.Builder().method(DELETE)
                .path("/goodbye")
                .handler(handleDeleteRequest)
                .uniqueName(addRouteNamePrefix("goodbye"))
                .legacyActionNames(Collections.emptySet())
                .build()
        );
    }

    private Function<RestRequest, RestResponse> handleGetRequest = (request) -> {
        String worldNameWithRandomAdjective = worldAdjectives.isEmpty()
            ? worldName
            : String.join(" ", worldAdjectives.get(rand.nextInt(worldAdjectives.size())), worldName);
        return new ExtensionRestResponse(request, OK, String.format(GREETING, worldNameWithRandomAdjective));
    };

    private Function<RestRequest, RestResponse> handlePostRequest = (request) -> {
        if (request.hasContent()) {
            String adjective = "";
            XContentType contentType = request.getXContentType();
            if (contentType == null) {
                // Plain text
                adjective = request.content().utf8ToString();
            } else if (contentType.equals(XContentType.JSON)) {
                try {
                    adjective = request.contentParser().mapStrings().get("adjective");
                } catch (IOException | OpenSearchParseException e) {
                    // Sample plain text response
                    return new ExtensionRestResponse(request, BAD_REQUEST, "Unable to parse adjective from JSON");
                }
            } else {
                // Sample text response with content type
                return new ExtensionRestResponse(
                    request,
                    NOT_ACCEPTABLE,
                    TEXT_CONTENT_TYPE,
                    "Only text and JSON content types are supported"
                );
            }
            if (adjective != null && !adjective.isBlank()) {
                worldAdjectives.add(adjective.trim());
                // Sample JSON response with a builder
                try {
                    XContentBuilder builder = JsonXContent.contentBuilder()
                        .startObject()
                        .field("worldAdjectives", worldAdjectives)
                        .endObject();
                    return new ExtensionRestResponse(request, OK, builder);
                } catch (IOException e) {
                    // Sample response for developer error
                    return unhandledRequest(request);
                }
            }
            byte[] noAdjective = "No adjective included with POST request".getBytes(StandardCharsets.UTF_8);
            // Sample binary response with content type
            return new ExtensionRestResponse(request, BAD_REQUEST, TEXT_CONTENT_TYPE, noAdjective);
        }
        // Sample bytes reference response with content type
        BytesReference noContent = BytesReference.fromByteBuffer(
            ByteBuffer.wrap("No content included with POST request".getBytes(StandardCharsets.UTF_8))
        );
        return new ExtensionRestResponse(request, BAD_REQUEST, TEXT_CONTENT_TYPE, noContent);
    };

    private Function<RestRequest, RestResponse> handlePutRequest = (request) -> {
        String name = request.param("name");
        try {
            worldName = URLDecoder.decode(name, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return new ExtensionRestResponse(request, BAD_REQUEST, e.getMessage());
        }
        return new ExtensionRestResponse(request, OK, "Updated the world's name to " + worldName);
    };

    private Function<RestRequest, RestResponse> handleDeleteRequest = (request) -> {
        this.worldName = DEFAULT_NAME;
        this.worldAdjectives.clear();
        return new ExtensionRestResponse(request, OK, "Goodbye, cruel world! Restored default values.");
    };
}
