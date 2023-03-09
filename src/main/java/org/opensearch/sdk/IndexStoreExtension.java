/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.apache.lucene.store.Directory;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.cluster.routing.ShardRouting;
import org.opensearch.common.Nullable;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.shard.ShardPath;
import org.opensearch.indices.recovery.RecoveryState;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * A extension that provides alternative directory implementations.
 *
 * 
 */
public interface IndexStoreExtension {

    /**
     * An interface that describes how to create a new directory instance per shard.
     */
    @FunctionalInterface
    interface DirectoryFactory {
        /**
         * Creates a new directory per shard. This method is called once per shard on shard creation.
         * @param indexSettings the shards index settings
         * @param shardPath the path the shard is using
         * @return a new lucene directory instance
         * @throws IOException if an IOException occurs while opening the directory
         */
        Directory newDirectory(IndexSettings indexSettings, ShardPath shardPath) throws IOException;
    }

    /**
     * An interface that describes how to create a new remote directory instance per shard.
     */
    @FunctionalInterface
    interface RemoteDirectoryFactory {
        /**
         * Creates a new remote directory per shard. This method is called once per shard on shard creation.
         * @param repositoryName repository name
         * @param indexSettings the shards index settings
         * @param shardPath the path the shard is using
         * @return a new RemoteDirectory instance
         * @throws IOException if an IOException occurs while opening the directory
         */
        Directory newDirectory(String repositoryName, IndexSettings indexSettings, ShardPath shardPath) throws IOException;
    }

    /**
     * The {@link DirectoryFactory} mappings for this extension. When an index is created the store type setting
     * {@link org.opensearch.index.IndexModule#INDEX_STORE_TYPE_SETTING} on the index will be examined and either use the default or a
     * built-in type, or looked up among all the directory factories from {@link IndexStoreExtension} extensions.
     *
     * @return a map from store type to an directory factory
     */
    Map<String, DirectoryFactory> getDirectoryFactories();

    /**
     * An interface that allows to create a new {@link RecoveryState} per shard.
     */
    @FunctionalInterface
    interface RecoveryStateFactory {
        /**
         * Creates a new {@link RecoveryState} per shard. This method is called once per shard on shard creation.
         * @return a new RecoveryState instance
         */
        RecoveryState newRecoveryState(ShardRouting shardRouting, DiscoveryNode targetNode, @Nullable DiscoveryNode sourceNode);
    }

    /**
     * The {@link RecoveryStateFactory} mappings for this extension. When an index is created the recovery type setting
     * {@link org.opensearch.index.IndexModule#INDEX_RECOVERY_TYPE_SETTING} on the index will be examined and either use the default
     * or looked up among all the recovery state factories from {@link IndexStoreExtension} extensions.
     *
     * @return a map from recovery type to an recovery state factory
     */
    default Map<String, RecoveryStateFactory> getRecoveryStateFactories() {
        return Collections.emptyMap();
    }
}
