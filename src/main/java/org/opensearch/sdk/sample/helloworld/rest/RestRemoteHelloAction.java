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
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.ExtensionActionResponse;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.sdk.BaseExtensionRestHandler;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.RouteHandler;
import org.opensearch.sdk.SDKClient;
import org.opensearch.sdk.action.ProxyAction;
import org.opensearch.sdk.action.ProxyActionRequest;
import org.opensearch.sdk.sample.helloworld.transport.SampleRequest;

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
        // This class happens to be local for simplicity but this should be from a dependency
        SampleRequest sampleRequest = new SampleRequest(name);
        // Serialize this request in a proxy action request
        ProxyActionRequest proxyActionRequest = new ProxyActionRequest(sampleRequest);

        // TODO: We need async client.execute to hide these action listener details and return the future directly
        // https://github.com/opensearch-project/opensearch-sdk-java/issues/584
        CompletableFuture<ExtensionActionResponse> futureResponse = new CompletableFuture<>();
        client.execute(
            ProxyAction.INSTANCE,
            proxyActionRequest,
            ActionListener.wrap(r -> futureResponse.complete(r), e -> futureResponse.completeExceptionally(e))
        );
        try {
            return new ExtensionRestResponse(
                request,
                OK,
                "Received remote extension reponse: "
                    + futureResponse.orTimeout(ExtensionsManager.EXTENSION_REQUEST_WAIT_TIMEOUT, TimeUnit.SECONDS).get().toString()
            );
        } catch (Exception e) {
            return exceptionalRequest(request, e);
        }
    };

}
