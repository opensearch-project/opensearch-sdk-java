/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.rest;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.RestHandler.ReplacedRoute;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;

/**
 * A subclass of {@link ReplacedRoute} that includes a handler method for that route.
 */
public class ReplacedNamedRouteHandler extends ReplacedRoute {

    private final String name;
    private final Set<String> actionNames;
    private final Function<RestRequest, ExtensionRestResponse> responseHandler;

    /**
     * Handle replaced routes using new and deprecated methods and new and deprecated paths.
     *
     * @param method route method
     * @param path new route path
     * @param deprecatedMethod deprecated method
     * @param deprecatedPath deprecated path
     * @param handler The method which handles the REST method and path.
     * @param name The name of the handler.
     * @param actionNames The list of action names to be registered for this handler.
     */
    public ReplacedNamedRouteHandler(
        Method method,
        String path,
        Method deprecatedMethod,
        String deprecatedPath,
        Function<RestRequest, ExtensionRestResponse> handler,
        String name,
        Set<String> actionNames
    ) {
        super(method, path, deprecatedMethod, deprecatedPath);
        this.responseHandler = handler;
        this.name = name;
        this.actionNames = actionNames == null ? Collections.emptySet() : actionNames;
    }

    /**
     * Handle replaced routes using route method, new and deprecated paths.
     * This constructor can be used when both new and deprecated paths use the same method.
     *
     * @param method route method
     * @param path new route path
     * @param deprecatedPath deprecated path
     * @param handler The method which handles the REST method and path.
     * @param name The name of the handler.
     * @param actionNames The list of action names to be registered for this handler.
     */
    public ReplacedNamedRouteHandler(
        Method method,
        String path,
        String deprecatedPath,
        Function<RestRequest, ExtensionRestResponse> handler,
        String name,
        Set<String> actionNames
    ) {
        this(method, path, method, deprecatedPath, handler, name, actionNames);
    }

    /**
     * Handle replaced routes using route, new and deprecated prefixes.
     *
     * @param route route
     * @param prefix new route prefix
     * @param deprecatedPrefix deprecated prefix
     * @param handler The method which handles the REST method and path.
     * @param name The name of the handler.
     * @param actionNames The list of action names to be registered for this handler.
     */
    public ReplacedNamedRouteHandler(
        Route route,
        String prefix,
        String deprecatedPrefix,
        Function<RestRequest, ExtensionRestResponse> handler,
        String name,
        Set<String> actionNames
    ) {
        this(route.getMethod(), prefix + route.getPath(), deprecatedPrefix + route.getPath(), handler, name, actionNames);
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
