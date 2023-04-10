/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.rest;

import org.opensearch.common.Nullable;
import org.opensearch.sdk.ExtensionRestHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.opensearch.rest.RestRequest.Method;

/**
 * Encapsulate multiple handlers for the same path, allowing different handlers for different HTTP verbs.
 * <p>
 * Used in SDK to provide identical path-matching functionality as OpenSearch, with Extension-based classes.
 */
final class SDKMethodHandlers {

    private final String path;
    private final Map<Method, ExtensionRestHandler> methodHandlers;

    SDKMethodHandlers(String path, ExtensionRestHandler handler, Method... methods) {
        this.path = path;
        this.methodHandlers = new HashMap<>(methods.length);
        for (Method method : methods) {
            methodHandlers.put(method, handler);
        }
    }

    /**
     * Add a handler for an additional array of methods. Note that {@code SDKMethodHandlers}
     * does not allow replacing the handler for an already existing method.
     */
    SDKMethodHandlers addMethods(ExtensionRestHandler handler, Method... methods) {
        for (Method method : methods) {
            ExtensionRestHandler existing = methodHandlers.putIfAbsent(method, handler);
            if (existing != null) {
                throw new IllegalArgumentException("Cannot replace existing handler for [" + path + "] for method: " + method);
            }
        }
        return this;
    }

    /**
     * Returns the handler for the given method or {@code null} if none exists.
     */
    @Nullable
    ExtensionRestHandler getHandler(Method method) {
        return methodHandlers.get(method);
    }

    /**
     * Return a set of all valid HTTP methods for the particular path
     */
    Set<Method> getValidMethods() {
        return methodHandlers.keySet();
    }
}
