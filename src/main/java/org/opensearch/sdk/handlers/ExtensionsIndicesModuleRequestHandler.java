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
import org.opensearch.index.IndicesModuleRequest;
import org.opensearch.index.IndicesModuleResponse;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.transport.TransportService;

/**
 * This class handles the request from OpenSearch to a {@link ExtensionsRunner#startTransportService(TransportService transportService)} call.
 */

public class ExtensionsIndicesModuleRequestHandler {
    private static final Logger logger = LogManager.getLogger(ExtensionsIndicesModuleRequestHandler.class);

    /**
     * Handles a request for extension point indices from OpenSearch.  The {@link ExtensionsInitRequestHandler} class must have been called first to initialize the extension.
     *
     * @param indicesModuleRequest  The request to handle.
     * @param transportService  The transport service communicating with OpenSearch.
     * @return A response to OpenSearch with this extension's index and search listeners.
     */
    public IndicesModuleResponse handleIndicesModuleRequest(IndicesModuleRequest indicesModuleRequest, TransportService transportService) {
        logger.info("Registering Indices Module Request received from OpenSearch");
        IndicesModuleResponse indicesModuleResponse = new IndicesModuleResponse(true, true, true);
        return indicesModuleResponse;
    }

}
