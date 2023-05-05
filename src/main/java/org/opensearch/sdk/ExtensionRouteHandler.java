/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.opensearch.rest.RestRequest;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.extensions.rest.RouteHandler;

import java.util.function.Function;

public class ExtensionRouteHandler extends RouteHandler {

    public ExtensionRouteHandler(
        String handlerName,
        RestRequest.Method method,
        String path,
        Function<RestRequest, ExtensionRestResponse> handler
    ) {
        super(ExtensionRouteHandlerFactory.getInstance().generateRouteName(handlerName), method, path, handler);
    }
}
