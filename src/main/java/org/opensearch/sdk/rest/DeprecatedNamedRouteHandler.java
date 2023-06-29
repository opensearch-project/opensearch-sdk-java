/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.rest;

import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.DeprecatedNamedRoute;
import org.opensearch.rest.RestHandler;
import org.opensearch.rest.RestRequest;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

/**
 * A subclass of {@link RestHandler.DeprecatedRoute} .
 */
public class DeprecatedNamedRouteHandler extends DeprecatedNamedRoute implements RouteHandlerWrapper {
    private final String name;
    private final Set<String> actionNames;
    private final Function<RestRequest, ExtensionRestResponse> responseHandler;

    /**
     * Handle deprecated routes using route method, deprecated path and deprecation message.
     *
     * @param method route method
     * @param path route path
     * @param deprecationMessage Message to be shown for this deprecated route
     * @param handler The method which handles the REST method and path.
     * @param name The name of this handler
     * @param actionNames The list of action names to be registered for this handler.
     */
    public DeprecatedNamedRouteHandler(
        RestRequest.Method method,
        String path,
        String deprecationMessage,
        Function<RestRequest, ExtensionRestResponse> handler,
        String name,
        Set<String> actionNames
    ) {
        super(method, path, deprecationMessage, name);
        this.name = name;
        this.actionNames = actionNames == null ? Collections.emptySet() : actionNames;
        this.responseHandler = handler;
    }

    /**
     * Executes the handler for this route.
     *
     * @param request The request to handle
     * @return the {@link ExtensionRestResponse} result from the handler for this route.
     */
    public ExtensionRestResponse handleRequest(RestRequest request) {
        return responseHandler.apply(request);
    }

    /**
     * The name of the RouteHandler. Must be unique across route handlers.
     * @return the name of this handler
     */
    public String name() {
        return this.name;
    }

    /**
     * The action names associate with the RouteHandler.
     * @return the set of action names registered for this route handler
     */
    public Set<String> actionNames() {
        return this.actionNames;
    }
}
