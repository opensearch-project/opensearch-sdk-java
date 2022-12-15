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
import org.opensearch.extensions.JobDetails;
import org.opensearch.extensions.JobDetailsResponse;

/**
 * This class handles the request from OpenSearch to fetch extension job details.
 */
public class ExtensionJobDetailsRequestHandler {
    private static final Logger logger = LogManager.getLogger(ExtensionJobDetailsRequestHandler.class);

    public JobDetailsResponse handleJobDetailsRequest(JobDetails jobDetails) {
        return new JobDetailsResponse(jobDetails);
    }

}
