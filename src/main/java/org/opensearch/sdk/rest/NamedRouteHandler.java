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
import org.opensearch.rest.NamedRoute;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;

import java.util.Set;
import java.util.function.Function;

/**
 * A subclass of {@link Route} that includes a handler method for that route.
 */
public class NamedRouteHandler extends NamedRoute {
    private final Function<RestRequest, ExtensionRestResponse> responseHandler;

    /**
     * Handle the method and path with the specified handler.
     *
     * @param method The {@link Method} to handle.
     * @param path The path to handle.
     * @param handler The method which handles the REST method and path.
     * @param name The name of the handler.
     * @param actionNames The list of action names to be registered for this handler.
     */
    public NamedRouteHandler(
        Method method,
        String path,
        Function<RestRequest, ExtensionRestResponse> handler,
        String name,
        Set<String> actionNames
    ) {
        super(new Builder().method(method).path(path).uniqueName(name).legacyActionNames(actionNames));
        this.responseHandler = handler;
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Expected route handler to have a unique name, found none.");
        }
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
}
