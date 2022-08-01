/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.extensions.RegisterApiResponse;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;
import org.opensearch.transport.TransportService;

import java.io.IOException;

/**
 * This class handles the response from OpenSearch to a {@link ExtensionsRunner#sendRegisterApiRequest(TransportService)} call.
 */
public class RegisterApiResponseHandler implements TransportResponseHandler<RegisterApiResponse> {
    private static final Logger logger = LogManager.getLogger(RegisterApiResponseHandler.class);

    @Override
    public void handleResponse(RegisterApiResponse response) {
        logger.info("received {}", response);
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("RegisterApiRequest failed", exp);
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public RegisterApiResponse read(StreamInput in) throws IOException {
        return new RegisterApiResponse(in);
    }
}
