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

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.Version;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.NamedWriteableRegistryResponse;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.discovery.InitializeExtensionsRequest;
import org.opensearch.discovery.InitializeExtensionsResponse;
import org.opensearch.extensions.DiscoveryExtension;
import org.opensearch.extensions.ExtensionsOrchestrator.OpenSearchRequestType;
import org.opensearch.extensions.OpenSearchRequest;
import org.opensearch.extensions.rest.RestExecuteOnExtensionRequest;
import org.opensearch.extensions.rest.RestExecuteOnExtensionResponse;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.rest.RestStatus;
import org.opensearch.sdk.handlers.ClusterSettingsResponseHandler;
import org.opensearch.sdk.handlers.ClusterStateResponseHandler;
import org.opensearch.sdk.handlers.EnvironmentSettingsResponseHandler;
import org.opensearch.sdk.handlers.LocalNodeResponseHandler;
import org.opensearch.sdk.handlers.RegisterRestActionsResponseHandler;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.Transport;
import org.opensearch.transport.TransportService;

public class TestExtensionsRunner extends OpenSearchTestCase {

    private static final String EXTENSION_NAME = "sample-extension";

    private ExtensionsRunner extensionsRunner;
    private TransportService transportService;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
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
        DiscoveryExtension extension = new DiscoveryExtension(
            EXTENSION_NAME,
            "opensearch-sdk-1",
            "",
            "",
            "",
            sourceNode.getAddress(),
            new HashMap<String, String>(),
            null,
            null
        );

        // Set mocked transport service
        extensionsRunner.setExtensionTransportService(this.transportService);
        doNothing().when(this.transportService).connectToNode(sourceNode);

        InitializeExtensionsRequest extensionInitRequest = new InitializeExtensionsRequest(sourceNode, extension);

        InitializeExtensionsResponse response = extensionsRunner.handleExtensionInitRequest(extensionInitRequest);
        // Test if name and unique ID are set
        assertEquals(EXTENSION_NAME, response.getName());
        assertEquals("opensearch-sdk-1", extensionsRunner.getUniqueId());
        // Test if the source node is set after handleExtensionInitRequest() is called during OpenSearch bootstrap
        assertEquals(sourceNode, extensionsRunner.getOpensearchNode());
    }

    @Test
    public void testHandleOpenSearchRequest() throws Exception {

        OpenSearchRequest request = new OpenSearchRequest(OpenSearchRequestType.REQUEST_OPENSEARCH_NAMED_WRITEABLE_REGISTRY);
        assertEquals(NamedWriteableRegistryResponse.class, extensionsRunner.handleOpenSearchRequest(request).getClass());

        // Add additional OpenSearch request handler tests here for each default extension point
    }

    @Test
    public void testHandleRestExecuteOnExtensionRequest() throws Exception {

        RestExecuteOnExtensionRequest request = new RestExecuteOnExtensionRequest(Method.GET, "/foo");
        RestExecuteOnExtensionResponse response = extensionsRunner.handleRestExecuteOnExtensionRequest(request);
        // this will fail in test environment with no registered actions
        assertEquals(RestStatus.NOT_FOUND, response.getStatus());
        assertEquals(BytesRestResponse.TEXT_CONTENT_TYPE, response.getContentType());
        String responseStr = new String(response.getContent(), StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("GET"));
        assertTrue(responseStr.contains("/foo"));
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
    public void testEnvironmentSettingsRequest() {

        List<String> componentSettingKeys = new ArrayList<>();
        extensionsRunner.sendEnvironmentSettingsRequest(transportService, componentSettingKeys);

        verify(transportService, times(1)).sendRequest(any(), anyString(), any(), any(EnvironmentSettingsResponseHandler.class));
    }

    @Test
    public void testRegisterRestActionsRequest() {

        extensionsRunner.sendRegisterRestActionsRequest(transportService);

        verify(transportService, times(1)).sendRequest(any(), anyString(), any(), any(RegisterRestActionsResponseHandler.class));
    }
}
