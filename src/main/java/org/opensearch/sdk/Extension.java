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
import java.util.Map;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.support.TransportAction;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.threadpool.ExecutorBuilder;
import org.opensearch.common.settings.Settings;

/**
 * This interface defines methods which an extension must provide. Extensions
 * will instantiate a class implementing this interface and pass it to the
 * {@link ExtensionsRunner} constructor to initialize.
 */
public interface Extension {

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
     * Gets an optional list of custom {@link NamedXContentRegistry.Entry} for the extension to combine with OpenSearch NamedXConent.
     *
     * @return a list of custom NamedXConent this extension uses.
     */
    default List<NamedXContentRegistry.Entry> getNamedXContent() {
        return Collections.emptyList();
    }

    /**
     * Returns components added by this extension.
     *
     * @return A collection of objects which will be bound to themselves for dependency injection.
     */
    default Collection<Object> createComponents() {
        return Collections.emptyList();
    }

    /**
     * Gets an optional list of custom {@link TransportAction} for the extension to register with OpenSearch.
     * <p>
     * TODO: ActionExtension#getActions will replace this: https://github.com/opensearch-project/opensearch-sdk-java/issues/368
     *
     * @return a list of custom transport actions this extension uses.
     */
    default Map<String, Class<? extends TransportAction<? extends ActionRequest, ? extends ActionResponse>>> getActionsMap() {
        return Collections.emptyMap();
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
