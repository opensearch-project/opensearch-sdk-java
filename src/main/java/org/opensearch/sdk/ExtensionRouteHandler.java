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

/**
 * Subclass of RouteHandler that utilizes a naming convention for extension routes that prepends
 * the extension's shortName (abbreviation) before any name for a route. i.e. hw:greet for the
 * HelloWorldExtension greet REST api
 */
public class ExtensionRouteHandler extends RouteHandler {

    /**
     *
     * @param handlerName A shortened name for this REST api's handler
     * @param method HTTP Method
     * @param path The path of the api
     * @param handler The REST handler
     */
    public ExtensionRouteHandler(
        String handlerName,
        RestRequest.Method method,
        String path,
        Function<RestRequest, ExtensionRestResponse> handler
    ) {
        super(ExtensionRouteHandlerFactory.getInstance().generateRouteName(handlerName), method, path, handler);
    }
}
