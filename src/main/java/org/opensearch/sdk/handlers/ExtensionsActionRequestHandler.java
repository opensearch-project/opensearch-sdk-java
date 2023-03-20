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
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.ActionType;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.io.stream.StreamInput;
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
        logger.debug("Received request to execute action [" + request.getAction() + "]");
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

        ActionType<? extends ActionResponse> action = optionalAction.get();
        logger.debug("Found matching action [" + action.name() + "], an instance of [" + action.getClass().getName() + "]");

        // Extract request class name from bytes and instantiate request
        int nullPos = indexOf(request.getRequestBytes(), (byte) 0);
        String requestClassName = new String(Arrays.copyOfRange(request.getRequestBytes(), 0, nullPos), StandardCharsets.UTF_8);
        ActionRequest actionRequest = null;
        try {
            Class<?> clazz = Class.forName(requestClassName);
            Constructor<?> constructor = clazz.getConstructor(StreamInput.class);
            StreamInput requestByteStream = StreamInput.wrap(
                Arrays.copyOfRange(request.getRequestBytes(), nullPos + 1, request.getRequestBytes().length - nullPos)
            );
            actionRequest = (ActionRequest) constructor.newInstance(requestByteStream);
        } catch (Exception e) {
            response.setResponseBytesAsString("No request class [" + requestClassName + "] is available.");
            return response;
        }

        // Execute the action
        // TODO: We need async client.execute to hide these action listener details and return the future directly
        // https://github.com/opensearch-project/opensearch-sdk-java/issues/584
        CompletableFuture<ExtensionActionResponse> futureResponse = new CompletableFuture<>();
        client.execute(action, actionRequest, ActionListener.wrap(r -> {
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

        logger.debug("Waiting for response to action [" + request.getAction() + "]");
        try {
            ExtensionActionResponse actionResponse = futureResponse.orTimeout(
                ExtensionsManager.EXTENSION_REQUEST_WAIT_TIMEOUT,
                TimeUnit.SECONDS
            ).get();
            response.setSuccess(true);
            response.setResponseBytes(actionResponse.getResponseBytes());
            logger.debug("Response successful to [" + request.getAction() + "]");
        } catch (Exception e) {
            response.setResponseBytesAsString("Action failed: " + e.getMessage());
            logger.debug("Response failed to [" + request.getAction() + "]");
        }
        logger.debug("Sending action response to OpenSearch: " + response.getResponseBytesAsString());
        return response;
    }

    private static int indexOf(byte[] bytes, byte value) {
        for (int offset = 0; offset < bytes.length; ++offset) {
            if (bytes[offset] == value) {
                return offset;
            }
        }
        return -1;
    }
}
