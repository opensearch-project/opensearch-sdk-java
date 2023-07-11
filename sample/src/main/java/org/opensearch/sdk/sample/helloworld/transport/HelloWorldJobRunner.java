/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.jobscheduler.spi.JobExecutionContext;
import org.opensearch.jobscheduler.spi.ScheduledJobParameter;
import org.opensearch.jobscheduler.spi.ScheduledJobRunner;

/**
 * Hello World Job Runner
 */
public class HelloWorldJobRunner implements ScheduledJobRunner {
    private static final Logger log = LogManager.getLogger(HelloWorldJobRunner.class);

    private static HelloWorldJobRunner INSTANCE;

    /**
     *
     * @return Return or create an instance of this job runner
     */
    public static HelloWorldJobRunner getJobRunnerInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (HelloWorldJobRunner.class) {
            if (INSTANCE != null) {
                return INSTANCE;
            }
            INSTANCE = new HelloWorldJobRunner();
            return INSTANCE;
        }
    }

    @Override
    public void runJob(ScheduledJobParameter job, JobExecutionContext context) {
        System.out.println("Hello, world!");
    }
}
