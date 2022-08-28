/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.rest;

import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.sdk.ExtensionAction;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.opensearch.rest.RestRequest.Method.GET;

/**
 * Sample Action. Extension Actions must implement {@link ExtensionAction}.
 */
public class RestHelloAction implements ExtensionAction {

    private static final String GREETING = "Hello, World!";

    @Override
    public List<Route> routes() {
        return singletonList(new Route(GET, "/hello"));
    }

    @Override
    public String getExtensionResponse(Method method, String uri) {
        if (Method.GET.equals(method) && "/hello".equals(uri)) {
            return GREETING;
        }
        return null;
    }

}
