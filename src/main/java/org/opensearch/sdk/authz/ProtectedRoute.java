package org.opensearch.sdk.authz;

import org.opensearch.rest.RestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.sdk.ExtensionRestResponse;
import org.opensearch.sdk.handlers.ExtensionRouteRequestHandler;

import java.util.regex.Pattern;

public class ProtectedRoute extends RestHandler.Route {

    public ExtensionRouteRequestHandler requestHandler;
    public ProtectedRoute(RestRequest.Method method, String path, ExtensionRouteRequestHandler requestHandler) {
        super(method, path);
        this.requestHandler = requestHandler;
    }

    public Pattern getRouteRegex() {
        if (getPath() == null) {
            return null;
        }
        String routeRegex = getPath().replaceAll("\\{.*?\\}", ".*");
        Pattern routePattern = Pattern.compile(routeRegex);
        return routePattern;
    }

    public ExtensionRestResponse handleRequest(RestRequest.Method method, String uri) {
        return this.requestHandler.handleRequest(method, uri);
    }
}