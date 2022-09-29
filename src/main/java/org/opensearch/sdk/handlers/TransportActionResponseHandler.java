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
import org.opensearch.extensions.action.TransportActionResponseToExtension;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;

import java.io.IOException;

/**
 * This class handles the response {{@link org.opensearch.extensions.ExtensionBooleanResponse }} from OpenSearch to Extension.
 */
public class TransportActionResponseHandler implements TransportResponseHandler<TransportActionResponseToExtension> {
    private static final Logger logger = LogManager.getLogger(ExtensionBooleanResponseHandler.class);

    @Override
    public void handleResponse(TransportActionResponseToExtension response) {
        logger.info("received {}", response);

    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("Extension Request failed", exp);
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public TransportActionResponseToExtension read(StreamInput in) throws IOException {
        return new TransportActionResponseToExtension(in);
    }
}
