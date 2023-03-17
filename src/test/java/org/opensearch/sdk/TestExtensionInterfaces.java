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
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.index.mapper.Mapper;
import org.opensearch.index.mapper.MetadataFieldMapper;
import org.opensearch.indices.recovery.RecoverySettings;
import org.opensearch.repositories.Repository;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Map;
import java.util.function.Predicate;

public class TestExtensionInterfaces extends OpenSearchTestCase {

    @Test
    void testExtension() {
        Extension extension = new Extension() {
            @Override
            public ExtensionSettings getExtensionSettings() {
                return null;
            }
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

    @Test
    void testSystemIndexExtension() {
        SystemIndexExtension systemIndexExtension = new SystemIndexExtension() {
        };
        assertTrue(systemIndexExtension.getSystemIndexDescriptors(null).isEmpty());
    }

    @Test
    void getRepositoriesReturnsEmptyMapByDefault() {

        Environment env = mock(Environment.class);
        NamedXContentRegistry namedXContentRegistry = mock(NamedXContentRegistry.class);
        ClusterService clusterService = mock(ClusterService.class);
        RecoverySettings recoverySettings = mock(RecoverySettings.class);

        RepositoryExtension repositoryExtension = new RepositoryExtension() {
            @Override
            public Map<String, Repository.Factory> getRepositories(
                Environment env,
                NamedXContentRegistry namedXContentRegistry,
                ClusterService clusterService,
                RecoverySettings recoverySettings
            ) {
                return RepositoryExtension.super.getRepositories(env, namedXContentRegistry, clusterService, recoverySettings);
            }

            @Override
            public Map<String, Repository.Factory> getInternalRepositories(
                Environment env,
                NamedXContentRegistry namedXContentRegistry,
                ClusterService clusterService,
                RecoverySettings recoverySettings
            ) {
                return RepositoryExtension.super.getInternalRepositories(env, namedXContentRegistry, clusterService, recoverySettings);
            }
        };

        Map<String, Repository.Factory> repositories = repositoryExtension.getRepositories(
            env,
            namedXContentRegistry,
            clusterService,
            recoverySettings
        );
        assertEquals(Collections.emptyMap(), repositories);
    }

    @Test
    void getInternalRepositoriesReturnsEmptyMapByDefault() {

        Environment env = mock(Environment.class);
        NamedXContentRegistry namedXContentRegistry = mock(NamedXContentRegistry.class);
        ClusterService clusterService = mock(ClusterService.class);
        RecoverySettings recoverySettings = mock(RecoverySettings.class);

        RepositoryExtension repositoryExtension = new RepositoryExtension() {
        };

        Map<String, Repository.Factory> repositories = repositoryExtension.getInternalRepositories(
            env,
            namedXContentRegistry,
            clusterService,
            recoverySettings
        );
        assertEquals(Collections.emptyMap(), repositories);
    }
}
