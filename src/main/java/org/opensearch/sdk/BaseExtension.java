/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.io.IOException;

import com.google.inject.Inject;

/**
 * An abstract class that simplifies extension initialization and provides an instance of the runner.
 */
public abstract class BaseExtension implements Extension, ActionExtension {
    /**
     * The {@link ExtensionsRunner} instance running this extension
     */
    @Inject
    private ExtensionsRunner extensionsRunner;

    /**
     * The extension settings include a name, host address, and port.
     */
    private final ExtensionSettings settings;

    /**
     * Instantiate this extension, initializing the connection settings and REST actions.
     * @param path to extensions configuration.
     */
    protected BaseExtension(String path) {
        try {
            this.settings = ExtensionSettings.readSettingsFromYaml(path);
            if (settings == null || settings.getHostAddress() == null || settings.getHostPort() == null) {
                throw new IOException("Failed to initialize Extension settings. No port bound.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * take an ExtensionSettings object and set it directly.
     * @param settings defined by the extension.
     */
    protected BaseExtension(ExtensionSettings settings) {
        this.settings = settings;
    }

    @Override
    public ExtensionSettings getExtensionSettings() {
        return this.settings;
    }

    /**
     * Gets the {@link ExtensionsRunner} of this extension.
     *
     * @return the extension runner.
     */
    public ExtensionsRunner extensionsRunner() {
        return this.extensionsRunner;
    }
}
