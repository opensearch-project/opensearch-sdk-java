/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.action;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.ActionType;
import org.opensearch.sdk.api.ActionExtension;
import org.opensearch.sdk.BaseExtension;
import org.opensearch.sdk.ExtensionSettings;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestSDKActionModule extends OpenSearchTestCase {

    public static final String TEST_ACTION_NAME = "testAction";

    private SDKActionModule sdkActionModule;

    public static class TestActionExtension extends BaseExtension implements ActionExtension {
        public TestActionExtension() {
            super(mock(ExtensionSettings.class));
        }

        @Override
        public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
            @SuppressWarnings("unchecked")
            ActionType<ActionResponse> testAction = mock(ActionType.class);
            when(testAction.name()).thenReturn(TEST_ACTION_NAME);

            return Arrays.asList(new ActionHandler<ActionRequest, ActionResponse>(testAction, null));
        }
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        sdkActionModule = new SDKActionModule(new TestActionExtension());
    }

    @Test
    public void testGetActions() {
        assertEquals(2, sdkActionModule.getActions().size());
        assertTrue(sdkActionModule.getActions().containsKey(RemoteExtensionAction.NAME));
        assertTrue(sdkActionModule.getActions().containsKey(TEST_ACTION_NAME));
    }
}
