package org.opensearch.sdk.handlers;

import org.opensearch.rest.RestRequest;
import org.opensearch.sdk.ExtensionRestResponse;
@FunctionalInterface
public interface ExtensionRouteRequestHandler {
    ExtensionRestResponse handleRequest(RestRequest.Method method, String uri);
}
