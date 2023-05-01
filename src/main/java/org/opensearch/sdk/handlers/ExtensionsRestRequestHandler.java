/*
 * Copyright OpenSearch Contributors
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
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.extensions.rest.RestExecuteOnExtensionResponse;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.SDKNamedXContentRegistry;
import org.opensearch.sdk.rest.ExtensionRestHandler;
import org.opensearch.sdk.rest.ExtensionRestPathRegistry;
import org.opensearch.sdk.rest.SDKHttpRequest;
import org.opensearch.sdk.rest.SDKRestRequest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptyList;
import static org.opensearch.rest.BytesRestResponse.TEXT_CONTENT_TYPE;
import static org.opensearch.rest.RestStatus.NOT_FOUND;

/**
 * This class handles the request from OpenSearch to a {@link ExtensionsRunner#startTransportService(TransportService transportService)} call.
 */

public class ExtensionsRestRequestHandler {
    private static final Logger logger = LogManager.getLogger(ExtensionsRestRequestHandler.class);
    private final ExtensionRestPathRegistry extensionRestPathRegistry;
    private final SDKNamedXContentRegistry sdkNamedXContentRegistry;

    /**
     * Instantiate this class with an existing registry
     *
     * @param restPathRegistry The ExtensionsRunnerer's REST path registry
     * @param sdkNamedXContentRegistry The SDKNamedXContentRegistry wrapper
     */
    public ExtensionsRestRequestHandler(ExtensionRestPathRegistry restPathRegistry, SDKNamedXContentRegistry sdkNamedXContentRegistry) {
        this.sdkNamedXContentRegistry = sdkNamedXContentRegistry;
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
                NOT_FOUND,
                TEXT_CONTENT_TYPE,
                String.join(" ", "No handler for", request.method().name(), request.path()).getBytes(UTF_8),
                emptyMap(),
                emptyList(),
                false
            );
        }

        SDKRestRequest sdkRestRequest = new SDKRestRequest(
            sdkNamedXContentRegistry.getRegistry(),
            request.params(),
            request.path(),
            request.headers(),
            new SDKHttpRequest(request),
            null
        );

        // Get response from extension
        ExtensionRestResponse response = restHandler.handleRequest(sdkRestRequest);
        logger.info("Sending extension response to OpenSearch: " + response.status());
        return new RestExecuteOnExtensionResponse(
            response.status(),
            response.contentType(),
            BytesReference.toBytes(response.content()),
            response.getHeaders(),
            response.getConsumedParams(),
            response.isContentConsumed()
        );
    }

}
