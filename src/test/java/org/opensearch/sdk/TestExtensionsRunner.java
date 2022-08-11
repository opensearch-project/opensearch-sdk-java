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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.Version;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.NamedWriteableRegistryResponse;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
<<<<<<< HEAD
import org.opensearch.common.xcontent.NamedXContentRegistryResponse;
import org.opensearch.discovery.PluginRequest;
import org.opensearch.discovery.PluginResponse;
=======
import org.opensearch.discovery.InitializeExtensionsRequest;
import org.opensearch.discovery.InitializeExtensionsResponse;
>>>>>>> main
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
    private Settings settings;
    private TransportService transportService;

    @BeforeEach
    public void setUp() throws Exception {
        this.extensionsRunner = new ExtensionsRunner();
        this.settings = Settings.builder().put("node.name", "sdk").build();
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

    // test ExtensionsRunner getTransportService return type is transport service
    @Test
    public void testGetTransportService() {
        assert (extensionsRunner.createTransportService(settings) instanceof TransportService);
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
        verify(transportService, times(6)).registerRequestHandler(anyString(), anyString(), anyBoolean(), anyBoolean(), any(), any());
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
        InitializeExtensionsRequest extensionInitRequest = new InitializeExtensionsRequest(sourceNode, null);
        InitializeExtensionsResponse response = extensionsRunner.handleExtensionInitRequest(extensionInitRequest);
        assertEquals(response.getName(), "extension");

        // Test if the source node is set after handleExtensionInitRequest()) is called during OpenSearch bootstrap
        assertEquals(extensionsRunner.getOpensearchNode(), sourceNode);
    }

    @Test
    public void testHandleOpenSearchRequest() throws Exception {

        OpenSearchRequest namedWriteableRegistryRequest = new OpenSearchRequest(
            OpenSearchRequestType.REQUEST_OPENSEARCH_NAMED_WRITEABLE_REGISTRY
        );
        assertEquals(
            NamedWriteableRegistryResponse.class,
            extensionsRunner.handleOpenSearchRequest(namedWriteableRegistryRequest).getClass()
        );

        OpenSearchRequest namedXContentRegistryRequest = new OpenSearchRequest(
            OpenSearchRequestType.REQUEST_OPENSEARCH_NAMED_XCONTENT_REGISTRY
        );
        assertEquals(
            NamedXContentRegistryResponse.class,
            extensionsRunner.handleOpenSearchRequest(namedXContentRegistryRequest).getClass()
        );

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

}
