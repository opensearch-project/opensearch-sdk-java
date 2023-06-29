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
import org.opensearch.rest.RestRequest;

import java.util.Set;

/**
 * Wrapper interface for named route handlers
 */
public interface RouteHandlerWrapper {

    /**
     * The name of the RouteHandler. Must be unique across route handlers.
     * @return the name of this handler
     */
    String name();

    /**
     * The action names associate with the RouteHandler.
     * @return the set of action names registered for this route handler
     */
    Set<String> actionNames();

    /**
     * Executes the handler for this route.
     *
     * @param request The request to handle
     * @return the {@link ExtensionRestResponse} result from the handler for this route.
     */
    ExtensionRestResponse handleRequest(RestRequest request);
}
