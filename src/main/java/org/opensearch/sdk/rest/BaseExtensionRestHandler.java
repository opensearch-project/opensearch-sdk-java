/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.rest;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.opensearch.rest.RestStatus.INTERNAL_SERVER_ERROR;
import static org.opensearch.rest.RestStatus.NOT_FOUND;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;
import org.opensearch.common.logging.DeprecationLogger;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.DeprecationRestHandler;
import org.opensearch.rest.NamedRoute;
import org.opensearch.rest.RestHandler.DeprecatedRoute;
import org.opensearch.rest.RestHandler.ReplacedRoute;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestStatus;

/**
 * Provides convenience methods to reduce boilerplate code in an {@link ExtensionRestHandler} implementation.
 */
public abstract class BaseExtensionRestHandler implements ExtensionRestHandler {

    private String extensionName;

    /**
     * Constant for JSON content type
     */
    public static final String JSON_CONTENT_TYPE = APPLICATION_JSON.withCharset(StandardCharsets.UTF_8).toString();

    @Override
    public List<NamedRoute> routes() {
        return Collections.emptyList();
    }

    /**
     * Defines a list of methods which handle each rest {@link DeprecatedRoute}. Override this in a subclass to use the functional syntax.
     *
     * @return a list of {@link DeprecatedRouteHandler} with corresponding methods to each route.
     */
    protected List<DeprecatedRouteHandler> deprecatedRouteHandlers() {
        return Collections.emptyList();
    }

    @Override
    public List<DeprecatedRoute> deprecatedRoutes() {
        return List.copyOf(deprecatedRouteHandlers());
    }

    /**
     * Defines a list of methods which handle each rest {@link ReplacedRoute}. Override this in a subclass to use the functional syntax.
     *
     * @return a list of {@link ReplacedRouteHandler} with corresponding methods to each route.
     */
    protected List<ReplacedRouteHandler> replacedRouteHandlers() {
        return Collections.emptyList();
    }

    public void setExtensionName(String extensionName) {
        this.extensionName = extensionName;
    }

    /**
     * Generates a name for the handler prepended with the extension's name
     * @param name The human-readable name for a route registered by this extension
     * @return Returns a name prepended with the extension's name
     */
    protected String routePrefix(String name) {
        return extensionName + ":" + name;
    }

    @Override
    public List<ReplacedRoute> replacedRoutes() {
        return List.copyOf(replacedRouteHandlers());
    }

    @Override
    public ExtensionRestResponse handleRequest(RestRequest request) {
        Optional<NamedRoute> route = routes().stream()
            .filter(rh -> rh.getMethod().equals(request.method()))
            .filter(rh -> restPathMatches(request.path(), rh.getPath()))
            .findFirst();
        if (route.isPresent() && route.get().handler() != null) {
            return (ExtensionRestResponse) route.get().handler().apply(request);
        }
        Optional<DeprecatedRouteHandler> deprecatedHandler = deprecatedRouteHandlers().stream()
            .filter(rh -> rh.getMethod().equals(request.method()))
            .filter(rh -> restPathMatches(request.path(), rh.getPath()))
            .findFirst();
        if (deprecatedHandler.isPresent()) {
            return deprecatedHandler.get().handleRequest(request);
        }
        Optional<ReplacedRouteHandler> replacedHandler = replacedRouteHandlers().stream()
            .filter(rh -> rh.getMethod().equals(request.method()))
            .filter(rh -> restPathMatches(request.path(), rh.getPath()))
            .findFirst();
        if (replacedHandler.isPresent()) {
            return replacedHandler.get().handleRequest(request);
        }
        replacedHandler = replacedRouteHandlers().stream()
            .filter(rh -> rh.getDeprecatedMethod().equals(request.method()))
            .filter(rh -> restPathMatches(request.path(), rh.getDeprecatedPath()))
            .findFirst();
        if (replacedHandler.isPresent()) {
            return replacedHandler.get().handleRequest(request);
        }
        return unhandledRequest(request);
    }

    /**
     * Determines if the request's path is a match for the configured handler path.
     *
     * @param requestPath The path from the {@link RestRequest}
     * @param handlerPath The path from the {@link NamedRoute} or {@link DeprecatedRouteHandler} or {@link ReplacedRouteHandler}
     * @return true if the request path matches the route
     */
    private boolean restPathMatches(String requestPath, String handlerPath) {
        // Check exact match
        if (handlerPath.equals(requestPath)) {
            return true;
        }
        // Split path to evaluate named params
        String[] handlerSplit = handlerPath.split("/");
        String[] requestSplit = requestPath.split("/");
        if (handlerSplit.length != requestSplit.length) {
            return false;
        }
        for (int i = 0; i < handlerSplit.length; i++) {
            if (!(handlerSplit[i].equals(requestSplit[i]) || (handlerSplit[i].startsWith("{") && handlerSplit[i].endsWith("}")))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a default response when a request does not match the handled methods or paths. This can occur if a
     * handler indicates routes that it handles but does not actually handle them.
     *
     * @param request The request that couldn't be handled.
     * @return an ExtensionRestResponse identifying the unhandled request.
     */
    protected ExtensionRestResponse unhandledRequest(RestRequest request) {
        return createJsonResponse(
            request,
            NOT_FOUND,
            "error",
            "Extension REST action improperly configured to handle: [" + request.method() + " " + request.uri() + "]"
        );
    }

    /**
     * Returns a default response when a request handler throws an exception.
     *
     * @param request The request that caused the exception
     * @param e The exception
     * @return an ExtensionRestResponse identifying the exception
     */
    protected ExtensionRestResponse exceptionalRequest(RestRequest request, Exception e) {
        return createJsonResponse(request, INTERNAL_SERVER_ERROR, "error", "Request failed with exception: [" + e.getMessage() + "]");
    }

    /**
     * Returns a JSON-formatted response for a given string.
     *
     * @param request The request to respond to
     * @param status The response status to send
     * @param fieldName The field name for the response string
     * @param responseStr The string to include
     * @return an ExtensionRestResponse in JSON format including the specified string as the content of the specified field
     */
    protected ExtensionRestResponse createJsonResponse(RestRequest request, RestStatus status, String fieldName, String responseStr) {
        try {
            return new ExtensionRestResponse(
                request,
                status,
                JsonXContent.contentBuilder().startObject().field(fieldName, responseStr).endObject()
            );
        } catch (IOException e) {
            // This Should Never Happen (TM)
            // If we messed up JSON code above, just send plain text
            return new ExtensionRestResponse(request, status, fieldName + ": " + responseStr);
        }
    }

    /**
     * Returns a String message of the detail of any unrecognized error occurred. The string is intended for use in error messages to be returned to the user.
     *
     * @param request The request that caused the exception
     * @param invalids Strings from the request which were unable to be understood.
     * @param candidates A set of words that are most likely to be the valid strings determined invalid, to be suggested to the user.
     * @param detail The parameter contains the details of the exception.
     * @return a String that contains the message.
     */
    protected final String unrecognized(
        final RestRequest request,
        final Set<String> invalids,
        final Set<String> candidates,
        final String detail
    ) {
        return BaseRestHandler.unrecognizedStrings(request, invalids, candidates, detail);
    }

    /**
    * Creates a new plain text response with OK status and empty JSON content
    *
    * @param request the REST request being responded to
     * @param status the REST response status
    * @return ExtensionRestResponse with OK status response
    */
    protected ExtensionRestResponse createEmptyJsonResponse(RestRequest request, RestStatus status) {
        return new ExtensionRestResponse(request, status, JSON_CONTENT_TYPE, "{}");
    }

    /**
     * A subclass of {@link DeprecatedRoute} that includes a handler method for that route.
     */
    public static class DeprecatedRouteHandler extends DeprecatedRoute {

        private final Function<RestRequest, ExtensionRestResponse> responseHandler;

        /**
         * Handle the method and path with the specified handler.
         *
         * @param method The {@link Method} to handle.
         * @param path The path to handle.
         * @param deprecationMessage The message to log with the deprecation logger
         * @param handler The method which handles the method and path.
         */
        public DeprecatedRouteHandler(
            Method method,
            String path,
            String deprecationMessage,
            Function<RestRequest, ExtensionRestResponse> handler
        ) {
            super(method, path, deprecationMessage);
            this.responseHandler = handler;
        }

        /**
         * Executes the handler for this route.
         *
         * @param request The request to handle
         * @return the {@link ExtensionRestResponse} result from the handler for this route.
         */
        public ExtensionRestResponse handleRequest(RestRequest request) {
            return responseHandler.apply(request);
        }
    }

    /**
     * {@code ExtensionDeprecationRestHandler} provides a proxy for any existing {@link ExtensionRestHandler} so that usage of the handler can be logged using the {@link DeprecationLogger}.
     */
    public static class ExtensionDeprecationRestHandler implements ExtensionRestHandler {

        private final ExtensionRestHandler handler;
        private final String deprecationMessage;
        private final DeprecationLogger deprecationLogger;

        /**
         * Create a {@link DeprecationRestHandler} that encapsulates the {@code handler} using the {@code deprecationLogger} to log deprecation {@code warning}.
         *
         * @param handler The rest handler to deprecate (it's possible that the handler is reused with a different name!)
         * @param deprecationMessage The message to warn users with when they use the {@code handler}
         * @param deprecationLogger The deprecation logger
         * @throws NullPointerException if any parameter except {@code deprecationMessage} is {@code null}
         * @throws IllegalArgumentException if {@code deprecationMessage} is not a valid header
         */
        public ExtensionDeprecationRestHandler(
            ExtensionRestHandler handler,
            String deprecationMessage,
            DeprecationLogger deprecationLogger
        ) {
            this.handler = Objects.requireNonNull(handler);
            this.deprecationMessage = DeprecationRestHandler.requireValidHeader(deprecationMessage);
            this.deprecationLogger = Objects.requireNonNull(deprecationLogger);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Usage is logged via the {@link DeprecationLogger} so that the actual response can be notified of deprecation as well.
         */
        @Override
        public ExtensionRestResponse handleRequest(RestRequest restRequest) {
            deprecationLogger.deprecate("deprecated_route", deprecationMessage);

            return handler.handleRequest(restRequest);
        }

        ExtensionRestHandler getHandler() {
            return handler;
        }

        String getDeprecationMessage() {
            return deprecationMessage;
        }
    }
}
