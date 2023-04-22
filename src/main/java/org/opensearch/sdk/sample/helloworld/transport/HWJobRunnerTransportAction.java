/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.transport;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.TransportAction;
import org.opensearch.common.inject.Provides;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.jobscheduler.spi.JobExecutionContext;
import org.opensearch.jobscheduler.transport.request.JobRunnerRequest;
import org.opensearch.jobscheduler.transport.response.JobRunnerResponse;
import org.opensearch.sdk.SDKClient.SDKRestClient;
import org.opensearch.sdk.SDKNamedXContentRegistry;
import org.opensearch.sdk.sample.helloworld.schedule.GreetJob;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskManager;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.opensearch.common.xcontent.XContentParserUtils.ensureExpectedToken;
import static org.opensearch.sdk.sample.helloworld.util.RestHandlerUtils.wrapRestActionListener;

public class HWJobRunnerTransportAction extends TransportAction<JobRunnerRequest, JobRunnerResponse> {

    private static final Logger LOG = LogManager.getLogger(HWJobRunnerTransportAction.class);

    private SDKRestClient client;
    private final SDKNamedXContentRegistry xContentRegistry;

    @Inject
    protected HWJobRunnerTransportAction(
            ActionFilters actionFilters,
            TaskManager taskManager,
            SDKNamedXContentRegistry xContentRegistry,
            SDKRestClient client
    ) {
        super(HWJobRunnerAction.NAME, actionFilters, taskManager);
        this.client = client;
        this.xContentRegistry = xContentRegistry;
    }

    @Override
    protected void doExecute(Task task, JobRunnerRequest request, ActionListener<JobRunnerResponse> actionListener) {
        String errorMessage = "Failed to run the Job";
        ActionListener<JobRunnerResponse> listener = wrapRestActionListener(actionListener, errorMessage);
        try {
            JobExecutionContext jobExecutionContext = request.getJobExecutionContext();
            String jobParameterDocumentId = jobExecutionContext.getJobId();
            if (jobParameterDocumentId == null || jobParameterDocumentId.isEmpty()) {
                listener.onFailure(new IllegalArgumentException("jobParameterDocumentId cannot be empty or null"));
            } else {
                CompletableFuture<GreetJob> inProgressFuture = new CompletableFuture<>();
                findById(jobParameterDocumentId, new ActionListener<>() {
                    @Override
                    public void onResponse(GreetJob anomalyDetectorJob) {
                        inProgressFuture.complete(anomalyDetectorJob);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        logger.info("could not find GreetJob with id " + jobParameterDocumentId, e);
                        inProgressFuture.completeExceptionally(e);
                    }
                });

                try {
                    GreetJob scheduledJobParameter = inProgressFuture.orTimeout(10000, TimeUnit.MILLISECONDS).join();

                    JobRunnerResponse jobRunnerResponse;
                    if (scheduledJobParameter != null && validateJobExecutionContext(jobExecutionContext)) {
                        jobRunnerResponse = new JobRunnerResponse(true);
                    } else {
                        jobRunnerResponse = new JobRunnerResponse(false);
                    }
                    listener.onResponse(jobRunnerResponse);
                    if (jobRunnerResponse.getJobRunnerStatus()) {
                        HelloWorldJobRunner.getJobRunnerInstance().runJob(scheduledJobParameter, jobExecutionContext);
                    }
                } catch (CompletionException e) {
                    if (e.getCause() instanceof TimeoutException) {
                        logger.info(" Request timed out with an exception ", e);
                    } else {
                        throw e;
                    }
                } catch (Exception e) {
                    logger.info(" Could not find Job Parameter due to exception ", e);
                }

            }
        } catch (Exception e) {
            LOG.error(e);
            listener.onFailure(e);
        }
    }

    private void findById(String jobParameterId, ActionListener<GreetJob> listener) {
        GetRequest getRequest = new GetRequest(GreetJob.HELLO_WORLD_JOB_INDEX, jobParameterId);
        try {
            client.get(getRequest, ActionListener.wrap(response -> {
                if (!response.isExists()) {
                    listener.onResponse(null);
                } else {
                    try {
                        XContentParser parser = XContentType.JSON.xContent()
                                .createParser(xContentRegistry.getRegistry(), LoggingDeprecationHandler.INSTANCE, response.getSourceAsString());
                        ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.nextToken(), parser);
                        listener.onResponse(GreetJob.parse(parser));
                    } catch (IOException e) {
                        logger.error("IOException occurred finding GreetJob for jobParameterId " + jobParameterId, e);
                        listener.onFailure(e);
                    }
                }
            }, exception -> {
                logger.error("Exception occurred finding GreetJob for jobParameterId " + jobParameterId, exception);
                listener.onFailure(exception);
            }));
        } catch (Exception e) {
            logger.error("Error occurred finding greet job with jobParameterId " + jobParameterId, e);
            listener.onFailure(e);
        }

    }

    private boolean validateJobExecutionContext(JobExecutionContext jobExecutionContext) {
        if (jobExecutionContext != null
                && jobExecutionContext.getJobId() != null
                && !jobExecutionContext.getJobId().isEmpty()
                && jobExecutionContext.getJobIndexName() != null
                && !jobExecutionContext.getJobIndexName().isEmpty()
                && jobExecutionContext.getExpectedExecutionTime() != null
                && jobExecutionContext.getJobVersion() != null) {
            return true;
        }
        return false;
    }
}