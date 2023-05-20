/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.opensearch.cluster.ClusterState;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.SettingUpgrader;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.AbstractScopedSettings;
import org.opensearch.common.settings.Setting.Property;
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
        // This will be empty on initialization, but updated later via apply()
        Settings nodeSettings = extensionsRunner.getEnvironmentSettings();
        Set<Setting<?>> settingsSet = new HashSet<>(extensionsRunner.getExtension().getSettings());
        this.clusterSettings = new SDKClusterSettings(nodeSettings, settingsSet);
    }

    /**
     * Send a request to OpenSearch to retrieve the cluster state
     *
     * @return the cluster state of OpenSearch
     */
    public ClusterState state() {
        if (extensionsRunner.isInitialized()) {
            return extensionsRunner.sendClusterStateRequest();
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

    /**
     * Updates cluster settings with current environment settings on the extensions runner.
     */
    public void updateSdkClusterSettings() {
        this.clusterSettings.applySettings(extensionsRunner.getEnvironmentSettings());
        extensionsRunner.getExtension().getSettings().stream().forEach(clusterSettings::registerSetting);
    }

    public SDKClusterSettings getClusterSettings() {
        return clusterSettings;
    }

    /**
     * This class simulates methods normally called from OpenSearch ClusterSettings class.
     */
    public class SDKClusterSettings extends AbstractScopedSettings {

        /**
         * Thread-safe map to hold pending updates until initialization completes
         */
        private final Map<Setting<?>, Consumer<?>> pendingSettingsUpdateConsumers = new ConcurrentHashMap<>();

        /**
         * Instantiate a new ClusterSettings instance.
         *
         * @param nodeSettings Environment settings associated with the node. Currently unused on extensions, provided for code compatibility.
         * @param settingsSet The extension's settings.
         */
        public SDKClusterSettings(final Settings nodeSettings, final Set<Setting<?>> settingsSet) {
            this(nodeSettings, settingsSet, Collections.emptySet());
        }

        /**
         * Instantiate a new ClusterSettings instance.
         *
         * @param nodeSettings Environment settings associated with the node. Currently unused on extensions, provided for code compatibility.
         * @param settingsSet The extension's settings.
         * @param settingUpgraders The extension's setting upgraders.
         */
        public SDKClusterSettings(
            final Settings nodeSettings,
            final Set<Setting<?>> settingsSet,
            final Set<SettingUpgrader<?>> settingUpgraders
        ) {
            super(nodeSettings, settingsSet, settingUpgraders, Property.NodeScope);
        }

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
         * If the ExtensionRunner has been initialized, send pending updates to OpenSearch, otherwise do nothing.
         * <p>
         * This method should be called from ExtensionsRunner after initialization, to clear the pending updates.
         */
        public synchronized void sendPendingSettingsUpdateConsumers() {
            // Do nothing until ExtensionsRunner initialized
            if (extensionsRunner.isInitialized() && !pendingSettingsUpdateConsumers.isEmpty()) {
                extensionsRunner.sendAddSettingsUpdateConsumerRequest(pendingSettingsUpdateConsumers);
                pendingSettingsUpdateConsumers.clear();
            }
        }
    }
}
