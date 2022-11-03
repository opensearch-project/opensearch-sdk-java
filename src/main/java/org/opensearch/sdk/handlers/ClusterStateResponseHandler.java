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
import org.opensearch.action.admin.cluster.state.ClusterStateResponse;
import org.opensearch.cluster.ClusterState;
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
 * This class handles the response from OpenSearch to a {@link ExtensionsRunner#sendClusterStateRequest(TransportService)} call.
 */
public class ClusterStateResponseHandler implements TransportResponseHandler<ClusterStateResponse> {
    private static final Logger logger = LogManager.getLogger(ClusterStateResponseHandler.class);
    private final CountDownLatch inProgressLatch;
    private ClusterState clusterState;

    /**
    * Instantiates a new ClusterStateResponseHandler with a count down latch and an empty ClusterState object
    */
    public ClusterStateResponseHandler() {
        this.inProgressLatch = new CountDownLatch(1);
        this.clusterState = ClusterState.EMPTY_STATE;
    }

    @Override
    public void handleResponse(ClusterStateResponse response) {
        logger.info("received {}", response);

        // Set cluster state from response
        this.clusterState = response.getState();
        inProgressLatch.countDown();
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("ExtensionClusterStateRequest failed", exp);
        inProgressLatch.countDown();
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public ClusterStateResponse read(StreamInput in) throws IOException {
        return new ClusterStateResponse(in);
    }

    /**
     * Invokes await on the ClusterStateResponseHandler count down latch
     */
    public void awaitResponse() throws InterruptedException {
        inProgressLatch.await(ExtensionsOrchestrator.EXTENSION_REQUEST_WAIT_TIMEOUT, TimeUnit.SECONDS);
    }

    public ClusterState getClusterState() {
        return this.clusterState;
    }
}
