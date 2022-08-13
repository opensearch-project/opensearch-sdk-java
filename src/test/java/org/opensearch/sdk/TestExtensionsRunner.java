/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.sdk;

import static java.util.Collections.emptySet;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.Version;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.NamedWriteableRegistryResponse;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.extensions.DiscoveryExtension;
import org.opensearch.discovery.InitializeExtensionsRequest;
import org.opensearch.discovery.InitializeExtensionsResponse;
import org.opensearch.extensions.OpenSearchRequest;
import org.opensearch.extensions.ExtensionsOrchestrator.OpenSearchRequestType;
import org.opensearch.sdk.handlers.ClusterSettingsResponseHandler;
import org.opensearch.sdk.handlers.ClusterStateResponseHandler;
import org.opensearch.sdk.handlers.LocalNodeResponseHandler;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.Transport;

public class TestExtensionsRunner extends OpenSearchTestCase {

    private ExtensionsRunner extensionsRunner;
    private TransportService transportService;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        this.extensionsRunner = new ExtensionsRunner();
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
    }

    // test manager method invokes start on transport service
    @Test
    public void testTransportServiceStarted() {

        // verify mocked object interaction in manager method
        extensionsRunner.startTransportService(transportService);
        verify(transportService, times(1)).start();
    }

    // test manager method invokes accept incoming requests on transport service
    @Test
    public void testTransportServiceAcceptedIncomingRequests() {

        // verify mocked object interaction in manager method
        extensionsRunner.startTransportService(transportService);
        verify(transportService, times(1)).acceptIncomingRequests();
    }

    @Test
    public void testRegisterRequestHandler() {

        extensionsRunner.startTransportService(transportService);
        verify(transportService, times(5)).registerRequestHandler(anyString(), anyString(), anyBoolean(), anyBoolean(), any(), any());
    }

    @Test
    public void testHandleExtensionInitRequest() throws UnknownHostException {
        DiscoveryNode sourceNode = new DiscoveryNode(
            "test_node",
            new TransportAddress(InetAddress.getByName("localhost"), 9876),
            emptyMap(),
            emptySet(),
            Version.CURRENT
        );
        List<DiscoveryExtension> extensions = List.of(
            new DiscoveryExtension("sample-extension", "opensearch-sdk-1", null, null, null, null, null, null, null)
        );
        PluginRequest pluginRequest = new PluginRequest(sourceNode, extensions);
        PluginResponse response = extensionsRunner.handlePluginsRequest(pluginRequest);
        assertEquals("sample-extension", response.getName());

        // Test if unique ID is set
        assertEquals("opensearch-sdk-1", extensionsRunner.getUniqueId());
        // Test if the source node is set after handlePluginRequest() is called during OpenSearch bootstrap
        assertEquals(sourceNode, extensionsRunner.getOpensearchNode());

        InitializeExtensionsRequest extensionInitRequest = new InitializeExtensionsRequest(sourceNode, null);
        InitializeExtensionsResponse response = extensionsRunner.handleExtensionInitRequest(extensionInitRequest);
        assertEquals(response.getName(), "extension");

        // Test if the source node is set after handleExtensionInitRequest()) is called during OpenSearch bootstrap
        assertEquals(extensionsRunner.getOpensearchNode(), sourceNode);
    }

    @Test
    public void testHandleOpenSearchRequest() throws Exception {

        OpenSearchRequest request = new OpenSearchRequest(OpenSearchRequestType.REQUEST_OPENSEARCH_NAMED_WRITEABLE_REGISTRY);
        assertEquals(extensionsRunner.handleOpenSearchRequest(request).getClass(), NamedWriteableRegistryResponse.class);

        // Add additional OpenSearch request handler tests here for each default extension point
    }

    @Test
    public void testClusterStateRequest() {

        extensionsRunner.sendClusterStateRequest(transportService);

        verify(transportService, times(1)).sendRequest(any(), anyString(), any(), any(ClusterStateResponseHandler.class));
    }

    @Test
    public void testClusterSettingRequest() {

        extensionsRunner.sendClusterSettingsRequest(transportService);

        verify(transportService, times(1)).sendRequest(any(), anyString(), any(), any(ClusterSettingsResponseHandler.class));
    }

    @Test
    public void testLocalNodeRequest() {

        extensionsRunner.sendLocalNodeRequest(transportService);

        verify(transportService, times(1)).sendRequest(any(), anyString(), any(), any(LocalNodeResponseHandler.class));
    }

    @Test
    public void testRegisterApiRequest() {

        extensionsRunner.sendRegisterApiRequest(transportService);

        verify(transportService, times(1)).sendRequest(any(), anyString(), any(), any(RegisterApiResponseHandler.class));
    }
}
