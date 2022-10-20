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
import org.opensearch.cluster.LocalNodeResponse;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.extensions.ExtensionsOrchestrator;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the response from OpenSearch to a {@link ExtensionsRunner#sendLocalNodeRequest(TransportService)} call.
 */
public class LocalNodeResponseHandler implements TransportResponseHandler<LocalNodeResponse> {
    private static final Logger logger = LogManager.getLogger(LocalNodeResponseHandler.class);
    private final CountDownLatch inProgressLatch;
    private DiscoveryNode localNode;

    /**
     * Instantiates a new LocalNodeResponseHandler with a count down latch
     */
    public LocalNodeResponseHandler() {
        this.inProgressLatch = new CountDownLatch(1);
        this.localNode = null;
    }

    @Override
    public void handleResponse(LocalNodeResponse response) {
        logger.info("received {}", response);

        // Set local node from response
        this.localNode = response.getLocalNode();
        inProgressLatch.countDown();
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("LocalNodeRequest failed", exp);
        inProgressLatch.countDown();
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public LocalNodeResponse read(StreamInput in) throws IOException {
        return new LocalNodeResponse(in);
    }

    /**
     * Invokes await on the LocalNodeResponseHandler count down latch
     */
    public void awaitResponse() throws InterruptedException {
        inProgressLatch.await(ExtensionsOrchestrator.EXTENSION_REQUEST_WAIT_TIMEOUT, TimeUnit.SECONDS);
    }

    public DiscoveryNode getLocalNode() {
        return this.localNode;
    }
}
