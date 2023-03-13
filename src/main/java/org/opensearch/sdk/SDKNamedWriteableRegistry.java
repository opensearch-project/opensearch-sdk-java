/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.opensearch.cluster.ClusterModule;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.NamedWriteableRegistry.Entry;
import org.opensearch.common.network.NetworkModule;
import org.opensearch.common.settings.Settings;
import org.opensearch.indices.IndicesModule;
import org.opensearch.search.SearchModule;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Combines Extension NamedWriteable with core OpenSearch NamedWriteable
 */
public class SDKNamedWriteableRegistry {
    private NamedWriteableRegistry namedXWriteableRegistry;

    /**
     * Creates and populates a NamedWriteableRegistry with the NamedWriteableRegistry entries for this extension and locally defined content.
     *
     * @param runner The ExtensionsRunner instance.
     */
    public SDKNamedWriteableRegistry(ExtensionsRunner runner) {
        this.namedXWriteableRegistry = createRegistry(runner.getEnvironmentSettings(), runner.getCustomNamedWriteable());
    }

    private NamedWriteableRegistry createRegistry(Settings settings, List<Entry> extensionNamedWriteable) {
        Stream<Entry> extensionContent = extensionNamedWriteable == null ? Stream.empty() : extensionNamedWriteable.stream();
        return new NamedWriteableRegistry(
            Stream.of(
                extensionContent,
                NetworkModule.getNamedWriteables().stream(),
                new IndicesModule(Collections.emptyList()).getNamedWriteables().stream(),
                new SearchModule(settings, Collections.emptyList()).getNamedWriteables().stream(),
                ClusterModule.getNamedWriteables().stream()
            ).flatMap(Function.identity()).collect(toList())
        );
    }

    /**
     * Gets the NamedWriteableRegistry.
     *
     * @return The NamedWriteableRegistry. Includes both extension-defined Writeable and core OpenSearch Writeable.
     */
    public NamedWriteableRegistry getRegistry() {
        return this.namedXWriteableRegistry;
    }
}
