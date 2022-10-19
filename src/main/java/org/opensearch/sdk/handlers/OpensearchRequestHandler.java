/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.handlers;

import org.opensearch.extensions.OpenSearchRequest;
import org.opensearch.sdk.ExtensionNamedWriteableRegistry;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.transport.TransportResponse;

/**
 * This class handles the request from OpenSearch to a {@link ExtensionsRunner#startTransportService(TransportService transportService)} call.
 */

public class OpensearchRequestHandler {
    private final ExtensionNamedWriteableRegistry extensionNamedWriteableRegistry;

    /**
     * Instantiate this object with a namedWriteableRegistry
     *
     * @param namedWriteableRegistry The registry passed from ExtensionsRunner
     */
    public OpensearchRequestHandler(ExtensionNamedWriteableRegistry namedWriteableRegistry) {
        this.extensionNamedWriteableRegistry = namedWriteableRegistry;
    }

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
                return extensionNamedWriteableRegistry.handleNamedWriteableRegistryRequest(request);
            // Add additional request handlers here
            default:
                throw new IllegalArgumentException("Handler not present for the provided request");
        }
    }

}
