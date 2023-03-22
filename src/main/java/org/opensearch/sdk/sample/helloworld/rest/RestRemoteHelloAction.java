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
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.ExtensionActionResponse;
import org.opensearch.extensions.action.RemoteExtensionActionResponse;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.sdk.BaseExtensionRestHandler;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.RouteHandler;
import org.opensearch.sdk.SDKClient;
import org.opensearch.sdk.action.ProxyAction;
import org.opensearch.sdk.action.ProxyActionRequest;
import org.opensearch.sdk.sample.helloworld.transport.SampleAction;
import org.opensearch.sdk.sample.helloworld.transport.SampleRequest;
import org.opensearch.sdk.sample.helloworld.transport.SampleResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestStatus.OK;

/**
 * Sample REST Handler demostrating proxy actions to another extension
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
    public List<RouteHandler> routeHandlers() {
        return List.of(new RouteHandler(GET, "/hello/{name}", handleRemoteGetRequest));
    }

    private Function<ExtensionRestRequest, ExtensionRestResponse> handleRemoteGetRequest = (request) -> {
        SDKClient client = extensionsRunner.getSdkClient();

        String name = request.param("name");
        // Create a request using class on remote
        // This class happens to be local for simplicity but is a class on the remote extension
        SampleRequest sampleRequest = new SampleRequest(name);

        // Serialize this request in a proxy action request
        // This requires that the remote extension has a corresponding transport action registered
        // This Action class happens to be local for simplicity but is a class on the remote extension
        ProxyActionRequest proxyActionRequest = new ProxyActionRequest(SampleAction.INSTANCE, sampleRequest);

        // TODO: We need async client.execute to hide these action listener details and return the future directly
        // https://github.com/opensearch-project/opensearch-sdk-java/issues/584
        CompletableFuture<RemoteExtensionActionResponse> futureResponse = new CompletableFuture<>();
        client.execute(
            ProxyAction.INSTANCE,
            proxyActionRequest,
            ActionListener.wrap(r -> futureResponse.complete(r), e -> futureResponse.completeExceptionally(e))
        );
        try {
            RemoteExtensionActionResponse response = futureResponse.orTimeout(ExtensionsManager.EXTENSION_REQUEST_WAIT_TIMEOUT, TimeUnit.SECONDS)
                .get();
            if (!response.isSuccess()) {
                return new ExtensionRestResponse(request, OK, "Remote extension reponse failed: " + response.getResponseBytesAsString());
            }
            // Parse out the expected response class from the bytes
            SampleResponse sampleResponse = new SampleResponse(StreamInput.wrap(response.getResponseBytes()));
            return new ExtensionRestResponse(request, OK, "Received greeting from remote extension: " + sampleResponse.getGreeting());
        } catch (Exception e) {
            return exceptionalRequest(request, e);
        }
    };

}
