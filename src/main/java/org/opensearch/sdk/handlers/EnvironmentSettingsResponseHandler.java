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
import org.opensearch.extensions.ExtensionsOrchestrator;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.settings.Settings;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the response from OpenSearch to a {@link ExtensionsRunner#sendEnvironmentSettingsRequest} call.
 */
public class EnvironmentSettingsResponseHandler implements TransportResponseHandler<EnvironmentSettingsResponse> {

    private static final Logger logger = LogManager.getLogger(EnvironmentSettingsResponseHandler.class);
    private final CountDownLatch inProgressLatch;
    private Settings environmentSettings;

    /**
    * Instantiates a new EnvironmentSettingsResponseHandler with a count down latch and an empty Settings object
    */
    public EnvironmentSettingsResponseHandler() {
        this.inProgressLatch = new CountDownLatch(1);
        this.environmentSettings = Settings.EMPTY;
    }

    @Override
    public void handleResponse(EnvironmentSettingsResponse response) {
        logger.info("received {}", response);

        // Set environmentSettings from response
        this.environmentSettings = response.getEnvironmentSettings();
        inProgressLatch.countDown();
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("EnvironmentSettingsRequest failed", exp);
        inProgressLatch.countDown();
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
     * @throws InterruptedException
     *        if await returns an exception
     */
    public void awaitResponse() throws InterruptedException {
        inProgressLatch.await(ExtensionsOrchestrator.EXTENSION_REQUEST_WAIT_TIMEOUT, TimeUnit.SECONDS);
    }

    public Settings getEnvironmentSettings() {
        return this.environmentSettings;
    }
}
