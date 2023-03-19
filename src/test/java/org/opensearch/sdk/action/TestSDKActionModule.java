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
import org.mockito.ArgumentCaptor;
import org.opensearch.Version;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.ActionType;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.RegisterTransportActionsRequest;
import org.opensearch.sdk.ActionExtension;
import org.opensearch.sdk.BaseExtension;
import org.opensearch.sdk.ExtensionSettings;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.handlers.AcknowledgedResponseHandler;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.Transport;
import org.opensearch.transport.TransportService;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestSDKActionModule extends OpenSearchTestCase {

    private static final String TEST_UNIQUE_ID = "test-extension";
    private static final String TEST_ACTION_NAME = "testAction";

    private ExtensionsRunner extensionsRunner;
    private TransportService transportService;
    private DiscoveryNode opensearchNode;
    private SDKActionModule sdkActionModule;

    private static class TestActionExtension extends BaseExtension implements ActionExtension {
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
        this.transportService = spy(
            new TransportService(
                Settings.EMPTY,
                mock(Transport.class),
                null,
                TransportService.NOOP_TRANSPORT_INTERCEPTOR,
                x -> null,
                null,
                Collections.emptySet()
            )
        );
        this.opensearchNode = new DiscoveryNode(
            "test_node",
            new TransportAddress(InetAddress.getByName("localhost"), 9876),
            emptyMap(),
            emptySet(),
            Version.CURRENT
        );
        extensionsRunner = mock(ExtensionsRunner.class);
        when(extensionsRunner.getExtensionTransportService()).thenReturn(transportService);
        when(extensionsRunner.getOpensearchNode()).thenReturn(opensearchNode);
        when(extensionsRunner.getUniqueId()).thenReturn(TEST_UNIQUE_ID);
        sdkActionModule = new SDKActionModule(extensionsRunner, new TestActionExtension());
    }

    @Test
    public void testRegisterTransportAction() {
        ArgumentCaptor<RegisterTransportActionsRequest> registerTransportActionsRequestCaptor = ArgumentCaptor.forClass(
            RegisterTransportActionsRequest.class
        );
        sdkActionModule.sendRegisterTransportActionsRequest();
        verify(transportService, times(1)).sendRequest(
            any(),
            eq(ExtensionsManager.REQUEST_EXTENSION_REGISTER_TRANSPORT_ACTIONS),
            registerTransportActionsRequestCaptor.capture(),
            any(AcknowledgedResponseHandler.class)
        );
        assertEquals(TEST_UNIQUE_ID, registerTransportActionsRequestCaptor.getValue().getUniqueId());
        assertTrue(registerTransportActionsRequestCaptor.getValue().getTransportActions().contains(ProxyAction.NAME));
        assertTrue(registerTransportActionsRequestCaptor.getValue().getTransportActions().contains(TEST_ACTION_NAME));
    }
}
