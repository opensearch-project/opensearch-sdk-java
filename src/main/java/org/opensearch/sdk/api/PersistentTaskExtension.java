/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.api;

import org.opensearch.client.Client;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.SettingsModule;
import org.opensearch.persistent.PersistentTasksExecutor;
import org.opensearch.threadpool.ThreadPool;

import java.util.Collections;
import java.util.List;

/**
 * Extension for registering persistent tasks executors.
 */

public interface PersistentTaskExtension {

    /**
     * Returns additional persistent tasks executors added by this extension.
     * @param clusterService the cluster service used to coordinate actions across the cluster.
     * @param threadPool the thread pool used to execute tasks.
     * @param client the client used to interact with the Elasticsearch cluster.
     * @param settingsModule the module containing Elasticsearch settings.
     * @param expressionResolver the resolver used to parse index name expressions.
     * @return an empty List.
     */
    default List<PersistentTasksExecutor<?>> getPersistentTasksExecutor(
        ClusterService clusterService,
        ThreadPool threadPool,
        Client client,
        SettingsModule settingsModule,
        IndexNameExpressionResolver expressionResolver
    ) {
        return Collections.emptyList();
    }

}
