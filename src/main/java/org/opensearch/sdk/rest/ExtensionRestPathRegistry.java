/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opensearch.common.Nullable;
import org.opensearch.common.logging.DeprecationLogger;
import org.opensearch.common.path.PathTrie;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestUtils;
import org.opensearch.rest.RestRequest.Method;

/**
 * This class registers REST paths from extension Rest Handlers.
 */
public class ExtensionRestPathRegistry {

    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(ExtensionRestPathRegistry.class);

    // PathTrie to match paths to handlers
    private PathTrie<SDKMethodHandlers> pathTrie = new PathTrie<>(RestUtils.REST_DECODER);
    // Lists to return registered handlers
    private List<String> registeredPaths = new ArrayList<>();
    private List<String> registeredDeprecatedPaths = new ArrayList<>();

    /**
     * Registers a REST handler with the controller. The REST handler declares the {@code method} and {@code path} combinations.
     *
     * @param restHandler The RestHandler to register routes.
     */
    public void registerHandler(ExtensionRestHandler restHandler) {
        restHandler.routes().forEach(route -> registerHandler(route.getMethod(), route.getPath(), restHandler));
        restHandler.deprecatedRoutes()
            .forEach(route -> registerAsDeprecatedHandler(route.getMethod(), route.getPath(), restHandler, route.getDeprecationMessage()));
    }

    /**
     * Registers a REST handler to be executed when one of the provided methods and path match the request.
     *
     * @param path Path to handle (e.g., "/{index}/{type}/_bulk")
     * @param handler The handler to actually execute
     * @param method GET, POST, etc.
     */
    private void registerHandler(Method method, String path, ExtensionRestHandler extensionRestHandler) {
        pathTrie.insertOrUpdate(
            path,
            new SDKMethodHandlers(path, extensionRestHandler, method),
            (mHandlers, newMHandler) -> mHandlers.addMethods(extensionRestHandler, method)
        );
        registeredPaths.add(restPathToString(method, path));
    }

    /**
     * Registers a REST handler to be executed when the provided {@code method} and {@code path} match the request.
     *
     * @param method GET, POST, etc.
     * @param path Path to handle (e.g., "/{index}/{type}/_bulk")
     * @param handler The handler to actually execute
     * @param deprecationMessage The message to log and send as a header in the response
     */
    private void registerAsDeprecatedHandler(
        RestRequest.Method method,
        String path,
        ExtensionRestHandler handler,
        String deprecationMessage
    ) {
        assert (handler instanceof ExtensionDeprecationRestHandler) == false;

        registerHandler(method, path, new ExtensionDeprecationRestHandler(handler, deprecationMessage, deprecationLogger));
        registeredDeprecatedPaths.addAll(Arrays.asList(restPathToString(method, path), deprecationMessage));
    }

    /**
     * Get the registered REST handler for the specified method and path.
     *
     * @param method  the registered method.
     * @param path  the registered path.
     * @return The REST handler registered to handle this method and path combination if found, null otherwise.
     */
    @Nullable
    public ExtensionRestHandler getHandler(Method method, String path) {
        SDKMethodHandlers mHandlers = pathTrie.retrieve(path);
        return mHandlers == null ? null : mHandlers.getHandler(method);
    }

    /**
     * List the registered routes.
     *
     * @return A list of strings identifying the registered routes.
     */
    public List<String> getRegisteredPaths() {
        return registeredPaths;
    }

    /**
     * List the registered deprecated routes.
     *
     * @return A list of strings, in pairs. The first of each pair identifies the registered routes, the second is a deprecation message.
     */
    public List<String> getRegisteredDeprecatedPaths() {
        return registeredDeprecatedPaths;
    }

    /**
     * Converts a REST method and path to a space delimited string.
     * <p>
     * This provides convenience for logging and serialization over transport.
     *
     * @param method  the method.
     * @param path  the path.
     * @return A string appending the method and path.
     */
    public static String restPathToString(Method method, String path) {
        return method.name() + " " + path;
    }
}
