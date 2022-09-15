package org.opensearch.sdk.authz;

import org.opensearch.rest.RestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.sdk.ExtensionRestResponse;
import org.opensearch.sdk.handlers.ExtensionRouteRequestHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProtectedRoute extends RestHandler.Route {

    private static final Pattern ROUTE_PARAM_REGEX = Pattern.compile("\\{(.*?)\\}");

    public ExtensionRouteRequestHandler requestHandler;

    private String requiredPermission;
    public ProtectedRoute(RestRequest.Method method, String path, String requiredPermission, ExtensionRouteRequestHandler requestHandler) {
        super(method, path);
        this.requestHandler = requestHandler;
        this.requiredPermission = requiredPermission;
    }

    public String getRequiredPermission() {
        return this.requiredPermission;
    }

    public Pattern getRouteRegex() {
        if (getPath() == null) {
            return null;
        }
        String routeRegex = getPath().replaceAll("\\{.*?\\}", "(.*)");
        Pattern routePattern = Pattern.compile(routeRegex);
        return routePattern;
    }

    public Map<String, Object> getConsumedParams(String uri) {
        // Extracting only route and URI params for now. Needs to extract params from the body when that is available
        Map<String, Object> consumedParams = new HashMap<>();
        Matcher nameMatcher = ROUTE_PARAM_REGEX.matcher(getPath());
        List<String> paramNames = new ArrayList<>();
        List<String> paramValues = new ArrayList<>();
        while (nameMatcher.find()) {
            for (int i = 0; i < nameMatcher.groupCount(); i++) {
                paramNames.add(nameMatcher.group(i+1));
            }
        }
        Matcher matcher = getRouteRegex().matcher(uri);
        while (matcher.find()) {
            for (int i = 0; i < matcher.groupCount(); i++) {
                paramValues.add(matcher.group(i+1));
            }
        }
        if (paramNames.size() != paramValues.size()) {
            // Failed to parse, how to respond?
            return consumedParams;
        }
        for (int i = 0; i < paramNames.size(); i++) {
            consumedParams.put(paramNames.get(i), paramValues.get(i));
        }
        return consumedParams;
    }

    public ExtensionRestResponse handleRequest(RestRequest.Method method, String uri) {
        return this.requestHandler.handleRequest(method, uri);
    }
}
