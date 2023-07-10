/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.rest;

import com.google.common.io.Resources;
import jakarta.json.stream.JsonParser;
import org.apache.commons.codec.Charsets;
import org.opensearch.OpenSearchException;
import org.opensearch.ResourceAlreadyExistsException;
import org.opensearch.action.ActionListener;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.WarningFailureException;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.json.jsonb.JsonbJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.GetMappingResponse;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.common.Strings;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.RemoteExtensionActionResponse;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.jobscheduler.JobSchedulerPlugin;
import org.opensearch.jobscheduler.rest.request.GetJobDetailsRequest;
import org.opensearch.jobscheduler.spi.schedule.IntervalSchedule;
import org.opensearch.jobscheduler.spi.schedule.Schedule;
import org.opensearch.rest.NamedRoute;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestResponse;
import org.opensearch.rest.RestStatus;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.SDKClient;
import org.opensearch.sdk.action.RemoteExtensionAction;
import org.opensearch.sdk.action.RemoteExtensionActionRequest;
import org.opensearch.sdk.rest.BaseExtensionRestHandler;
import org.opensearch.sdk.sample.helloworld.schedule.GreetJob;
import org.opensearch.sdk.sample.helloworld.transport.HWJobParameterAction;
import org.opensearch.sdk.sample.helloworld.transport.HWJobRunnerAction;
import org.opensearch.sdk.sample.helloworld.transport.SampleAction;
import org.opensearch.sdk.sample.helloworld.transport.SampleRequest;
import org.opensearch.sdk.sample.helloworld.transport.SampleResponse;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestRequest.Method.PUT;
import static org.opensearch.rest.RestStatus.OK;

/**
 * Sample REST Handler demonstrating proxy actions to another extension
 */
public class RestRemoteHelloAction extends BaseExtensionRestHandler {
    private ExtensionsRunner extensionsRunner;

    /**
     * Instantiate this action
     *
     * @param runner The ExtensionsRunner instance
     */
    public RestRemoteHelloAction(ExtensionsRunner runner) {
        this.extensionsRunner = runner;
    }

    @Override
    public List<NamedRoute> routes() {
        return List.of(
                new NamedRoute.Builder().method(GET)
                        .path("/hello/{name}")
                        .handler(handleRemoteGetRequest)
                        .uniqueName(routePrefix("remote_greet_with_name"))
                        .legacyActionNames(Collections.emptySet())
                        .build(),
                new NamedRoute.Builder().method(GET)
                        .path("/schedule/hello")
                        .handler(handleScheduleRequest)
                        .uniqueName(routePrefix("scheduled_greet"))
                        .legacyActionNames(Collections.emptySet())
                        .build()
        );
    }

    private void registerJobDetails(SDKClient.SDKRestClient client) throws IOException {

        XContentBuilder requestBody = JsonXContent.contentBuilder();
        requestBody.startObject();
        requestBody.field(GetJobDetailsRequest.JOB_INDEX, GreetJob.HELLO_WORLD_JOB_INDEX);
        requestBody.field(GetJobDetailsRequest.JOB_TYPE, GreetJob.PARSE_FIELD_NAME);
        requestBody.field(GetJobDetailsRequest.JOB_PARAMETER_ACTION, HWJobParameterAction.class.getName());
        requestBody.field(GetJobDetailsRequest.JOB_RUNNER_ACTION, HWJobRunnerAction.class.getName());
        requestBody.field(GetJobDetailsRequest.EXTENSION_UNIQUE_ID, extensionsRunner.getSdkTransportService().getUniqueId());
        requestBody.endObject();

        Request request = new Request("PUT", String.format(Locale.ROOT, "%s/%s", JobSchedulerPlugin.JS_BASE_URI, "_job_details"));
        request.setJsonEntity(Strings.toString(requestBody));

        Response response = client.performRequest(request);
        boolean registeredJobDetails = RestStatus.fromCode(response.getStatusLine().getStatusCode()) == RestStatus.OK ? true : false;
    }

    /**
     * Get hello world job index mapping json content.
     *
     * @return hello world job index mapping
     * @throws IOException IOException if mapping file can't be read correctly
     */
    public static String getHelloWorldJobMappings() throws IOException {
        URL url = RestRemoteHelloAction.class.getClassLoader().getResource("mappings/hello-world-jobs.json");
        return Resources.toString(url, Charsets.UTF_8);
    }

    private JsonpMapper setupMapper(int rand) {
        // Randomly choose json-b or jackson
        if (rand % 2 == 0) {
            return new JsonbJsonpMapper() {
                @Override
                public boolean ignoreUnknownFields() {
                    return false;
                }
            };
        } else {
            return new JacksonJsonpMapper() {
                @Override
                public boolean ignoreUnknownFields() {
                    return false;
                }
            };
        }
    }

    /**
     * Deserializes json string into a java object
     *
     * @param json The JSON string
     * @param clazz The class to deserialize to
     * @param <T> The class to deserialize to
     *
     * @return Object instance of clazz requested
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        int rand = new Random().nextInt();
        JsonpMapper mapper = setupMapper(rand);
        JsonParser parser = mapper.jsonProvider().createParser(new StringReader(json));
        return mapper.deserialize(parser, clazz);
    }

    private Function<RestRequest, RestResponse> handleScheduleRequest = (request) -> {
        SDKClient client = extensionsRunner.getSdkClient();
        SDKClient.SDKRestClient restClient = client.initializeRestClient();
        OpenSearchClient javaClient = client.initializeJavaClient();

        try {
            registerJobDetails(restClient);
        } catch (WarningFailureException e) {
            // ignore
        } catch (Exception e) {
            if (e instanceof ResourceAlreadyExistsException
                || e.getCause() instanceof ResourceAlreadyExistsException
                || e.getMessage().contains("resource_already_exists_exception")) {
                // ignore
            } else {
                // Catch all other OpenSearchExceptions
                return new ExtensionRestResponse(request, RestStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }

        try {
            String mappingsJson = getHelloWorldJobMappings();
            GetMappingResponse response = fromJson(mappingsJson, GetMappingResponse.class);
            TypeMapping mappings = response.get(GreetJob.HELLO_WORLD_JOB_INDEX).mappings();

            CreateIndexRequest cir = new CreateIndexRequest.Builder().index(GreetJob.HELLO_WORLD_JOB_INDEX).mappings(mappings).build();

            OpenSearchIndicesClient indicesClient = javaClient.indices();
            indicesClient.create(cir);
        } catch (IOException e) {
            return new ExtensionRestResponse(request, RestStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (WarningFailureException e) {
            // TODO This is failing on ConvertResponse. Ignoring.
            /*
             * org.opensearch.transport.RemoteTransportException: [hello-world][127.0.0.1:4532][internal:extensions/restexecuteonextensiontaction]
             * Caused by: org.opensearch.common.io.stream.NotSerializableExceptionWrapper: warning_failure_exception: method [PUT], host [https://127.0.0.1:9200], URI [/.hello-world-jobs], status line [HTTP/2.0 200 OK]
             * Warnings: [index name [.hello-world-jobs] starts with a dot '.', in the next major version, index names starting with a dot are reserved for hidden indices and system indices, this request accesses system indices: [.opendistro_security], but in a future major version, direct access to system indices will be prevented by default]
             * {"acknowledged":true,"shards_acknowledged":true,"index":".hello-world-jobs"}
             */
        } catch (OpenSearchException e) {
            if (e instanceof ResourceAlreadyExistsException || e.getCause() instanceof ResourceAlreadyExistsException) {} else {
                // Catch all other OpenSearchExceptions
                return new ExtensionRestResponse(request, RestStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } catch (Exception e) {
            if (e.getMessage().contains("resource_already_exists_exception")) {} else {
                // Catch all other OpenSearchExceptions
                return new ExtensionRestResponse(request, RestStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }

        Schedule schedule = new IntervalSchedule(Instant.now(), 1, ChronoUnit.MINUTES);
        Duration duration = Duration.of(1, ChronoUnit.MINUTES);

        GreetJob job = new GreetJob("hw", schedule, true, Instant.now(), null, Instant.now(), duration.getSeconds());

        try {
            // Reference: AnomalyDetector - IndexAnomalyDetectorJobActionHandler.indexAnomalyDetectorJob
            IndexRequest ir = new IndexRequest.Builder().index(GreetJob.HELLO_WORLD_JOB_INDEX).document(job.toPojo()).build();
            javaClient.index(ir);
        } catch (IOException e) {
            return new ExtensionRestResponse(request, RestStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        return new ExtensionRestResponse(request, OK, "GreetJob successfully scheduled");
    };

    private Function<RestRequest, RestResponse> handleRemoteGetRequest = (request) -> {
        SDKClient client = extensionsRunner.getSdkClient();

        String name = request.param("name");
        // Create a request using class on remote
        // This class happens to be local for simplicity but is a class on the remote extension
        SampleRequest sampleRequest = new SampleRequest(name);

        // Serialize this request in a proxy action request
        // This requires that the remote extension has a corresponding transport action registered
        // This Action class happens to be local for simplicity but is a class on the remote extension
        RemoteExtensionActionRequest proxyActionRequest = new RemoteExtensionActionRequest(SampleAction.INSTANCE, sampleRequest);

        // TODO: We need async client.execute to hide these action listener details and return the future directly
        // https://github.com/opensearch-project/opensearch-sdk-java/issues/584
        CompletableFuture<RemoteExtensionActionResponse> futureResponse = new CompletableFuture<>();
        client.execute(
            RemoteExtensionAction.INSTANCE,
            proxyActionRequest,
            ActionListener.wrap(r -> futureResponse.complete(r), e -> futureResponse.completeExceptionally(e))
        );
        try {
            RemoteExtensionActionResponse response = futureResponse.orTimeout(
                ExtensionsManager.EXTENSION_REQUEST_WAIT_TIMEOUT,
                TimeUnit.SECONDS
            ).get();
            if (!response.isSuccess()) {
                return new ExtensionRestResponse(request, OK, "Remote extension response failed: " + response.getResponseBytesAsString());
            }
            // Parse out the expected response class from the bytes
            SampleResponse sampleResponse = new SampleResponse(StreamInput.wrap(response.getResponseBytes()));
            return new ExtensionRestResponse(request, OK, "Received greeting from remote extension: " + sampleResponse.getGreeting());
        } catch (Exception e) {
            return exceptionalRequest(request, e);
        }
    };

}
