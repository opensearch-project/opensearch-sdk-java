/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opensearch.Version;
import org.opensearch.action.admin.cluster.state.ClusterStateRequest;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.RegisterTransportActionsRequest;
import org.opensearch.extensions.action.TransportActionRequestFromExtension;
import org.opensearch.sdk.action.RemoteExtensionAction;
import org.opensearch.sdk.action.RemoteExtensionActionRequest;
import org.opensearch.sdk.action.SDKActionModule;
import org.opensearch.sdk.action.TestSDKActionModule;
import org.opensearch.sdk.handlers.AcknowledgedResponseHandler;
import org.opensearch.sdk.handlers.ClusterStateResponseHandler;
import org.opensearch.sdk.handlers.ExtensionActionResponseHandler;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.Transport;
import org.opensearch.transport.TransportService;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

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

    @Test
    public void testRemoteExtensionActionRequest() {
        ArgumentCaptor<TransportActionRequestFromExtension> transportActionRequestFromExtensionCaptor = ArgumentCaptor.forClass(
            TransportActionRequestFromExtension.class
        );
        String expectedAction = "com.example.action";
        String expectedRequest = "com.example.request";
        byte[] expectedRequestBytes = "test".getBytes(StandardCharsets.UTF_8);
        RemoteExtensionActionRequest request = new RemoteExtensionActionRequest(expectedAction, expectedRequest, expectedRequestBytes);
        sdkTransportService.sendRemoteExtensionActionRequest(request);
        verify(transportService, times(1)).sendRequest(
            any(),
            eq(ExtensionsManager.TRANSPORT_ACTION_REQUEST_FROM_EXTENSION),
            transportActionRequestFromExtensionCaptor.capture(),
            any(ExtensionActionResponseHandler.class)
        );
        assertEquals(TEST_UNIQUE_ID, transportActionRequestFromExtensionCaptor.getValue().getUniqueId());
        assertEquals(expectedAction, transportActionRequestFromExtensionCaptor.getValue().getAction());
        String expectedString = expectedRequest + (char) RemoteExtensionActionRequest.UNIT_SEPARATOR + "test";
        assertEquals(
            expectedString,
            new String(transportActionRequestFromExtensionCaptor.getValue().getRequestBytes(), StandardCharsets.UTF_8)
        );
    }

    @Test
    public void testsendClusterStateRequest() {
        ArgumentCaptor<ClusterStateRequest> clusterStateRequestCaptor = ArgumentCaptor.forClass(ClusterStateRequest.class);
        ClusterStateRequest request = new ClusterStateRequest().clear().indices("foo", "bar");
        sdkTransportService.sendClusterStateRequest(request);
        verify(transportService, times(1)).sendRequest(
            any(),
            eq(ExtensionsManager.REQUEST_EXTENSION_CLUSTER_STATE),
            clusterStateRequestCaptor.capture(),
            any(ClusterStateResponseHandler.class)
        );
        assertArrayEquals(new String[] { "foo", "bar" }, clusterStateRequestCaptor.getValue().indices());
    }
}
