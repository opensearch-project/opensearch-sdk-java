/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import java.util.Collection;
import java.util.Collections;

import org.opensearch.cluster.service.ClusterService;
import org.opensearch.threadpool.ThreadPool;

public abstract class BaseExtension implements Extension {
    protected SDKClient client;
    protected ClusterService clusterService;
    protected ThreadPool threadPool;

    /**
     * Empty constructor to fulfill abstract class requirements
     */
    protected BaseExtension() {

    }

    public Collection<Object> createComponents(SDKClient client, ClusterService clusterService, ThreadPool threadPool) {
        this.client = client;
        this.clusterService = clusterService;
        this.threadPool = threadPool;

        return Collections.emptyList();
    }
}
