/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.crud.rest;

import org.opensearch.action.ActionType;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.sdk.ExtensionRestResponse;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.opensearch.rest.RestRequest.Method.POST;
import static org.opensearch.rest.RestRequest.Method.PUT;
import static org.opensearch.rest.RestStatus.*;


// TODO This class implements multiple Actions. Can we define one action at a time?

/**
 * Sample REST Handler (REST Action). Extension REST handlers must implement {@link ExtensionRestHandler}.
 */
public class CrudRestHandler extends ExtensionRestHandler {

    private static final String CREATE_SUCCESS = "PUT /create successful";
    private static final String UPDATE_SUCCESS = "POST /update successful";

    public CrudRestHandler(String extensionId) {
        super(extensionId);
    }

    @Override
    public List<Route> routes() {
        return List.of(new Route(PUT, "/crud/create"), new Route(POST, "/crud/update"));
    }

    // How should the extension list what permissions it wants to create?
    // Will the permissions be part of the API spec of the extension?
    @Override
    public ExtensionRestResponse handleRequest(Method method, String uri) {
        System.out.println("method: " + method);
        System.out.println("URI: " + uri);
        // TODO modify RestExecuteOnExtensionRequest to get request body. (Should it get params and headers too?)
        List<String> consumedParams = new ArrayList<>();
        if (Method.PUT.equals(method) && "/crud/create".equals(uri)) {
            return new ExtensionRestResponse(OK, CREATE_SUCCESS, consumedParams);
        } else if (Method.POST.equals(method) && "/crud/update".equals(uri)) {
            return new ExtensionRestResponse(OK, UPDATE_SUCCESS, consumedParams);
        }
        return new ExtensionRestResponse(
                NOT_FOUND,
                "Extension REST action improperly configured to handle " + method.name() + " " + uri,
                consumedParams
        );
    }

}
