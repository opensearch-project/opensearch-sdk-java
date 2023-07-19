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
        SDKRestRequest sdkRestRequest = (SDKRestRequest) request;
        List<String> authorizationHeaders = sdkRestRequest.getHeaders().get("Authorization");
        Map<String, String> headers = new HashMap<>();
        if (!authorizationHeaders.isEmpty()) {
            headers.put("Authorization", authorizationHeaders.get(0));
        }
        OpenSearchClient restClient1 = extensionsRunner.getSdkClient()
                .initializeJavaClientWithHeaders(headers);

        try {
            restClient1.indices().create(new CreateIndexRequest.Builder().index(".my-index").build());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (WarningFailureException e2) {
            System.out.println(e2.getMessage());
        }

        String name = request.param("name");

        return new ExtensionRestResponse(request, OK, String.format(GREETING, name));
    };

}
