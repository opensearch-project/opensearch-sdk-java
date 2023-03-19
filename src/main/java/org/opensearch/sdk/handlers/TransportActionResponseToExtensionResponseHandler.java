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
import org.opensearch.extensions.action.TransportActionResponseToExtension;
import org.opensearch.common.Nullable;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.sdk.action.SDKActionModule;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the response from OpenSearch to a {@link SDKActionModule#sendProxyActionRequest()} call.
 */
public class TransportActionResponseToExtensionResponseHandler implements TransportResponseHandler<TransportActionResponseToExtension> {

    private static final Logger logger = LogManager.getLogger(TransportActionResponseToExtensionResponseHandler.class);
    private final CompletableFuture<TransportActionResponseToExtension> inProgressFuture;
    private byte[] responseBytes = null;

    /**
    * Instantiates a new TransportActionResponseToExtensionHandler
    */
    public TransportActionResponseToExtensionResponseHandler() {
        this.inProgressFuture = new CompletableFuture<>();
    }

    @Override
    public void handleResponse(TransportActionResponseToExtension response) {
        logger.info("received {}", response);

        // Set TransportActionResponseToExtension from response
        this.responseBytes = response.getResponseBytes();
        inProgressFuture.complete(response);
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("TransportActionResponseToExtensionRequest failed", exp);
        inProgressFuture.completeExceptionally(exp);
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public TransportActionResponseToExtension read(StreamInput in) throws IOException {
        return new TransportActionResponseToExtension(in);
    }

    /**
     * Waits for the TransportActionResponseToExtensionHandler future to complete
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
