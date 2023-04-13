/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opensearch.rest.BaseRestHandler;
import static org.opensearch.rest.RestStatus.INTERNAL_SERVER_ERROR;
import static org.opensearch.rest.RestStatus.NOT_FOUND;

import java.io.IOException;

import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestStatus;

/**
 * Provides convenience methods to reduce boilerplate code in an {@link ExtensionRestHandler} implementation.
 */
public abstract class BaseExtensionRestHandler implements ExtensionRestHandler {

    /**
     * Defines a list of methods which handle each rest {@link Route}. Override this in a subclass to use the functional
     * syntax.
     *
     * @return a list of {@link RouteHandler} with corresponding methods to each route.
     */
    protected List<RouteHandler> routeHandlers() {
        return Collections.emptyList();
    }

    @Override
    public List<Route> routes() {
        return List.copyOf(routeHandlers());
    }

    @Override
    public ExtensionRestResponse handleRequest(RestRequest request) {
        Optional<RouteHandler> handler = routeHandlers().stream()
            .filter(rh -> rh.getMethod().equals(request.method()))
            .filter(rh -> restPathMatches(request.path(), rh.getPath()))
            .findFirst();
        return handler.isPresent() ? handler.get().handleRequest(request) : unhandledRequest(request);
    }

    /**
     * Determines if the request's path is a match for the configured handler path.
     *
     * @param requestPath The path from the {@link RestRequest}
     * @param handlerPath The path from the {@link RouteHandler}
     * @return true if the request path matches the route
     */
    private boolean restPathMatches(String requestPath, String handlerPath) {
        // Check exact match
        if (handlerPath.equals(requestPath)) {
            return true;
        }
        // Split path to evaluate named params
        String[] handlerSplit = handlerPath.split("/");
        String[] requestSplit = requestPath.split("/");
        if (handlerSplit.length != requestSplit.length) {
            return false;
        }
        for (int i = 0; i < handlerSplit.length; i++) {
            if (!(handlerSplit[i].equals(requestSplit[i]) || (handlerSplit[i].startsWith("{") && handlerSplit[i].endsWith("}")))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a default response when a request does not match the handled methods or paths. This can occur if a
     * handler indicates routes that it handles but does not actually handle them.
     *
     * @param request The request that couldn't be handled.
     * @return an ExtensionRestResponse identifying the unhandled request.
     */
    protected ExtensionRestResponse unhandledRequest(RestRequest request) {
        return createJsonResponse(
            request,
            NOT_FOUND,
            "error",
            "Extension REST action improperly configured to handle: [" + request.method() + " " + request.uri() + "]"
        );
    }

    /**
     * Returns a default response when a request handler throws an exception.
     *
     * @param request The request that caused the exception
     * @param e The exception
     * @return an ExtensionRestResponse identifying the exception
     */
    protected ExtensionRestResponse exceptionalRequest(RestRequest request, Exception e) {
        return createJsonResponse(request, INTERNAL_SERVER_ERROR, "error", "Request failed with exception: [" + e.getMessage() + "]");
    }

    /**
     * Returns a JSON-formatted response for a given string.
     *
     * @param request The request to respond to
     * @param status The response status to send
     * @param fieldName The field name for the response string
     * @param responseStr The string to include
     * @return an ExtensionRestResponse in JSON format including the specified string as the content of the specified field
     */
    protected ExtensionRestResponse createJsonResponse(RestRequest request, RestStatus status, String fieldName, String responseStr) {
        try {
            return new ExtensionRestResponse(
                request,
                status,
                JsonXContent.contentBuilder().startObject().field(fieldName, responseStr).endObject()
            );
        } catch (IOException e) {
            // This Should Never Happen (TM)
            // If we messed up JSON code above, just send plain text
            return new ExtensionRestResponse(request, status, fieldName + ": " + responseStr);
        }
    }

    /**
     * Returns a String message of the detail of any unrecognized error occurred. The string is intended for use in error messages to be returned to the user.
     *
     * @param request The request that caused the exception
     * @param invalids Strings from the request which were unable to be understood.
     * @param candidates A set of words that are most likely to be the valid strings determined invalid, to be suggested to the user.
     * @param detail The parameter contains the details of the exception.
     * @return a String that contains the message.
     */
    protected final String unrecognized(
        final RestRequest request,
        final Set<String> invalids,
        final Set<String> candidates,
        final String detail
    ) {
        return BaseRestHandler.unrecognizedStrings(request, invalids, candidates, detail);
    }
}
