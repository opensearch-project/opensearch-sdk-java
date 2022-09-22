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
import org.opensearch.extensions.rest.RestExecuteOnExtensionRequest;
import org.opensearch.extensions.rest.RestExecuteOnExtensionResponse;
import org.opensearch.rest.RestResponse;
import org.opensearch.rest.RestStatus;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.ExtensionRestPathRegistry;
import org.opensearch.sdk.ExtensionRestRequest;

/**
 * This class handles the request from OpenSearch to a {@link ExtensionsRunner#startTransportService(TransportService transportService)} call.
 */

public class ExtensionsRestRequestHandler {
    private static final Logger logger = LogManager.getLogger(ExtensionsRestRequestHandler.class);
    private ExtensionRestPathRegistry extensionRestPathRegistry = new ExtensionRestPathRegistry();

    /**
     * Handles a request from OpenSearch to execute a REST request on the extension.
     *
     * @param request  The REST request to execute.
     * @return A response acknowledging the request.
     */
    public RestExecuteOnExtensionResponse handleRestExecuteOnExtensionRequest(RestExecuteOnExtensionRequest request) {

        ExtensionRestHandler restHandler = extensionRestPathRegistry.getHandler(request.getMethod(), request.getUri());
        if (restHandler == null) {
            return new RestExecuteOnExtensionResponse(
                RestStatus.NOT_FOUND,
                "No handler for " + ExtensionRestPathRegistry.restPathToString(request.getMethod(), request.getUri())
            );
        }
        // ExtensionRestRequest restRequest = new ExtensionRestRequest(request);
        ExtensionRestRequest restRequest = new ExtensionRestRequest(
            request.getMethod(),
            request.getUri(),
            request.getRequestIssuerIdentity()
        );

        // Get response from extension
        RestResponse response = restHandler.handleRequest(restRequest);
        logger.info("Sending extension response to OpenSearch: " + response.status());
        return new RestExecuteOnExtensionResponse(
            response.status(),
            response.contentType(),
            BytesReference.toBytes(response.content()),
            response.getHeaders()
        );
    }

}
