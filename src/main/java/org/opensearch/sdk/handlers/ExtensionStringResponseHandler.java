/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.extensions.ExtensionStringResponse;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;
import java.io.IOException;

/**
 * This class handles the response from OpenSearch to call returning an {@link ExtensionStringResponse}.
 */
public class ExtensionStringResponseHandler implements TransportResponseHandler<ExtensionStringResponse> {
    private static final Logger logger = LogManager.getLogger(ExtensionStringResponseHandler.class);

    @Override
    public void handleResponse(ExtensionStringResponse response) {
        logger.info("received {}", response.getResponse());
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("Request failed", exp);
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public ExtensionStringResponse read(StreamInput in) throws IOException {
        return new ExtensionStringResponse(in);
    }
}
