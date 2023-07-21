/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.rest;

import org.opensearch.action.ActionListener;
import org.opensearch.client.WarningFailureException;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.RemoteExtensionActionResponse;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.NamedRoute;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestResponse;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.SDKClient;
import org.opensearch.sdk.action.RemoteExtensionAction;
import org.opensearch.sdk.action.RemoteExtensionActionRequest;
import org.opensearch.sdk.rest.BaseExtensionRestHandler;
import org.opensearch.sdk.rest.SDKRestRequest;
import org.opensearch.sdk.sample.helloworld.transport.SampleAction;
import org.opensearch.sdk.sample.helloworld.transport.SampleRequest;
import org.opensearch.sdk.sample.helloworld.transport.SampleResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.core.rest.RestStatus.OK;
import static org.opensearch.sdk.sample.helloworld.rest.RestHelloAction.GREETING;

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
        super(runner.getSdkClient());
        this.extensionsRunner = runner;
    }

    @Override
    public List<NamedRoute> routes() {
        return List.of(

            new NamedRoute.Builder().method(GET)
                .path("/hello/{name}")
                .handler(handleRemoteGetRequest)
                .uniqueName(addRouteNamePrefix("remote_greet_with_name"))
                .legacyActionNames(Collections.emptySet())
                .build(),
            new NamedRoute.Builder().method(GET)
                .path("/greet/{name}")
                .handler(handleLocalGetRequest)
                .uniqueName(addRouteNamePrefix("local_greet_with_name"))
                .legacyActionNames(Collections.emptySet())
                .build(),
            new NamedRoute.Builder().method(GET)
                .path("/service_account_token_example")
                .handler(handleServiceAccountTokenExampleRequest)
                .uniqueName(addRouteNamePrefix("service_account_token_example"))
                .legacyActionNames(Collections.emptySet())
                .build()
        );
    }

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

    private Function<RestRequest, RestResponse> handleLocalGetRequest = (request) -> {
        // Example usage of userRestClient
        try {
            userRestClient.indices().create(new CreateIndexRequest.Builder().index(".my-index").build());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (WarningFailureException e2) {
            System.out.println(e2.getMessage());
        }

        String name = request.param("name");

        return new ExtensionRestResponse(request, OK, String.format(GREETING, name));
    };

    private Function<RestRequest, RestResponse> handleServiceAccountTokenExampleRequest = (request) -> {
        // Uncomment the lines below to try out different actions utilizing the service account token

        OpenSearchClient adminRestClient = extensionsRunner.getSdkClient()
                .initializeJavaClientWithHeaders(
                        Map.of("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)))
                );

        try {
            adminRestClient.indices().create(new CreateIndexRequest.Builder().index(".hello-world-jobs").build());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (WarningFailureException e2) {
            System.out.println(e2.getMessage());
        }

        // Example usage of extension rest client - utilizing service account token
        try {
            extensionsRunner.getExtensionRestClient().indices().delete(new DeleteIndexRequest.Builder().index(".hello-world-jobs").build());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (WarningFailureException e2) {
            System.out.println(e2.getMessage());
        }

        // Try reading from index with service account token

//        try {
//            adminRestClient.indices().create(new CreateIndexRequest.Builder().index("logs-123").build());
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        } catch (WarningFailureException e2) {
//            System.out.println(e2.getMessage());
//        }
//
//        try {
//            SearchRequest searchRequest = new SearchRequest.Builder()
//                    .index("logs-123")
//                    .build();
//            SearchResponse<JsonNode> searchResponse = userRestClient.search(searchRequest, JsonNode.class);
//            System.out.println("SearchResponse: " + searchResponse);
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        } catch (WarningFailureException e2) {
//            System.out.println(e2.getMessage());
//        }
//
//        try {
//            IndexData indexData = new IndexData("John", "Doe");
//            IndexRequest<IndexData> indexRequest = new IndexRequest.Builder<IndexData>().index("logs-123").id("1").document(indexData).build();
//            userRestClient.index(indexRequest);
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        } catch (WarningFailureException e2) {
//            System.out.println(e2.getMessage());
//        }

        return new ExtensionRestResponse(request, OK, String.format(GREETING, "World"));
    };

    static class IndexData {
        private String firstName;
        private String lastName;

        public IndexData(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        @Override
        public String toString() {
            return String.format("IndexData{first name='%s', last name='%s'}", firstName, lastName);
        }
    }

}
