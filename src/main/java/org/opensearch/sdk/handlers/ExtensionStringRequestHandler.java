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
import org.opensearch.extensions.ExtensionStringResponse;

/**
 * This class handles the request from OpenSearch to fetch extension job details like jobType, jobIndex.
 */
public class ExtensionStringRequestHandler {
    private static final Logger logger = LogManager.getLogger(ExtensionStringRequestHandler.class);

    /**
     * Handles the request from OpenSearch to handle ExtensionStringRequest
     *
     * @param response The response to be returned as per request
     */
    public ExtensionStringResponse handleRequest(String response) {
        logger.info("Registering getJobDetails Request received from OpenSearch  " + response);
        return new ExtensionStringResponse(response);
    }

}
