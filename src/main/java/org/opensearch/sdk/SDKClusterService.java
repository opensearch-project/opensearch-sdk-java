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
import java.util.function.Consumer;

import org.opensearch.cluster.ClusterState;
import org.opensearch.common.settings.Setting;

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
        return extensionsRunner.sendClusterStateRequest(extensionsRunner.getExtensionTransportService());
    }

    public SDKClusterSettings getClusterSettings() {
        return clusterSettings;
    }

    /**
     * This class simulates methods normally called from OpenSearch ClusterSettings class.
     */
    public class SDKClusterSettings {

        /**
         * Add a single settings update consumer to OpenSearch
         * @param <T> The Type of the setting.
         *
         * @param setting The setting for which to consume updates.
         * @param consumer The consumer of the updates
         */
        public <T> void addSettingsUpdateConsumer(Setting<T> setting, Consumer<T> consumer) {
            addSettingsUpdateConsumer(Map.of(setting, consumer));
        }

        /**
         * Add multiple settings update consumers to OpenSearch
         *
         * @param settingUpdateConsumers A map of Setting to Consumer.
         */
        public void addSettingsUpdateConsumer(Map<Setting<?>, Consumer<?>> settingUpdateConsumers) {
            extensionsRunner.sendAddSettingsUpdateConsumerRequest(extensionsRunner.getExtensionTransportService(), settingUpdateConsumers);
        }
    }
}
