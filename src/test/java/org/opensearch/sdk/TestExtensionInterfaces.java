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

import org.junit.jupiter.api.Test;
import org.opensearch.common.settings.Settings;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Map;

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
    public void testGetEmptyMap() {
        // Create an instance of the class that implements the interface being tested
        RepositoryExtension extension = new RepositoryExtension() {
            @Override
            public Map<String, Object> getEmptyMap() {
                return null;
            }
        };

        // Call the method that returns an empty map
        Map<String, Object> result = extension.getEmptyMap();

        // Verify that the map is empty
        assertTrue(result == null || result.isEmpty());
    }

}
