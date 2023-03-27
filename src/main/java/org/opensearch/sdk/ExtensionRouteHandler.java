package org.opensearch.sdk;

import org.opensearch.rest.RestRequest;
import org.opensearch.rest.extensions.ExtensionRestRequest;
import org.opensearch.rest.extensions.ExtensionRestResponse;
import org.opensearch.rest.extensions.RouteHandler;

import java.util.function.Function;

public class ExtensionRouteHandler extends RouteHandler {

    public ExtensionRouteHandler(String extensionUniqueId, String handlerName, RestRequest.Method method, String path, Function<ExtensionRestRequest, ExtensionRestResponse> handler) {
        super("extension:" + extensionUniqueId + "/" + handlerName, method, path, handler);
    }
}
