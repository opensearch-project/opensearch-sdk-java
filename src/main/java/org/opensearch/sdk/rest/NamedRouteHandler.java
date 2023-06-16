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
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

/**
 * A subclass of {@link Route} that includes a handler method for that route.
 */
public class NamedRouteHandler extends Route {

    private final String name;

    private final Set<String> actionNames;

    private final Function<RestRequest, ExtensionRestResponse> responseHandler;

    /**
     * Handle the method and path with the specified handler.
     *
     * @param name The name of the handler.
     * @param actionNames The list of action names to be registered for thishandler.
     * @param method The {@link Method} to handle.
     * @param path The path to handle.
     * @param handler The method which handles the method and path.
     */
    public NamedRouteHandler(
        Method method,
        String path,
        String name,
        Set<String> actionNames,
        Function<RestRequest, ExtensionRestResponse> handler
    ) {
        super(method, path);
        this.responseHandler = handler;
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Expected route handler to have a unique name, found none.");
        }
        this.name = name;
        this.actionNames = actionNames == null ? Collections.emptySet() : actionNames;
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
     */
    public String name() {
        return this.name;
    }

    /**
     * The action names associate with the RouteHandler.
     */
    public Set<String> actionNames() {
        return this.actionNames;
    }

}
