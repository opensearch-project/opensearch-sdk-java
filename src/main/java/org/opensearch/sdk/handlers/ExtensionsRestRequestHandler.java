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
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.extensions.rest.RestExecuteOnExtensionResponse;
import org.opensearch.rest.RestStatus;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.ExtensionRestPathRegistry;
import org.opensearch.sdk.ExtensionRestResponse;

/**
 * This class handles the request from OpenSearch to a {@link ExtensionsRunner#startTransportService(TransportService transportService)} call.
 */

public class ExtensionsRestRequestHandler {
    private static final Logger logger = LogManager.getLogger(ExtensionsRestRequestHandler.class);
    private final ExtensionRestPathRegistry extensionRestPathRegistry;

    /**
     * Instantiate this class with an existing registry
     *
     * @param restPathRegistry The ExtensionsRunnerer's REST path registry
     */
    public ExtensionsRestRequestHandler(ExtensionRestPathRegistry restPathRegistry) {
        this.extensionRestPathRegistry = restPathRegistry;
    }

    /**
     * Handles a request from OpenSearch to execute a REST request on the extension.
     *
     * @param request  The REST request to execute.
     * @return A response acknowledging the request.
     */
    public RestExecuteOnExtensionResponse handleRestExecuteOnExtensionRequest(ExtensionRestRequest request) {

        ExtensionRestHandler restHandler = extensionRestPathRegistry.getHandler(request.method(), request.path());
        if (restHandler == null) {
            return new RestExecuteOnExtensionResponse(
                RestStatus.NOT_FOUND,
                "No handler for " + ExtensionRestPathRegistry.restPathToString(request.method(), request.path())
            );
        }

        // Get response from extension
        ExtensionRestResponse response = restHandler.handleRequest(request);
        logger.info("Sending extension response to OpenSearch: " + response.status());
        return new RestExecuteOnExtensionResponse(
            response.status(),
            response.contentType(),
            BytesReference.toBytes(response.content()),
            response.getHeaders()
        );
    }

}
