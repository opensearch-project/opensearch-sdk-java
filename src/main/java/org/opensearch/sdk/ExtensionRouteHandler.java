package org.opensearch.sdk;

import org.opensearch.rest.RestRequest;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.extensions.rest.RouteHandler;

import java.util.function.Function;

public class ExtensionRouteHandler extends RouteHandler {

    public ExtensionRouteHandler(String extensionShortName, String handlerName, RestRequest.Method method, String path, Function<ExtensionRestRequest, ExtensionRestResponse> handler) {
        super("extension:" + extensionShortName + "/" + handlerName, method, path, handler);
    }
}
