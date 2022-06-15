package org.opensearch.sdk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.cluster.ExtensionClusterStateResponse;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ExtensionClusterStateResponseHandler implements TransportResponseHandler<ExtensionClusterStateResponse> {
    private static final Logger logger = LogManager.getLogger(ExtensionClusterStateResponseHandler.class);
    final CountDownLatch inProgressLatch = new CountDownLatch(1);

    @Override
    public void handleResponse(ExtensionClusterStateResponse response) {
        logger.info("received {}", response);
        inProgressLatch.countDown();
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("ClusterStateRequest failed", exp);
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public ExtensionClusterStateResponse read(StreamInput in) throws IOException {
        return new ExtensionClusterStateResponse(in);
    }
}
