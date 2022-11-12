/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import static org.opensearch.rest.RestStatus.NOT_FOUND;

import java.util.List;
import java.util.Optional;

import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestStatus;

/**
 * Provides convenience methods to reduce boilerplate code in an {@link ExtensionRestHandler} implementation.
 */
public abstract class BaseExtensionRestHandler implements ExtensionRestHandler {

    private ExtensionRestRequest request;

    /**
     * Defines a list of methods which handle each rest {@link Route}.
     *
     * @return a list of {@link RouteHandler} with corresponding methods to each route.
     */
    protected abstract List<RouteHandler> routeHandlers();

    @Override
    public List<Route> routes() {
        return List.copyOf(routeHandlers());
    }

    @Override
    public ExtensionRestResponse handleRequest(ExtensionRestRequest request) {
        this.request = request;
        Optional<RouteHandler> handler = routeHandlers().stream()
            .filter(rh -> rh.getMethod().equals(request.method()))
            .filter(rh -> restPathMatches(request.path(), rh.getPath()))
            .findFirst();
        return handler.isPresent() ? handler.get().getExtensionRestResponse() : unhandledRequest();
    }

    /**
     * Determines if the request's path is a match for the configured handler path.
     *
     * @param requestPath The path from the {@link ExtensionRestRequest}
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
     * @return an ExtensionRestResponse identifying the unhandled request.
     */
    protected ExtensionRestResponse unhandledRequest() {
        return createResponse(NOT_FOUND, "Extension REST action improperly configured to handle " + getRequest().toString());
    }

    /**
     * Creates an {@link ExtensionRestResponse} with the given status and Xcontent.
     *
     * @param status The {@link RestStatus} for the response.
     * @param builder An {@link XContentBuilder} for the response.
     * @return the response.
     */
    protected ExtensionRestResponse createResponse(RestStatus status, XContentBuilder builder) {
        return new ExtensionRestResponse(getRequest(), status, builder);
    }

    /**
     * Creates an {@link ExtensionRestResponse} with the given status and text content.
     *
     * @param status The {@link RestStatus} for the response.
     * @param content The content.
     * @return the response.
     */
    protected ExtensionRestResponse createResponse(RestStatus status, String content) {
        return new ExtensionRestResponse(getRequest(), status, content);
    }

    /**
     * Creates an {@link ExtensionRestResponse} with the given status and content.
     *
     * @param status The {@link RestStatus} for the response.
     * @param contentType The type of the content.
     * @param content The content.
     * @return the response.
     */
    protected ExtensionRestResponse createResponse(RestStatus status, String contentType, String content) {
        return new ExtensionRestResponse(getRequest(), status, contentType, content);
    }

    /**
     * Creates an {@link ExtensionRestResponse} with the given status and binary content.
     *
     * @param status The {@link RestStatus} for the response.
     * @param contentType The type of the content.
     * @param content The content.
     * @return the response.
     */
    protected ExtensionRestResponse createResponse(RestStatus status, String contentType, byte[] content) {
        return new ExtensionRestResponse(getRequest(), status, contentType, content);
    }

    /**
     * Creates an {@link ExtensionRestResponse} with the given status and binary content.
     *
     * @param status The {@link RestStatus} for the response.
     * @param contentType The type of the content.
     * @param content The content.
     * @return the response.
     */
    protected ExtensionRestResponse createResponse(RestStatus status, String contentType, BytesReference content) {
        return new ExtensionRestResponse(getRequest(), status, contentType, content);
    }

    public ExtensionRestRequest getRequest() {
        return request;
    }
}
