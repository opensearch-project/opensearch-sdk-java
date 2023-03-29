/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.opensearch.cluster.service.ClusterService;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.indices.recovery.RecoverySettings;
import org.opensearch.repositories.Repository;

import java.util.Collections;
import java.util.Map;

/**
 * An extension point for {@link Extension} implementations to add custom snapshot repositories.
 *
 */

public interface RepositoryExtension {

    /**
     * Returns repository types added by this extension.
     *
     * @param env The environment for the local node, which may be used for the local settings and path. repo
     * @param namedXContentRegistry register named objects.
     * @param clusterService  service for operating cluster state.
     * @param recoverySettings settings related to cluster recovery.
     *
     * The key of the returned {@link Map} is the type name of the repository and
     * the value is a factory to construct the {@link Repository} interface.
     */
    default Map<String, Repository.Factory> getRepositories(
        Environment env,
        NamedXContentRegistry namedXContentRegistry,
        ClusterService clusterService,
        RecoverySettings recoverySettings
    ) {
        return Collections.emptyMap();
    }

    /**
     * Returns internal repository types added by this extension. Internal repositories cannot be registered
     * through the external API.
     *
     * @param env The environment for the local node, which may be used for the local settings and path.repo
     * @param namedXContentRegistry register named objects.
     * @param clusterService  service for operating cluster state.
     * @param recoverySettings settings related to cluster recovery.
     *
     * The key of the returned {@link Map} is the type name of the repository and
     * the value is a factory to construct the {@link Repository} interface.
     */
    default Map<String, Repository.Factory> getInternalRepositories(
        Environment env,
        NamedXContentRegistry namedXContentRegistry,
        ClusterService clusterService,
        RecoverySettings recoverySettings
    ) {
        return Collections.emptyMap();
    }
}
