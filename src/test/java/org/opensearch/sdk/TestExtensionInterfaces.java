/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Collections;
import java.util.Map;

<<<<<<< HEAD
import org.junit.jupiter.api.Assertions;
=======
>>>>>>> 2420d90 (Tests for IndexStoreExtension added.)
import org.junit.jupiter.api.Test;
import org.opensearch.common.breaker.CircuitBreaker;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.mapper.Mapper;
import org.opensearch.index.mapper.MetadataFieldMapper;
import org.opensearch.indices.analysis.AnalysisModule;
import org.opensearch.indices.breaker.BreakerSettings;
import org.opensearch.ingest.Processor;
import org.opensearch.sdk.api.ActionExtension;
import org.opensearch.sdk.api.AnalysisExtension;
import org.opensearch.sdk.api.CircuitBreakerExtension;
import org.opensearch.sdk.api.EngineExtension;
import org.opensearch.sdk.api.IndexStoreExtension;
import org.opensearch.sdk.api.IngestExtension;
import org.opensearch.sdk.api.MapperExtension;
import org.opensearch.sdk.api.PersistentTaskExtension;
import org.opensearch.sdk.api.RepositoryExtension;
import org.opensearch.sdk.api.ScriptExtension;
import org.opensearch.sdk.api.SearchExtension;
import org.opensearch.sdk.api.SystemIndexExtension;

import org.opensearch.test.OpenSearchTestCase;

import java.util.function.Predicate;

public class TestExtensionInterfaces extends OpenSearchTestCase {

    @Test
    void testExtension() {
        Extension extension = new Extension() {
            @Override
            public ExtensionSettings getExtensionSettings() {
                return null;
            }

            @Override
            public void setExtensionsRunner(ExtensionsRunner runner) {}
        };

        assertTrue(extension.getSettings().isEmpty());
        assertTrue(extension.getNamedXContent().isEmpty());
        assertTrue(extension.createComponents(null).isEmpty());
        assertTrue(extension.getExecutorBuilders(Settings.EMPTY).isEmpty());
    }

    @Test
    void testActionExtension() {
        ActionExtension actionExtension = new ActionExtension() {
        };

        assertTrue(actionExtension.getActions().isEmpty());
        assertTrue(actionExtension.getClientActions().isEmpty());
        assertTrue(actionExtension.getActionFilters().isEmpty());
        assertTrue(actionExtension.getExtensionRestHandlers().isEmpty());
        assertTrue(actionExtension.getRestHeaders().isEmpty());
        assertTrue(actionExtension.getTaskHeaders().isEmpty());
        assertDoesNotThrow(() -> actionExtension.getRestHandlerWrapper(null));
        assertTrue(actionExtension.indicesAliasesRequestValidators().isEmpty());
        assertTrue(actionExtension.mappingRequestValidators().isEmpty());
    }

    @Test
    void testScriptExtension() {
        ScriptExtension scriptExtension = new ScriptExtension() {
        };
        assertNull(scriptExtension.getScriptEngine(null, null));
        assertTrue(scriptExtension.getContexts().isEmpty());

    }

    @Test
    void testEngineExtension() {
        EngineExtension engineExtension = new EngineExtension() {
        };

        assertTrue(engineExtension.getEngineFactory(null).isEmpty());
        assertTrue(engineExtension.getCustomCodecServiceFactory(null).isEmpty());
        assertTrue(engineExtension.getCustomTranslogDeletionPolicyFactory().isEmpty());
    }

    @Test
    void testESearchExtension() {
        SearchExtension searchExtension = new SearchExtension() {
        };

        assertTrue(searchExtension.getScoreFunctions().isEmpty());
        assertTrue(searchExtension.getSignificanceHeuristics().isEmpty());
        assertTrue(searchExtension.getMovingAverageModels().isEmpty());
        assertTrue(searchExtension.getFetchSubPhases(null).isEmpty());
        assertTrue(searchExtension.getSearchExts().isEmpty());
        assertTrue(searchExtension.getHighlighters().isEmpty());
        assertTrue(searchExtension.getSuggesters().isEmpty());
        assertTrue(searchExtension.getQueries().isEmpty());
        assertTrue(searchExtension.getSorts().isEmpty());
        assertTrue(searchExtension.getAggregations().isEmpty());
        assertTrue(searchExtension.getAggregationExtentions().isEmpty());
        assertTrue(searchExtension.getCompositeAggregations().isEmpty());
        assertTrue(searchExtension.getPipelineAggregations().isEmpty());
        assertTrue(searchExtension.getRescorers().isEmpty());
        assertTrue(searchExtension.getQueryPhaseSearcher().isEmpty());
        assertTrue(searchExtension.getIndexSearcherExecutorProvider().isEmpty());
    }

    @Test
<<<<<<< HEAD
    public void testGetMappers() {
        MapperExtension mapperExtension = new MapperExtension() {
        };
        Map<String, Mapper.TypeParser> mappers = mapperExtension.getMappers();
        Assertions.assertTrue(mappers.isEmpty());
    }

    @Test
    public void testGetMetadataMappers() {
        MapperExtension mapperExtension = new MapperExtension() {
        };
        Map<String, MetadataFieldMapper.TypeParser> metadataMappers = mapperExtension.getMetadataMappers();
        Assertions.assertTrue(metadataMappers.isEmpty());
    }

    @Test
    public void testGetFieldFilter() {
        MapperExtension extension = new MapperExtension() {
        };
        Predicate<String> predicate = extension.getFieldFilter().apply("myIndex");
        Assertions.assertNotNull(predicate);
    }

    @Test
    void testPersistentTaskExtension() {
        PersistentTaskExtension extension = new PersistentTaskExtension() {
        };

        var result = extension.getPersistentTasksExecutor(null, null, null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
=======
>>>>>>> 2420d90 (Tests for IndexStoreExtension added.)
    void testIndexStoreExtension() {
        IndexStoreExtension indexStoreExtension = new IndexStoreExtension() {
            @Override
            public Map<String, DirectoryFactory> getDirectoryFactories() {
                return Collections.emptyMap();
            }
        };
        assertTrue(indexStoreExtension.getDirectoryFactories().isEmpty());
        assertTrue(indexStoreExtension.getRecoveryStateFactories().isEmpty());
    }
<<<<<<< HEAD

    @Test
    void testSystemIndexExtension() {
        SystemIndexExtension systemIndexExtension = new SystemIndexExtension() {
        };
        assertTrue(systemIndexExtension.getSystemIndexDescriptors(null).isEmpty());
    }

    @Test
    void testIngestExtension() {
        IngestExtension ingestExtension = new IngestExtension() {
        };
        Processor.Parameters parameters = new Processor.Parameters(null, null, null, null, null, null, null, null, null);
        assertTrue(ingestExtension.getProcessors(parameters).isEmpty());
    }

    @Test
    void testCircuitBreakerExtension() {
        CircuitBreakerExtension circuitBreakerExtension = new CircuitBreakerExtension() {
            private CircuitBreaker circuitBreaker;

            @Override
            public BreakerSettings getCircuitBreaker(Settings settings) {
                return null;
            }

            @Override
            public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
                this.circuitBreaker = circuitBreaker;
            }
        };
        assertNull(circuitBreakerExtension.getCircuitBreaker(null));
    }

    @Test
    public void testRepositoryExtension() {
        RepositoryExtension repositoryExtension = new RepositoryExtension() {
        };
        assertTrue(repositoryExtension.getRepositories(null, null, null, null).isEmpty());
        assertTrue(repositoryExtension.getInternalRepositories(null, null, null, null).isEmpty());
    }

    @Test
    public void testAnalysisExtension() {
        AnalysisExtension analysisExtension = new AnalysisExtension() {
        };
        AnalysisModule.AnalysisProvider<?> provider = (indexSettings, environment, name, settings) -> null;
        AnalysisModule.AnalysisProvider<?> analysisProvider = AnalysisExtension.requiresAnalysisSettings(provider);

        assertTrue(analysisExtension.getCharFilters().isEmpty());
        assertTrue(analysisExtension.getTokenFilters().isEmpty());
        assertTrue(analysisExtension.getTokenizers().isEmpty());
        assertTrue(analysisExtension.getAnalyzers().isEmpty());
        assertTrue(analysisExtension.getPreBuiltAnalyzerProviderFactories().isEmpty());
        assertTrue(analysisExtension.getPreConfiguredCharFilters().isEmpty());
        assertTrue(analysisExtension.getPreConfiguredTokenFilters().isEmpty());
        assertTrue(analysisExtension.getPreConfiguredTokenizers().isEmpty());
        assertTrue(analysisExtension.getHunspellDictionaries().isEmpty());
        assertTrue(analysisProvider.requiresAnalysisSettings());
    }
=======
>>>>>>> 2420d90 (Tests for IndexStoreExtension added.)
}
