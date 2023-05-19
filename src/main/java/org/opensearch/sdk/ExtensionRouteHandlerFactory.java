/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.opensearch.OpenSearchException;

/**
 * A helper class to help form names for Extension REST Handlers. The convention for handler naming
 * of extension REST handlers is to use the shortName (abbreviation) of the extension followed by
 * a name for the REST handler
 *
 * i.e. For the HelloWorld extension, the name for /_extensions/_hw/hello is hw:greet
 */
public class ExtensionRouteHandlerFactory {
    private static ExtensionRouteHandlerFactory INSTANCE;

    private String extensionShortName;

    private ExtensionRouteHandlerFactory() {}

    /**
     * Gets an instance of this ExtensionRouteHandlerFactory
     * @return The instance of this ExtensionRouteHandlerFactory
     */
    public static ExtensionRouteHandlerFactory getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ExtensionRouteHandlerFactory();
        }

        return INSTANCE;
    }

    /**
     *
     * @return Indicates whether this class has previously been initialized
     */
    public boolean isInitialized() {
        return INSTANCE != null && this.extensionShortName != null;
    }

    /**
     * Initializes this ExtensionRouteHandlerFactory
     * @param extensionShortName The shortName for the extension
     */
    public void init(String extensionShortName) {
        if (this.extensionShortName != null) {
            throw new OpenSearchException("ExtensionRouteHandlerFactory was previously initialized");
        }
        this.extensionShortName = extensionShortName;
    }

    /**
     * Generates a name for the handler prepended with the extension's shortName
     * @param handlerName The human-readable name for a route registered by this extension
     * @return Returns a name prepended with the extension's shortName
     */
    public String generateRouteName(String handlerName) {
        return extensionShortName + ":" + handlerName;
    }
}
