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
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.threadpool.ThreadPool;

public abstract class BaseExtension implements Extension {
    protected SDKClient client;
    protected ClusterService clusterService;
    protected ThreadPool threadPool;
    protected NamedXContentRegistry xContentRegistry;
    protected Environment environment;

    /**
     * Empty constructor to fulfill abstract class requirements
     */
    public BaseExtension() {

    }

    @Override
    public Collection<Object> createComponents(
        SDKClient client,
        ClusterService clusterService,
        ThreadPool threadPool,
        NamedXContentRegistry xContentRegistry,
        Environment environment
    ) {
        this.client = client;
        this.clusterService = clusterService;
        this.threadPool = threadPool;
        this.xContentRegistry = xContentRegistry;
        this.environment = environment;

        return Collections.emptyList();
    }
}
