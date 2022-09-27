/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.extensions.ExtensionBooleanResponse;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.index.IndicesModuleRequest;
import org.opensearch.discovery.InitializeExtensionsRequest;

/**
 * This class handles the request from OpenSearch to a {@link ExtensionsRunner#startTransportService(TransportService transportService)} call.
 */

public class ExtensionsIndicesModuleNameRequestHandler {
    private static final Logger logger = LogManager.getLogger(ExtensionsIndicesModuleNameRequestHandler.class);

    /**
     * Handles a request for extension name from OpenSearch.  The {@link #handleExtensionInitRequest(InitializeExtensionsRequest)} method must have been called first to initialize the extension.
     *
     * @param indicesModuleRequest  The request to handle.
     * @return A response acknowledging the request.
     */
    public ExtensionBooleanResponse handleIndicesModuleNameRequest(IndicesModuleRequest indicesModuleRequest) {
        // Works as beforeIndexRemoved
        logger.info("Registering Indices Module Name Request received from OpenSearch");
        ExtensionBooleanResponse indicesModuleNameResponse = new ExtensionBooleanResponse(true);
        return indicesModuleNameResponse;
    }

}
