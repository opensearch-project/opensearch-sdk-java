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
import java.util.Collection;
import java.util.List;

import org.opensearch.core.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.settings.Setting;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.threadpool.ExecutorBuilder;
import org.opensearch.common.settings.Settings;

/**
 * This interface defines methods which an extension must provide. Extensions
 * will instantiate a class implementing this interface and pass it to the
 * {@link ExtensionsRunner} constructor to initialize.
 */
public interface Extension {

    /**
     * Set the instance of {@link ExtensionsRunner} for this extension.
     *
     * @param runner The ExtensionsRunner instance.
     */
    public void setExtensionsRunner(ExtensionsRunner runner);

    /**
     * Gets the {@link ExtensionSettings} of this extension.
     *
     * @return the extension settings.
     */
    ExtensionSettings getExtensionSettings();

    /**
     * Gets an optional list of custom {@link Setting} for the extension to register with OpenSearch.
     *
     * @return a list of custom settings this extension uses.
     */
    default List<Setting<?>> getSettings() {
        return Collections.emptyList();
    }

    /**
     * Gets an optional list of custom {@link NamedXContentRegistry.Entry} for the extension to combine with OpenSearch NamedWriteable.
     *
     * @return a list of custom NamedXConent this extension uses.
     */
    default List<NamedXContentRegistry.Entry> getNamedXContent() {
        return Collections.emptyList();
    }

    /**
     * Gets an optional list of custom {@link NamedWriteableRegistry.Entry} for the extension to combine with OpenSearch NamedXWriteable.
     *
     * @return a list of custom NamedWriteable this extension uses.
     */
    default List<NamedWriteableRegistry.Entry> getNamedWriteables() {
        return Collections.emptyList();
    }

    /**
     * Returns components added by this extension.
     *
     * @param runner the ExtensionsRunner instance. Use getters from this object as required to instantiate components to return.
     * @return A collection of objects which will be bound to themselves for dependency injection.
     */
    default Collection<Object> createComponents(ExtensionsRunner runner) {
        return Collections.emptyList();
    }

    /**
     * Provides the list of this Extension's custom thread pools, empty if
     * none.
     *
     * @param settings the current settings
     * @return executors builders for this Extension's custom thread pools
     */
    default List<ExecutorBuilder<?>> getExecutorBuilders(Settings settings) {
        return Collections.emptyList();
    }
}
