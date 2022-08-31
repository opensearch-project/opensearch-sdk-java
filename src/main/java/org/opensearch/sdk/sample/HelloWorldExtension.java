/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample;

import java.io.IOException;
import java.util.List;

import org.opensearch.sdk.Extension;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.sdk.ExtensionSettings;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.sample.rest.RestHelloAction;

/**
 * Sample class to demonstrate how to use the OpenSearch SDK for Java to create
 * an extension.
 * <p>
 * To create your own extension, implement the {@link #getExtensionSettings()} and {@link #getExtensionRestHandlers()} methods.
 * You may either create an {@link ExtensionSettings} object directly with the constructor, or read it from a YAML file on your class path.
 * <p>
 * To execute, pass an instatiated object of this class to {@link ExtensionsRunner#run(Extension)}.
 */
public class HelloWorldExtension implements Extension {

    /**
     * Optional classpath-relative path to a yml file containing extension settings.
     */
    private static final String EXTENSION_SETTINGS_PATH = "/sample/extension-settings.yml";

    /**
     * The extension settings include a name, host address, and port.
     */
    private ExtensionSettings settings;

    /**
     * Instantiate this extension, initializing the connection settings and REST actions.
     */
    public HelloWorldExtension() {
        try {
            this.settings = initializeSettings();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public ExtensionSettings getExtensionSettings() {
        return this.settings;
    }

    @Override
    public List<ExtensionRestHandler> getExtensionRestHandlers() {
        return List.of(new RestHelloAction());
    }

    /**
     * The Extension must provide its settings to the ExtensionsRunner.
     * These may be optionally read from a YAML file on the class path.
     * Or you may directly instantiate with the ExtensionSettings constructor.
     *
     * @return This extension's settings.
     * @throws IOException on failure to load settings.
     */
    private static ExtensionSettings initializeSettings() throws IOException {
        ExtensionSettings settings = Extension.readSettingsFromYaml(EXTENSION_SETTINGS_PATH);
        if (settings == null || settings.getHostAddress() == null || settings.getHostPort() == null) {
            throw new IOException("Failed to initialize Extension settings. No port bound.");
        }
        return settings;
    }

    /**
     * Entry point to execute an extension.
     *
     * @param args  Unused.
     * @throws IOException on a failure in the ExtensionsRunner
     */
    public static void main(String[] args) throws IOException {
        // Execute this extension by instantiating it and passing to ExtensionsRunner
        ExtensionsRunner.run(new HelloWorldExtension());
    }
}
