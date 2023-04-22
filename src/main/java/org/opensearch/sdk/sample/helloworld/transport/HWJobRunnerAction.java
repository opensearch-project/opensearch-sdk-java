/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.transport;

import org.opensearch.action.ActionType;
import org.opensearch.jobscheduler.transport.response.JobRunnerResponse;

public class HWJobRunnerAction extends ActionType<JobRunnerResponse> {

    public static final String NAME = "extensions:hw/greet_job_runner";
    public static final HWJobRunnerAction INSTANCE = new HWJobRunnerAction();

    private HWJobRunnerAction() {
        super(NAME, JobRunnerResponse::new);
    }
}