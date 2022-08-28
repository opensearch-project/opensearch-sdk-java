/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import java.util.List;

import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest.Method;

/**
 * This interface defines methods which an extension REST handler (action) must provide.
 */
public interface ExtensionAction {

    /**
     * The list of {@link Route}s that this ExtensionAction is responsible for handling.
     */
    List<Route> routes();

    /**
     * Sends a response back to OpenSearch for a configured route on an extension.
     *
     * @param method A REST method.
     * @param uri The URI to handle
     * @return A response string to be sent to the end user via OpenSearch
     */
    String getExtensionResponse(Method method, String uri);
}
