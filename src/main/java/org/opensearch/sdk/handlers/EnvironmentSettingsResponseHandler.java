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
import org.opensearch.env.EnvironmentSettingsResponse;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.settings.Settings;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the response from OpenSearch to a {@link org.opensearch.sdk.SDKTransportService#sendEnvironmentSettingsRequest} call.
 */
public class EnvironmentSettingsResponseHandler implements TransportResponseHandler<EnvironmentSettingsResponse> {

    private static final Logger logger = LogManager.getLogger(EnvironmentSettingsResponseHandler.class);
    private final CompletableFuture<EnvironmentSettingsResponse> inProgressFuture;
    private Settings environmentSettings;

    /**
    * Instantiates a new EnvironmentSettingsResponseHandler with a count down latch and an empty Settings object
    */
    public EnvironmentSettingsResponseHandler() {
        this.inProgressFuture = new CompletableFuture<>();
        this.environmentSettings = Settings.EMPTY;
    }

    @Override
    public void handleResponse(EnvironmentSettingsResponse response) {
        logger.info("received {}", response);

        // Set environmentSettings from response
        this.environmentSettings = response.getEnvironmentSettings();
        inProgressFuture.complete(response);
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("EnvironmentSettingsRequest failed", exp);
        inProgressFuture.completeExceptionally(exp);
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public EnvironmentSettingsResponse read(StreamInput in) throws IOException {
        return new EnvironmentSettingsResponse(in);
    }

    /**
     * Invokes await on the EnvironmentSettingsResponseHandler count down latch
     * @throws Exception
     *        if the response times out
     */
    public void awaitResponse() throws Exception {
        inProgressFuture.orTimeout(ExtensionsManager.EXTENSION_REQUEST_WAIT_TIMEOUT, TimeUnit.SECONDS).get();
    }

    public Settings getEnvironmentSettings() {
        return this.environmentSettings;
    }
}
