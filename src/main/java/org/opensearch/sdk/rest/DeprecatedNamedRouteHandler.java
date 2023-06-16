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
import org.opensearch.rest.RestHandler;
import org.opensearch.rest.RestRequest;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

/**
 * A subclass of {@link RestHandler.DeprecatedRoute} .
 */
public class DeprecatedNamedRouteHandler extends RestHandler.DeprecatedRoute {
    private final String name;
    private final Set<String> actionNames;
    private final Function<RestRequest, ExtensionRestResponse> responseHandler;

    public DeprecatedNamedRouteHandler(
        RestRequest.Method method,
        String path,
        String name,
        Set<String> actionNames,
        String deprecationMessage,
        Function<RestRequest, ExtensionRestResponse> handler
    ) {
        super(method, path, deprecationMessage);
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
