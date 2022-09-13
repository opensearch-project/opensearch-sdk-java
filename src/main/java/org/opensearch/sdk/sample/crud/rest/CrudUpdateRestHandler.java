package org.opensearch.sdk.sample.crud.rest;

import org.opensearch.rest.RestRequest;
import org.opensearch.sdk.ExtensionRestResponse;
import org.opensearch.sdk.handlers.ExtensionRouteRequestHandler;

import java.util.ArrayList;
import java.util.List;

import static org.opensearch.rest.RestStatus.OK;

public class CrudUpdateRestHandler implements ExtensionRouteRequestHandler {

    private static final String UPDATE_SUCCESS = "POST /update successful";

    @Override
    public ExtensionRestResponse handleRequest(RestRequest.Method method, String uri) {
        System.out.println("CrudUpdateRestHandler.handleRequest");
        List<String> consumedParams = new ArrayList<>();
        consumedParams.add("detector_id");
        return new ExtensionRestResponse(OK, UPDATE_SUCCESS, consumedParams);
    }
}
