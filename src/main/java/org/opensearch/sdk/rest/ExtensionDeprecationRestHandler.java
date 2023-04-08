/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.rest;

import java.util.Objects;

import org.opensearch.common.logging.DeprecationLogger;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.DeprecationRestHandler;
import org.opensearch.rest.RestRequest;

/**
 * {@code ExtensionDeprecationRestHandler} provides a proxy for any existing {@link Extension RestHandler} so that usage of the handler can be logged using the {@link DeprecationLogger}.
 */
public class ExtensionDeprecationRestHandler implements ExtensionRestHandler {

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
    public ExtensionDeprecationRestHandler(ExtensionRestHandler handler, String deprecationMessage, DeprecationLogger deprecationLogger) {
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
