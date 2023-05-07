/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.api;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AnalyzerProvider;
import org.opensearch.index.analysis.CharFilterFactory;
import org.opensearch.index.analysis.PreBuiltAnalyzerProviderFactory;
import org.opensearch.index.analysis.PreConfiguredCharFilter;
import org.opensearch.index.analysis.PreConfiguredTokenFilter;
import org.opensearch.index.analysis.PreConfiguredTokenizer;
import org.opensearch.index.analysis.TokenFilterFactory;
import org.opensearch.index.analysis.TokenizerFactory;
import org.opensearch.indices.analysis.AnalysisModule;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * OpenSearch doesn't have any automatic mechanism to share these components between indexes. If any component is heavy enough to warrant
 * such sharing then it is the Extension's responsibility to do it in their {@link AnalysisModule.AnalysisProvider} implementation. We recommend against doing
 * this unless absolutely necessary because it can be difficult to get the caching right given things like behavior changes across versions.
 */
public interface AnalysisExtension {
    /**
     * Override to add additional {@link CharFilter}s. See {@link #requiresAnalysisSettings(AnalysisModule.AnalysisProvider)}
     * how to on get the configuration from the index.
     */
    default Map<String, AnalysisModule.AnalysisProvider<CharFilterFactory>> getCharFilters() {
        return emptyMap();
    }

    /**
     * Override to add additional {@link TokenFilter}s. See {@link #requiresAnalysisSettings(AnalysisModule.AnalysisProvider)}
     * how to on get the configuration from the index.
     */
    default Map<String, AnalysisModule.AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        return emptyMap();
    }

    /**
     * Override to add additional {@link Tokenizer}s. See {@link #requiresAnalysisSettings(AnalysisModule.AnalysisProvider)}
     * how to on get the configuration from the index.
     */
    default Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> getTokenizers() {
        return emptyMap();
    }

    /**
     * Override to add additional {@link Analyzer}s. See {@link #requiresAnalysisSettings(AnalysisModule.AnalysisProvider)}
     * how to on get the configuration from the index.
     */
    default Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
        return emptyMap();
    }

    /**
     * Override to add additional pre-configured {@link Analyzer}s.
     */
    default List<PreBuiltAnalyzerProviderFactory> getPreBuiltAnalyzerProviderFactories() {
        return emptyList();
    }

    /**
     * Override to add additional pre-configured {@link CharFilter}s.
     */
    default List<PreConfiguredCharFilter> getPreConfiguredCharFilters() {
        return emptyList();
    }

    /**
     * Override to add additional pre-configured {@link TokenFilter}s.
     */
    default List<PreConfiguredTokenFilter> getPreConfiguredTokenFilters() {
        return emptyList();
    }

    /**
     * Override to add additional pre-configured {@link Tokenizer}.
     */
    default List<PreConfiguredTokenizer> getPreConfiguredTokenizers() {
        return emptyList();
    }

    /**
     * Override to add additional hunspell {@link org.apache.lucene.analysis.hunspell.Dictionary}s.
     * @return an empty Map that has a key of type String and a value of type org.apache.lucene.analysis.hunspell.Dictionary.
     */
    default Map<String, org.apache.lucene.analysis.hunspell.Dictionary> getHunspellDictionaries() {
        return emptyMap();
    }

    /**
     * Mark an {@link AnalysisModule.AnalysisProvider} as requiring the index's settings.
     * @param provider this is an instance of the basic interface for analysis components.
     * @param <T> is a type parameter that specifies the type of the analysis module that will be returned by the provider.
     */
    static <T> AnalysisModule.AnalysisProvider<T> requiresAnalysisSettings(AnalysisModule.AnalysisProvider<T> provider) {
        return new AnalysisModule.AnalysisProvider<T>() {
            @Override
            public T get(IndexSettings indexSettings, Environment environment, String name, Settings settings) throws IOException {
                return provider.get(indexSettings, environment, name, settings);
            }

            @Override
            public boolean requiresAnalysisSettings() {
                return true;
            }
        };
    }
}
