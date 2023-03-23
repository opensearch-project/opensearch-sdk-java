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
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.RemoteExtensionActionResponse;
import org.opensearch.sdk.SDKTransportService;
import org.opensearch.common.Nullable;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the response from OpenSearch to a {@link SDKTransportService#sendProxyActionRequest()} call.
 */
public class ExtensionActionResponseHandler implements TransportResponseHandler<RemoteExtensionActionResponse> {

    private static final Logger logger = LogManager.getLogger(ExtensionActionResponseHandler.class);
    private final CompletableFuture<RemoteExtensionActionResponse> inProgressFuture;
    private byte[] responseBytes = null;

    /**
    * Instantiates a new ExtensionActionResponseHandler
    */
    public ExtensionActionResponseHandler() {
        this.inProgressFuture = new CompletableFuture<>();
    }

    @Override
    public void handleResponse(RemoteExtensionActionResponse response) {
        logger.info("Received response bytes: " + response.getResponseBytes().length + " bytes");

        logger.debug("Received response bytes: " + response.getResponseBytesAsString());
        // Set ExtensionActionResponse from response
        this.responseBytes = response.getResponseBytes();
        inProgressFuture.complete(response);
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("ExtensionActionResponseRequest failed", exp);
        inProgressFuture.completeExceptionally(exp);
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public RemoteExtensionActionResponse read(StreamInput in) throws IOException {
        return new RemoteExtensionActionResponse(in);
    }

    /**
     * Waits for the ExtensionActionResponseHandler future to complete
     * @throws Exception
     *        if the response times out
     */
    public void awaitResponse() throws Exception {
        inProgressFuture.orTimeout(ExtensionsManager.EXTENSION_REQUEST_WAIT_TIMEOUT, TimeUnit.SECONDS).get();
    }

    @Nullable
    public byte[] getResponseBytes() {
        return this.responseBytes;
    }
}
