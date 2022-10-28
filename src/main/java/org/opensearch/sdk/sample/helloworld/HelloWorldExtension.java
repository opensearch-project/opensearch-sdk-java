/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld;

import java.io.IOException;
import java.util.List;

import org.opensearch.sdk.BaseExtension;
import org.opensearch.sdk.Extension;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.sdk.ExtensionSettings;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.sample.helloworld.rest.RestHelloAction;

/**
 * Sample class to demonstrate how to use the OpenSearch SDK for Java to create
 * an extension.
 * <p>
 * To create your own extension, implement the {@link #getExtensionSettings()} and {@link #getExtensionRestHandlers()} methods.
 * You may either create an {@link ExtensionSettings} object directly with the constructor, or read it from a YAML file on your class path.
 * <p>
 * To execute, pass an instatiated object of this class to {@link ExtensionsRunner#run(Extension)}.
 */
public class HelloWorldExtension extends BaseExtension {

    /**
     * Instantiate this extension, initializing the connection settings and REST actions.
     */
    public HelloWorldExtension() {
        super();

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
