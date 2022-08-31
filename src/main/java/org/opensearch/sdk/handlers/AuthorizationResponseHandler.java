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
import org.opensearch.extensions.rest.AuthorizationResponse;
import org.opensearch.extensions.rest.RegisterRestActionsResponse;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;
import org.opensearch.transport.TransportService;

import java.io.IOException;

/**
 * This class handles the response from OpenSearch to a {@link ExtensionsRunner#sendRegisterRestActionsRequest(TransportService)} call.
 */
public class AuthorizationResponseHandler implements TransportResponseHandler<AuthorizationResponse> {
    private static final Logger logger = LogManager.getLogger(AuthorizationResponseHandler.class);

    @Override
    public void handleResponse(AuthorizationResponse response) {
        logger.info("received {}", response.getResponse());
    }

    @Override
    public void handleException(TransportException exp) {
        logger.info("RegisterActionsRequest failed", exp);
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    public AuthorizationResponse read(StreamInput in) throws IOException {
        return new AuthorizationResponse(in);
    }
}
