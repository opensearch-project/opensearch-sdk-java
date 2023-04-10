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
import java.util.List;

import org.opensearch.common.Nullable;
import org.opensearch.common.path.PathTrie;
import org.opensearch.rest.RestUtils;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.sdk.ExtensionRestHandler;

/**
 * This class registers REST paths from extension Rest Handlers.
 */
public class ExtensionRestPathRegistry {

    // PathTrie to match paths to handlers
    private PathTrie<SDKMethodHandlers> pathTrie = new PathTrie<>(RestUtils.REST_DECODER);
    // List to return registered handlers
    private List<String> registeredPaths = new ArrayList<>();

    /**
     * Register a REST handler to handle a method and route in this extension's path registry.
     *
     * @param method  The method to register.
     * @param path  The path to register. May include named wildcards.
     * @param extensionRestHandler  The RestHandler to handle this route
     */
    public void registerHandler(Method method, String path, ExtensionRestHandler extensionRestHandler) {
        pathTrie.insertOrUpdate(
            path,
            new SDKMethodHandlers(path, extensionRestHandler, method),
            (mHandlers, newMHandler) -> mHandlers.addMethods(extensionRestHandler, method)
        );
        registeredPaths.add(restPathToString(method, path));
    }

    /**
     * Register a REST handler to handle a method and route in this extension's path registry.
     *
     * @param method  The method to register.
     * @param path  The path to register. May include named wildcards.
     * @param name  An optional name of the REST handler
     * @param extensionRestHandler  The RestHandler to handle this route
     */
    public void registerHandler(Method method, String path, String name, ExtensionRestHandler extensionRestHandler) {
        pathTrie.insertOrUpdate(
                path,
                new SDKMethodHandlers(path, extensionRestHandler, method),
                (mHandlers, newMHandler) -> mHandlers.addMethods(extensionRestHandler, method)
        );
        String restPathWithName = restPathToString(method, path, name);
        registeredPaths.add(restPathWithName);
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

    /**
     * Converts a REST method and path to a space delimited string to be used as a map lookup key.
     *
     * @param method  the method.
     * @param path  the path.
     * @param name  the name.
     * @return A string appending the method and path.
     */
    public static String restPathToString(Method method, String path, String name) {
        return method.name() + " " + path + " " + name;
    }
}
