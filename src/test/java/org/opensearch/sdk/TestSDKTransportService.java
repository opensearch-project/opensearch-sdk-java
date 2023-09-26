/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.opensearch.Version;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.RegisterTransportActionsRequest;
import org.opensearch.sdk.action.RemoteExtensionAction;
import org.opensearch.sdk.action.SDKActionModule;
import org.opensearch.sdk.action.TestSDKActionModule;
import org.opensearch.sdk.handlers.AcknowledgedResponseHandler;
import org.opensearch.telemetry.tracing.noop.NoopTracer;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.Transport;
import org.opensearch.transport.TransportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Collections;

import org.mockito.ArgumentCaptor;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestSDKTransportService extends OpenSearchTestCase {

    private static final String TEST_UNIQUE_ID = "test-extension";

    private TransportService transportService;
    private DiscoveryNode opensearchNode;
    private SDKActionModule sdkActionModule;
    private SDKTransportService sdkTransportService;

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
                Collections.emptySet(),
                NoopTracer.INSTANCE
            )
        );
        this.opensearchNode = new DiscoveryNode(
            "test_node",
            new TransportAddress(InetAddress.getByName("localhost"), 9876),
            emptyMap(),
            emptySet(),
            Version.CURRENT
        );
        sdkActionModule = new SDKActionModule(new TestSDKActionModule.TestActionExtension());

        sdkTransportService = new SDKTransportService();
        sdkTransportService.setTransportService(transportService);
        sdkTransportService.setOpensearchNode(opensearchNode);
        sdkTransportService.setUniqueId(TEST_UNIQUE_ID);
    }

    @Test
    public void testRegisterTransportAction() {
        ArgumentCaptor<RegisterTransportActionsRequest> registerTransportActionsRequestCaptor = ArgumentCaptor.forClass(
            RegisterTransportActionsRequest.class
        );

        sdkTransportService.sendRegisterTransportActionsRequest(sdkActionModule.getActions());
        verify(transportService, times(1)).sendRequest(
            any(),
            eq(ExtensionsManager.REQUEST_EXTENSION_REGISTER_TRANSPORT_ACTIONS),
            registerTransportActionsRequestCaptor.capture(),
            any(AcknowledgedResponseHandler.class)
        );
        assertEquals(TEST_UNIQUE_ID, registerTransportActionsRequestCaptor.getValue().getUniqueId());
        // Should contain the TestAction, but since it's mocked the name may change
        assertTrue(
            registerTransportActionsRequestCaptor.getValue()
                .getTransportActions()
                .stream()
                .anyMatch(s -> s.startsWith("org.opensearch.action.ActionType$MockitoMock$"))
        );
        // Internal action should be filtered out
        assertFalse(registerTransportActionsRequestCaptor.getValue().getTransportActions().contains(RemoteExtensionAction.class.getName()));
    }
}
