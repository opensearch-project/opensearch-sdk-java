package org.opensearch.sdk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.opensearch.cluster.ClusterSettingsResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ClusterSettingResponseHandler implements TransportResponseHandler<ClusterSettingsResponse> {
    private static final Logger logger = LogManager.getLogger(ClusterSettingResponseHandler.class);
    final CountDownLatch inProgressLatch = new CountDownLatch(1);

    @Override
    public void handleResponse(ClusterSettingsResponse response) {
        logger.info("received {}", response);
        inProgressLatch.countDown();
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("ClusterSettingRequest failed", exp);
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public ClusterSettingsResponse read(StreamInput in) throws IOException {
        return new ClusterSettingsResponse(in);
    }
}
