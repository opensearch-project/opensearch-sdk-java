/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.opensearch.cluster.ClusterModule;
import org.opensearch.common.network.NetworkModule;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.indices.IndicesModule;
import org.opensearch.search.SearchModule;

/**
 * Combines Extension NamedXContent with core OpenSearch NamedXContent
 */
public class ExtensionNamedXContentRegistry {
    private final NamedXContentRegistry namedXContentRegistry;

    /**
     * Creates and populates a NamedXContentRegistry with the given NamedXContentRegistry entries for this extension and
     * locally defined content.
     *
     * @param settings OpenSearch environment settings
     * @param extensionNamedXContent List of NamedXContentRegistry.Entry to be registered
     */
    public ExtensionNamedXContentRegistry(Settings settings, List<NamedXContentRegistry.Entry> extensionNamedXContent) {
        this.namedXContentRegistry = new NamedXContentRegistry(
            Stream.of(
                extensionNamedXContent.stream(),
                NetworkModule.getNamedXContents().stream(),
                IndicesModule.getNamedXContents().stream(),
                new SearchModule(settings, Collections.emptyList()).getNamedXContents().stream(),
                ClusterModule.getNamedXWriteables().stream()
            ).flatMap(Function.identity()).collect(toList())
        );
    }

    /**
     * Gets the NamedXContentRegistry.
     *
     * @return The NamedXContentRegistry. Includes both extension-defined XContent and core OpenSearch XContent.
     */
    public NamedXContentRegistry getRegistry() {
        return this.namedXContentRegistry;
    }
}
