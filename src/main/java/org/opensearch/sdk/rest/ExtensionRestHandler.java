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
import java.util.List;

import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestHandler;
import org.opensearch.rest.RestHandler.DeprecatedRoute;
import org.opensearch.rest.RestHandler.ReplacedRoute;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest;

/**
 * This interface defines methods which an extension REST handler (action) must provide.
 * It is the Extension counterpart to core OpenSearch {@link RestHandler}.
 */
@FunctionalInterface
public interface ExtensionRestHandler {

    /**
     * Handles REST Requests forwarded from OpenSearch for a configured route on an extension.
     * Parameter contains components of the {@link RestRequest} received from a user.
     * This method corresponds to the {@link BaseRestHandler#prepareRequest} method.
     *
     * @param restRequest a REST request object for a request to be forwarded to extensions
     * @return An {@link ExtensionRestResponse} to the request.
     */
    ExtensionRestResponse handleRequest(RestRequest restRequest);

    /**
     * A list of {@link Route}s that this ExtensionRestHandler is responsible for handling.
     *
     * @return The routes this handler will handle.
     */
    default List<Route> routes() {
        return Collections.emptyList();
    }

    /**
     * A list of routes handled by this RestHandler that are deprecated and do not have a direct replacement.
     * If changing the {@code path} or {@code method} of a route, use {@link #replacedRoutes()}.
     *
     * @return Deprecated routes this handler will handle.
     */
    default List<DeprecatedRoute> deprecatedRoutes() {
        return Collections.emptyList();
    }

    /**
     * A list of routes handled by this RestHandler that have had their {@code path} and/or {@code method} changed.
     * The pre-existing {@code route} will be registered as deprecated alongside the updated {@code route}.
     *
     * @return Replaced routes this handler will handle.
     */
    default List<ReplacedRoute> replacedRoutes() {
        return Collections.emptyList();
    }
}
