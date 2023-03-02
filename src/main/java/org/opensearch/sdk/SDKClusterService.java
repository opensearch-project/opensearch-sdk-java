/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.opensearch.cluster.ClusterState;
import org.opensearch.common.settings.Setting;
import org.opensearch.extensions.DiscoveryExtensionNode;

/**
 * This class simulates methods normally called from OpenSearch ClusterService class.
 */
public class SDKClusterService {

    private final ExtensionsRunner extensionsRunner;
    private final SDKClusterSettings clusterSettings;

    /**
     * Create an instance of this object.
     *
     * @param extensionsRunner An {@link ExtensionsRunner} instance.
     */
    public SDKClusterService(ExtensionsRunner extensionsRunner) {
        this.extensionsRunner = extensionsRunner;
        this.clusterSettings = new SDKClusterSettings();
    }

    /**
     * Send a request to OpenSearch to retrieve the cluster state
     *
     * @return the cluster state of OpenSearch
     */
    public ClusterState state() {
        if (extensionsRunner.isInitialized()) {
            return extensionsRunner.sendClusterStateRequest(extensionsRunner.getExtensionTransportService());
        }
        throw new IllegalStateException("The Extensions Runner has not been initialized.");
    }

    /**
     *  Returns the local extension node
     *
     * @return the local extension node
     */
    public DiscoveryExtensionNode localNode() {
        return extensionsRunner.getExtensionNode();
    }

    public SDKClusterSettings getClusterSettings() {
        return clusterSettings;
    }

    /**
     * This class simulates methods normally called from OpenSearch ClusterSettings class.
     */
    public class SDKClusterSettings {

        /**
         * Thread-safe map to hold pending updates until initialization completes
         */
        private Map<Setting<?>, Consumer<?>> pendingSettingsUpdateConsumers = new ConcurrentHashMap<>();

        /**
         * Add a single settings update consumer to OpenSearch. Before initialization the update will be stored in a pending state.
         *
         * @param <T> The Type of the setting.
         * @param setting The setting for which to consume updates.
         * @param settingsUpdateConsumer The consumer of the updates.
         */
        public synchronized <T> void addSettingsUpdateConsumer(Setting<T> setting, Consumer<T> settingsUpdateConsumer) {
            pendingSettingsUpdateConsumers.put(setting, settingsUpdateConsumer);
            sendPendingSettingsUpdateConsumers();
        }

        /**
         * Add multiple settings update consumers to OpenSearch. Before initialization the updates will be stored in a pending state.
         *
         * @param settingsUpdateConsumers A map of Setting to update Consumer.
         */
        public synchronized void addSettingsUpdateConsumer(Map<Setting<?>, Consumer<?>> settingsUpdateConsumers) {
            settingsUpdateConsumers.entrySet().stream().forEach(e -> pendingSettingsUpdateConsumers.put(e.getKey(), e.getValue()));
            sendPendingSettingsUpdateConsumers();
        }

        /**
         * If the ExtensionRunner has been initialized, send pending updates to OpenSearch, otherwise do nothing.
         * <p>
         * This method should be called from ExtensionsRunner after initialization, to clear the pending updates.
         */
        public synchronized void sendPendingSettingsUpdateConsumers() {
            // Do nothing until ExtensionsRunner initialized
            if (extensionsRunner.isInitialized() && !pendingSettingsUpdateConsumers.isEmpty()) {
                extensionsRunner.sendAddSettingsUpdateConsumerRequest(
                    extensionsRunner.getExtensionTransportService(),
                    pendingSettingsUpdateConsumers
                );
                pendingSettingsUpdateConsumers.clear();
            }
        }
    }
}
