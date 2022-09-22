/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.handlers;

import org.opensearch.extensions.OpenSearchRequest;
import org.opensearch.sdk.NamedWriteableRegistryAPI;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.transport.TransportResponse;

/**
 * This class handles the request from OpenSearch to a {@link ExtensionsRunner#startTransportService(TransportService transportService)} call.
 */

public class OpensearchRequestHandler {
    private NamedWriteableRegistryAPI namedWriteableRegistryApi = new NamedWriteableRegistryAPI();

    /**
     * Handles a request from OpenSearch and invokes the extension point API corresponding with the request type
     *
     * @param request  The request to handle.
     * @return A response to OpenSearch for the corresponding API
     * @throws Exception if the corresponding handler for the request is not present
     */
    public TransportResponse handleOpenSearchRequest(OpenSearchRequest request) throws Exception {
        // Read enum
        switch (request.getRequestType()) {
            case REQUEST_OPENSEARCH_NAMED_WRITEABLE_REGISTRY:
                return namedWriteableRegistryApi.handleNamedWriteableRegistryRequest(request);
            // Add additional request handlers here
            default:
                throw new IllegalArgumentException("Handler not present for the provided request");
        }
    }

}
