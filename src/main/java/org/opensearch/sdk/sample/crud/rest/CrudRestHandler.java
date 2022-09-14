/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.crud.rest;

import org.opensearch.common.path.PathTrie;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.rest.RestUtils;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.sdk.ExtensionRestResponse;
import org.opensearch.sdk.authz.ProtectedRoute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.opensearch.rest.RestRequest.Method.*;
import static org.opensearch.rest.RestStatus.*;


// TODO This class implements multiple Actions. Can we define one action at a time?

/**
 * Sample REST Handler (REST Action). Extension REST handlers must implement {@link ExtensionRestHandler}.
 */
public class CrudRestHandler implements ExtensionRestHandler {

    private static final String CREATE_SUCCESS = "PUT /create successful";
    private static final String UPDATE_SUCCESS = "POST /update successful";

    private PathTrie<List<Route>> pathTrie;

    public CrudRestHandler() {
        super();
        // initializes pathTrie
        routes();
    }

    @Override
    public List<Route> routes() {
        List<Route> routes = List.of(
                new ProtectedRoute(GET, "/detector", "ListDetector", (Method method, String uri) -> new ExtensionRestResponse(OK, CREATE_SUCCESS, List.of())),
                new ProtectedRoute(PUT, "/detector", "CreateDetector", new CrudCreateRestHandler()),
                new ProtectedRoute(POST, "/detector/{detector_id}", "UpdateDetector", new CrudUpdateRestHandler()),
                new ProtectedRoute(GET, "/detector/{detector_id}/results", "ListResults", (Method method, String uri) -> new ExtensionRestResponse(OK, CREATE_SUCCESS, List.of())),
                new ProtectedRoute(GET, "/detector/{detector_id}/results/{results_id}", "GetResults",(Method method, String uri) -> new ExtensionRestResponse(OK, CREATE_SUCCESS, List.of())),
                new ProtectedRoute(DELETE, "/detector/{detector_id}/results/{results_id}", "DeleteResults", (Method method, String uri) -> new ExtensionRestResponse(OK, CREATE_SUCCESS, List.of()))
        );
        // Only initialize this on first call
        if (pathTrie == null) {
            pathTrie = new PathTrie<>(RestUtils.REST_DECODER);
            for (Route r : routes) {
                List<Route> routesForPath = pathTrie.retrieve(r.getPath());
                if (routesForPath == null) {
                    routesForPath = new ArrayList<>();
                    routesForPath.add(r);
                    pathTrie.insert(r.getPath(), routesForPath);
                } else {
                    routesForPath.add(r);
                }
            }
        }
        return routes;
    }

    // How should the extension list what permissions it wants to create?
    // Will the permissions be part of the API spec of the extension?
    @Override
    public ExtensionRestResponse handleRequest(Method method, String uri) {
        if (pathTrie.retrieve(uri) == null || pathTrie.retrieve(uri).isEmpty()) {
            return new ExtensionRestResponse(
                NOT_FOUND,
                "Extension REST action improperly configured to handle " + method.name() + " " + uri,
                List.of()
            );
        }
        ProtectedRoute matchingRoute = getMatchingRoute(method, uri);
        if (matchingRoute != null) {
            return matchingRoute.handleRequest(method, uri);
        }
        return new ExtensionRestResponse(
                NOT_FOUND,
                "Extension REST action improperly configured to handle " + method.name() + " " + uri,
                List.of()
        );
    }

    public ProtectedRoute getMatchingRoute(Method method, String uri) {
        for (Route r : routes()) {
            ProtectedRoute protectedRoute = (ProtectedRoute) r;
            if (protectedRoute.getMethod() == method && uri.matches(protectedRoute.getRouteRegex().pattern())) {
                return protectedRoute;
            }
        }
        return null;
    }
}
