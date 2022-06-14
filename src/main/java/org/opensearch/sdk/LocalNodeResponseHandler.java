package org.opensearch.sdk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.opensearch.cluster.LocalNodeResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class LocalNodeResponseHandler implements TransportResponseHandler<LocalNodeResponse> {
    private static final Logger logger = LogManager.getLogger(LocalNodeResponseHandler.class);
    final CountDownLatch inProgressLatch = new CountDownLatch(1);

    @Override
    public void handleResponse(LocalNodeResponse response) {
        logger.info("received {}", response);
        inProgressLatch.countDown();
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("LocalNodeRequest failed", exp);
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public LocalNodeResponse read(StreamInput in) throws IOException {
        return new LocalNodeResponse(in);
    }
}
