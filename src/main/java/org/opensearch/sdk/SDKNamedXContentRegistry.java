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
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.xcontent.NamedXContentRegistry.Entry;
import org.opensearch.indices.IndicesModule;
import org.opensearch.search.SearchModule;

/**
 * Combines Extension NamedXContent with core OpenSearch NamedXContent
 */
public class SDKNamedXContentRegistry {
    /**
     * The empty {@link SDKNamedXContentRegistry} for use when you are sure that you aren't going to call
     * {@link XContentParser#namedObject(Class, String, Object)}. Be *very* careful with this singleton because a parser using it will fail
     * every call to {@linkplain XContentParser#namedObject(Class, String, Object)}. Every non-test usage really should be checked
     * thoroughly and marked with a comment about how it was checked. That way anyone that sees code that uses it knows that it is
     * potentially dangerous.
     */
    public static final SDKNamedXContentRegistry EMPTY = new SDKNamedXContentRegistry();

    private NamedXContentRegistry namedXContentRegistry;

    /**
     * Creates an empty registry.
     */
    private SDKNamedXContentRegistry() {
        this.namedXContentRegistry = NamedXContentRegistry.EMPTY;
    }

    /**
     * Creates and populates a NamedXContentRegistry with the NamedXContentRegistry entries for this extension and locally defined content.
     *
     * @param runner The ExtensionsRunner instance.
     */
    public SDKNamedXContentRegistry(ExtensionsRunner runner) {
        this.namedXContentRegistry = createRegistry(runner.getEnvironmentSettings(), runner.getCustomNamedXContent());
    }

    /**
     * Updates the NamedXContentRegistry with the NamedXContentRegistry entries for this extension and locally defined content.
     * <p>
     * Only necessary if environment settings have changed.
     *
     * @param runner The ExtensionsRunner instance.
     */
    public void updateNamedXContentRegistry(ExtensionsRunner runner) {
        this.namedXContentRegistry = createRegistry(runner.getEnvironmentSettings(), runner.getCustomNamedXContent());
    }

    private NamedXContentRegistry createRegistry(Settings settings, List<Entry> extensionNamedXContent) {
        Stream<Entry> extensionContent = extensionNamedXContent == null ? Stream.empty() : extensionNamedXContent.stream();
        return new NamedXContentRegistry(
            Stream.of(
                extensionContent,
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

    /**
     * Sets the NamedXContentRegistry. Used primarily for tests.
     *
     * @param namedXContentRegistry The registry to set.
     */
    public void setRegistry(NamedXContentRegistry namedXContentRegistry) {
        this.namedXContentRegistry = namedXContentRegistry;
    }
}
