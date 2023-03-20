/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.handlers;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.ActionType;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.ExtensionActionRequest;
import org.opensearch.extensions.action.ExtensionActionResponse;
import org.opensearch.sdk.SDKClient;
import org.opensearch.sdk.SDKTransportService;
import org.opensearch.sdk.action.SDKActionModule;

/**
 * This class handles a request from OpenSearch from another extension's {@link SDKTransportService#sendProxyActionRequest()} call.
 */
public class ExtensionsActionRequestHandler {
    private static final Logger logger = LogManager.getLogger(ExtensionsActionRequestHandler.class);

    final SDKActionModule sdkActionModule;
    final SDKClient client;

    /**
     * Instantiate this handler
     *
     * @param sdkClient An initialized SDKClient with the registered actions
     * @param sdkActionModule An initialized SDKActionModule with the registered actions
     */
    public ExtensionsActionRequestHandler(SDKClient sdkClient, SDKActionModule sdkActionModule) {
        this.sdkActionModule = sdkActionModule;
        this.client = sdkClient;
    }

    /**
     * Handles a request from OpenSearch to execute a TransportAction on the extension.
     *
     * @param request The request to execute
     * @return The response from the TransportAction
     */
    public ExtensionActionResponse handleExtensionActionRequest(ExtensionActionRequest request) {
        logger.info("Received request to execute action [" + request.getAction() + "]");
        final ExtensionActionResponse response = new ExtensionActionResponse(false, new byte[0]);

        // Find matching ActionType instance
        Optional<? extends ActionType<? extends ActionResponse>> optionalAction = sdkActionModule.getActions()
            .values()
            .stream()
            .map(h -> h.getAction())
            .filter(a -> a.getClass().getName().equals(request.getAction()))
            .findAny();
        if (optionalAction.isEmpty()) {
            response.setResponseBytesAsString("No action [" + request.getAction() + "] is registered.");
            return response;
        }

        // Execute the action
        logger.info("Executing action [" + request.getAction() + "]");
        ActionType<? extends ActionResponse> action = optionalAction.get();
        // TODO: We need async client.execute to hide these action listener details and return the future directly
        // https://github.com/opensearch-project/opensearch-sdk-java/issues/584
        CompletableFuture<ExtensionActionResponse> futureResponse = new CompletableFuture<>();
        client.execute(action, request, ActionListener.wrap(r -> {
            byte[] bytes = new byte[0];
            try (BytesStreamOutput out = new BytesStreamOutput()) {
                ((ActionResponse) r).writeTo(out);
                bytes = BytesReference.toBytes(out.bytes());
            } catch (IOException e) {
                // This Should Never Happen (TM)
                // Won't get an IOException locally
            }
            response.setSuccess(true);
            response.setResponseBytes(bytes);
            futureResponse.complete(response);
        }, e -> futureResponse.completeExceptionally(e)));

        logger.info("Waiting for response to action [" + request.getAction() + "]");
        try {
            ExtensionActionResponse actionResponse = futureResponse.orTimeout(
                ExtensionsManager.EXTENSION_REQUEST_WAIT_TIMEOUT,
                TimeUnit.SECONDS
            ).get();
            response.setSuccess(true);
            response.setResponseBytes(actionResponse.getResponseBytes());
            logger.info("Response successful to [" + request.getAction() + "]");
        } catch (Exception e) {
            response.setResponseBytesAsString("Action failed: " + e.getMessage());
            logger.info("Response failed to [" + request.getAction() + "]");
        }
        logger.info("Sending action response to OpenSearch: " + response.getResponseBytesAsString());
        return response;
    }

}
