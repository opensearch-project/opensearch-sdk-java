/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import java.util.List;

import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestHandler;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.rest.RestResponse;

/**
 * This interface defines methods which an extension REST handler (action) must provide.
 * It is the Extension counterpart to core OpenSearch {@link RestHandler}.
 */
public interface ExtensionRestHandler {

    /**
     * The list of {@link Route}s that this ExtensionRestHandler is responsible for handling.
     */
    List<Route> routes();

    /**
     * Handles REST Requests forwarded from OpenSearch for a configured route on an extension.
     * Parameters are components of the {@link RestRequest} received from a user.
     * This method corresponds to the {@link BaseRestHandler#prepareRequest} method.
     *
     * @param method A REST method.
     * @param uri The URI to handle.
     * @return A {@link RestResponse} to the request.
     */
    RestResponse handleRequest(Method method, String uri);
}
