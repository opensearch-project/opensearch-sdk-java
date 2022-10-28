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
import java.util.Collection;
import java.util.Collections;

import org.opensearch.cluster.service.ClusterService;
import org.opensearch.threadpool.ThreadPool;

/**
 * An abstract class that provides sample methods required by extensions
 */
public abstract class BaseExtension implements Extension {
    /**
     * A client to make requests to the system
     */
    protected SDKClient client;

    /**
     * A service to allow watching and updating cluster state
     */
    protected ClusterService clusterService;

    /**
     * A service to allow retrieving an executor to run an async action
     */
    protected ThreadPool threadPool;

    /**
     * Optional classpath-relative path to a yml file containing extension settings.
     */
    protected static final String EXTENSION_SETTINGS_PATH = "/sample/helloworld-settings.yml";

    /**
     * The extension settings include a name, host address, and port.
     */
    protected ExtensionSettings settings;

    /**
     * Instantiate this extension, initializing the connection settings and REST actions.
     */
    protected BaseExtension() {
        try {
            this.settings = initializeSettings();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * The Extension must provide its settings to the ExtensionsRunner.
     * These may be optionally read from a YAML file on the class path.
     * Or you may directly instantiate with the ExtensionSettings constructor.
     *
     * @return This extension's settings.
     * @throws IOException on failure to load settings.
     */
    protected static ExtensionSettings initializeSettings() throws IOException {
        ExtensionSettings settings = Extension.readSettingsFromYaml(EXTENSION_SETTINGS_PATH);
        if (settings == null || settings.getHostAddress() == null || settings.getHostPort() == null) {
            throw new IOException("Failed to initialize Extension settings. No port bound.");
        }
        return settings;
    }

    /**
     * Returns components added by this extension.
     *
     * @param client A client to make requests to the system
     * @param clusterService A service to allow watching and updating cluster state
     * @param threadPool A service to allow retrieving an executor to run an async action
     * @return A collection of objects
     */
    public Collection<Object> createComponents(SDKClient client, ClusterService clusterService, ThreadPool threadPool) {
        this.client = client;
        this.clusterService = clusterService;
        this.threadPool = threadPool;

        return Collections.emptyList();
    }
}
