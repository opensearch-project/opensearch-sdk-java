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
     * The {@link ExtensionsRunner} instance running this extension
     */
    protected ExtensionsRunner extensionsRunner;

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
     * The extension settings include a name, host address, and port.
     */
    private final ExtensionSettings settings;

    /**
     * Instantiate this extension, initializing the connection settings and REST actions.
     */
    protected BaseExtension(String path) {
        try {
            this.settings = ExtensionSettings.readSettingsFromYaml(path);
            if (settings == null || settings.getHostAddress() == null || settings.getHostPort() == null) {
                throw new IOException("Failed to initialize Extension settings. No port bound.");
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    /**
     * take an ExtensionSettings object and set it directly
     */
    protected BaseExtension(ExtensionSettings settings) {
        this.settings = settings;
    }

    @Override
    public ExtensionSettings getExtensionSettings() {
        return this.settings;
    }

    @Override
    public void setExtensionsRunner(ExtensionsRunner extensionsRunner) {
        this.extensionsRunner = extensionsRunner;
    }

    @Override
    public Collection<Object> createComponents(SDKClient client, ClusterService clusterService, ThreadPool threadPool) {
        this.client = client;
        this.clusterService = clusterService;
        this.threadPool = threadPool;

        return Collections.emptyList();
    }
}
