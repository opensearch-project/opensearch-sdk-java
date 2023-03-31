/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.util.function.Function;

import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;

/**
 * A subclass of {@link Route} that includes a handler method for that route.
 */
public class RouteHandler extends Route {

    private final Function<RestRequest, ExtensionRestResponse> responseHandler;

    /**
     * Handle the method and path with the specified handler.
     *
     * @param method The {@link Method} to handle.
     * @param path The path to handle.
     * @param handler The method which handles the method and path.
     */
    public RouteHandler(Method method, String path, Function<RestRequest, ExtensionRestResponse> handler) {
        super(method, path);
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
}
