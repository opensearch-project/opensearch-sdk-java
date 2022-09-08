/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.crud.rest;

import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.sdk.ExtensionRestResponse;
import org.opensearch.sdk.authz.RequiresPermissions;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.opensearch.rest.RestRequest.Method.POST;
import static org.opensearch.rest.RestStatus.*;

/**
 * Sample REST Handler (REST Action). Extension REST handlers must implement {@link ExtensionRestHandler}.
 */
public class RestUpdateAction implements ExtensionRestHandler {

    private static final String SUCCESSFUL = "POST /update successful";

    @Override
    public List<Route> routes() {
        // TODO Accept URL args
        return singletonList(new Route(POST, "/crud/update"));
    }

    // How should the extension list what permissions it wants to create?
    // Will the permissions be part of the API spec of the extension?
    @RequiresPermissions(
            permissions = { "extensions:sample/crud/update" }
    )

    // TODO modify RestExecuteOnExtensionRequest to get request body. (Should it get params and headers too?)
    @Override
    public ExtensionRestResponse handleRequest(Method method, String uri) {
        List<String> consumedParams = new ArrayList<>();
        if (Method.POST.equals(method) && "/crud/update".equals(uri)) {
            return new ExtensionRestResponse(OK, SUCCESSFUL, consumedParams);
        }
        return new ExtensionRestResponse(
                NOT_FOUND,
                "Extension REST action improperly configured to handle " + method.name() + " " + uri,
                consumedParams
        );
    }

}
