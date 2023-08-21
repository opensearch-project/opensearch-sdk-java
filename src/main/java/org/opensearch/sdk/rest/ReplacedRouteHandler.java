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
import org.opensearch.rest.RestHandler.ReplacedRoute;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.rest.RestResponse;

import java.util.function.Function;

/**
 * A subclass of {@link ReplacedRoute} that includes a handler method for that route.
 */
public class ReplacedRouteHandler extends ReplacedRoute {

    private final Function<RestRequest, RestResponse> responseHandler;

    /**
     * Handle replaced routes using new and deprecated methods and new and deprecated paths.
     *
     * @param method route method
     * @param path new route path
     * @param deprecatedMethod deprecated method
     * @param deprecatedPath deprecated path
     * @param handler The method which handles the REST method and path.
     */
    public ReplacedRouteHandler(
        Method method,
        String path,
        Method deprecatedMethod,
        String deprecatedPath,
        Function<RestRequest, RestResponse> handler
    ) {
        super(method, path, deprecatedMethod, deprecatedPath);
        this.responseHandler = handler;
    }

    /**
     * Handle replaced routes using route method, new and deprecated paths.
     * This constructor can be used when both new and deprecated paths use the same method.
     *
     * @param method route method
     * @param path new route path
     * @param deprecatedPath deprecated path
     * @param handler The method which handles the REST method and path.
     */
    public ReplacedRouteHandler(Method method, String path, String deprecatedPath, Function<RestRequest, RestResponse> handler) {
        this(method, path, method, deprecatedPath, handler);
    }

    /**
     * Handle replaced routes using route, new and deprecated prefixes.
     *
     * @param route route
     * @param prefix new route prefix
     * @param deprecatedPrefix deprecated prefix
     * @param handler The method which handles the REST method and path.
     */
    public ReplacedRouteHandler(Route route, String prefix, String deprecatedPrefix, Function<RestRequest, RestResponse> handler) {
        this(route.getMethod(), prefix + route.getPath(), deprecatedPrefix + route.getPath(), handler);
    }

    /**
     * Executes the handler for this route.
     *
     * @param request The request to handle
     * @return the {@link ExtensionRestResponse} result from the handler for this route.
     */
    public ExtensionRestResponse handleRequest(RestRequest request) {
        return (ExtensionRestResponse) responseHandler.apply(request);
    }
}
