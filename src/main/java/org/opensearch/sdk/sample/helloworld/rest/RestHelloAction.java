/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.helloworld.rest;

import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.rest.RestResponse;
import org.opensearch.sdk.ExtensionRestHandler;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestStatus.OK;
import static org.opensearch.rest.RestStatus.INTERNAL_SERVER_ERROR;

/**
 * Sample REST Handler (REST Action). Extension REST handlers must implement {@link ExtensionRestHandler}.
 */
public class RestHelloAction implements ExtensionRestHandler {

    private static final String GREETING = "Hello, World!";

    @Override
    public List<Route> routes() {
        return singletonList(new Route(GET, "/hello"));
    }

    @Override
    public RestResponse handleRequest(Method method, String uri) {
        if (Method.GET.equals(method) && "/hello".equals(uri)) {
            return new BytesRestResponse(OK, GREETING);
        }
        return new BytesRestResponse(
            INTERNAL_SERVER_ERROR,
            "Extension REST action improperly configured to handle " + method.name() + " " + uri
        );
    }

}
