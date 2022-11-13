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

import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.RestHandler.Route;

/**
 * Provides convenience methods to reduce boilerplate code in an {@link ExtensionRestHandler} implementation.
 */
public abstract class BaseExtensionRestHandler implements ExtensionRestHandler {

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
        Optional<RouteHandler> handler = routeHandlers().stream()
            .filter(rh -> rh.getMethod().equals(request.method()))
            .filter(rh -> restPathMatches(request.path(), rh.getPath()))
            .findFirst();
        return handler.isPresent() ? handler.get().handleRequest(request) : unhandledRequest(request);
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
     * Returns a default response when a request does not match the handled methods or paths.
     * This can occur if a handler indicates routes that it handles but does not actually handle them.
     *
     * @param request The request that couldn't be handled.
     * @return an ExtensionRestResponse identifying the unhandled request.
     */
    protected ExtensionRestResponse unhandledRequest(ExtensionRestRequest request) {
        return new ExtensionRestResponse(request, NOT_FOUND, "Extension REST action improperly configured to handle " + request.toString());
    }
}
