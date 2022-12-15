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

import org.opensearch.cluster.service.ClusterService;
import org.opensearch.extensions.JobDetails;
import org.opensearch.threadpool.ThreadPool;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.support.TransportAction;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.xcontent.NamedXContentRegistry;

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
     * Gets a list of {@link ExtensionRestHandler} implementations this extension handles.
     *
     * @return a list of REST handlers (REST actions) this extension handles.
     */
    List<ExtensionRestHandler> getExtensionRestHandlers();

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
     * Set the Extension's instance of its corresponding {@link ExtensionsRunner}.
     *
     * @param extensionsRunner The ExtensionsRunner running this extension.
     */
    void setExtensionsRunner(ExtensionsRunner extensionsRunner);

    /**
     * Returns components added by this extension.
     *
     * @param client A client to make requests to the system
     * @param clusterService A service to allow watching and updating cluster state
     * @param threadPool A service to allow retrieving an executor to run an async action
     * @return A collection of objects
     */
    Collection<Object> createComponents(SDKClient client, ClusterService clusterService, ThreadPool threadPool);

    /**
     * Gets an optional list of custom {@link TransportAction} for the extension to register with OpenSearch.
     *
     * @return a list of custom transport actions this extension uses.
     */
    default Map<String, Class<? extends TransportAction<ActionRequest, ActionResponse>>> getActions() {
        return Collections.emptyMap();
    }

    JobDetails getJobDetails();
}
