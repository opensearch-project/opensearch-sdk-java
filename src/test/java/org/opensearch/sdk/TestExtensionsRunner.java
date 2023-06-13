/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.Version;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.bytes.BytesArray;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.Setting.Property;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.discovery.InitializeExtensionRequest;
import org.opensearch.discovery.InitializeExtensionResponse;
import org.opensearch.extensions.DiscoveryExtensionNode;
import org.opensearch.extensions.AcknowledgedResponse;
import org.opensearch.extensions.ExtensionDependency;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.extensions.rest.RestExecuteOnExtensionResponse;
import org.opensearch.http.HttpRequest;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.rest.RestStatus;
import org.opensearch.common.settings.Setting;
import org.opensearch.extensions.UpdateSettingsRequest;
import org.opensearch.sdk.handlers.ClusterSettingsResponseHandler;
import org.opensearch.sdk.handlers.ClusterStateResponseHandler;
import org.opensearch.sdk.handlers.EnvironmentSettingsResponseHandler;
import org.opensearch.sdk.handlers.ExtensionsInitRequestHandler;
import org.opensearch.sdk.handlers.ExtensionsRestRequestHandler;
import org.opensearch.sdk.rest.ExtensionRestPathRegistry;
import org.opensearch.sdk.handlers.AcknowledgedResponseHandler;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.Transport;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.TransportSettings;
import org.opensearch.common.settings.WriteableSetting;

public class TestExtensionsRunner extends OpenSearchTestCase {

    private static final String EXTENSION_NAME = "sample-extension";
    private ExtensionsInitRequestHandler extensionsInitRequestHandler;
    private ExtensionsRestRequestHandler extensionsRestRequestHandler = new ExtensionsRestRequestHandler(
        new ExtensionRestPathRegistry(),
        SDKNamedXContentRegistry.EMPTY
    );
    private ExtensionsRunner extensionsRunner;
    private SDKTransportService sdkTransportService;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.extensionsRunner = new ExtensionsRunnerForTest();
        extensionsRunner.getSdkTransportService().setUniqueId("opensearch-sdk-1");
        this.extensionsInitRequestHandler = new ExtensionsInitRequestHandler(extensionsRunner);
        this.sdkTransportService = extensionsRunner.getSdkTransportService();
        this.sdkTransportService.setTransportService(
            spy(
                new TransportService(
                    Settings.EMPTY,
                    mock(Transport.class),
                    null,
                    TransportService.NOOP_TRANSPORT_INTERCEPTOR,
                    x -> null,
                    null,
                    Collections.emptySet()
                )
            )
        );
    }

    @Test
    public void testStartTransportService() {
        extensionsRunner.startTransportService(sdkTransportService.getTransportService());
        // test manager method invokes start on transport service
        verify(sdkTransportService.getTransportService(), times(1)).start();
        // cannot verify acceptIncomingRequests as it is a final method
        // test registerRequestHandlers
        verify(sdkTransportService.getTransportService(), times(5)).registerRequestHandler(
            anyString(),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(),
            any()
        );
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
        DiscoveryExtensionNode extension = new DiscoveryExtensionNode(
            EXTENSION_NAME,
            "opensearch-sdk-1",
            sourceNode.getAddress(),
            new HashMap<String, String>(),
            Version.fromString("3.0.0"),
            Version.fromString("3.0.0"),
            new ArrayList<ExtensionDependency>()
        );

        doNothing().when(sdkTransportService.getTransportService()).connectToNodeAsExtension(sourceNode, "opensearch-sdk-1");

        InitializeExtensionRequest extensionInitRequest = new InitializeExtensionRequest(sourceNode, extension);

        InitializeExtensionResponse response = extensionsInitRequestHandler.handleExtensionInitRequest(extensionInitRequest);
        // Test if name and unique ID are set
        assertEquals(EXTENSION_NAME, response.getName());
        assertEquals("opensearch-sdk-1", extensionsRunner.getSdkTransportService().getUniqueId());
        // Test if the source node is set after handleExtensionInitRequest() is called during OpenSearch bootstrap
        assertEquals(sourceNode, extensionsRunner.getSdkTransportService().getOpensearchNode());
    }

    @Test
    public void testHandleExtensionRestRequest() throws Exception {

        String ext = "token_placeholder";
        @SuppressWarnings("unused") // placeholder to test the token when identity features merged
        Principal userPrincipal = () -> "user1";
        HttpRequest.HttpVersion httpVersion = HttpRequest.HttpVersion.HTTP_1_1;
        ExtensionRestRequest request = new ExtensionRestRequest(
            Method.GET,
            "/foo",
            "/foo",
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            new BytesArray("bar"),
            ext,
            httpVersion
        );
        RestExecuteOnExtensionResponse response = extensionsRestRequestHandler.handleRestExecuteOnExtensionRequest(request);
        // this will fail in test environment with no registered actions
        assertEquals(RestStatus.NOT_FOUND, response.getStatus());
        assertEquals(BytesRestResponse.TEXT_CONTENT_TYPE, response.getContentType());
        String responseStr = new String(response.getContent(), StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("GET"));
        assertTrue(responseStr.contains("/foo"));
    }

    @Test
    public void testHandleUpdateSettingsRequest() throws Exception {

        Setting<Integer> fallbackSetting = Setting.intSetting("component.fallback.setting.key", 0, 0, Property.Dynamic);
        Setting<Integer> componentSetting = Setting.intSetting("component.setting.key", fallbackSetting, Property.Dynamic);
        UpdateSettingsRequest request = new UpdateSettingsRequest(WriteableSetting.SettingType.Integer, componentSetting, null);
        assertEquals(
            AcknowledgedResponse.class,
            extensionsRunner.updateSettingsRequestHandler.handleUpdateSettingsRequest(request).getClass()
        );
    }

    @Test
    public void testClusterStateRequest() {

        sdkTransportService.sendClusterStateRequest();

        verify(sdkTransportService.getTransportService(), times(1)).sendRequest(
            any(),
            anyString(),
            any(),
            any(ClusterStateResponseHandler.class)
        );
    }

    @Test
    public void testClusterSettingRequest() {

        sdkTransportService.sendClusterSettingsRequest();

        verify(sdkTransportService.getTransportService(), times(1)).sendRequest(
            any(),
            anyString(),
            any(),
            any(ClusterSettingsResponseHandler.class)
        );
    }

    @Test
    public void testEnvironmentSettingsRequest() {
        sdkTransportService.sendEnvironmentSettingsRequest();

        verify(sdkTransportService.getTransportService(), times(1)).sendRequest(
            any(),
            anyString(),
            any(),
            any(EnvironmentSettingsResponseHandler.class)
        );
    }

    @Test
    public void testRegisterRestActionsRequest() {

        sdkTransportService.sendRegisterRestActionsRequest(extensionsRunner.getExtensionRestPathRegistry());

        verify(sdkTransportService.getTransportService(), times(1)).sendRequest(
            any(),
            anyString(),
            any(),
            any(AcknowledgedResponseHandler.class)
        );
    }

    @Test
    public void testRegisterCustomSettingsRequest() {
        sdkTransportService.sendRegisterCustomSettingsRequest(extensionsRunner.getCustomSettings());

        verify(sdkTransportService.getTransportService(), times(1)).sendRequest(
            any(),
            anyString(),
            any(),
            any(AcknowledgedResponseHandler.class)
        );
    }

    @Test
    public void testGettersAndSetters() throws IOException {
        assertFalse(extensionsRunner.isInitialized());
        extensionsRunner.setInitialized();
        assertTrue(extensionsRunner.isInitialized());

        Settings settings = Settings.builder().put("test.key", "test.value").build();
        assertTrue(extensionsRunner.getEnvironmentSettings().isEmpty());
        extensionsRunner.setEnvironmentSettings(settings);
        assertEquals("test.value", extensionsRunner.getEnvironmentSettings().get("test.key"));

        assertTrue(extensionsRunner.getCustomNamedXContent().isEmpty());
        assertNotNull(extensionsRunner.getNamedXContentRegistry().getRegistry());
        extensionsRunner.updateNamedXContentRegistry();
        assertNotNull(extensionsRunner.getNamedXContentRegistry().getRegistry());
        assertTrue(extensionsRunner.getExtension() instanceof BaseExtension);
        assertEquals(extensionsRunner, ((BaseExtension) extensionsRunner.getExtension()).extensionsRunner());
        assertNotNull(extensionsRunner.getThreadPool());
        assertNotNull(extensionsRunner.getTaskManager());
        assertNotNull(extensionsRunner.getSdkClient());
        assertNotNull(extensionsRunner.getSdkClusterService());
        assertNotNull(extensionsRunner.getSdkTransportService());
        assertNotNull(extensionsRunner.getIndexNameExpressionResolver());

        settings = extensionsRunner.getSettings();
        assertEquals(ExtensionsRunnerForTest.NODE_NAME, settings.get(ExtensionsRunner.NODE_NAME_SETTING));
        assertEquals(ExtensionsRunnerForTest.NODE_HOST, settings.get(TransportSettings.BIND_HOST.getKey()));
        assertEquals(ExtensionsRunnerForTest.NODE_PORT, settings.get(TransportSettings.PORT.getKey()));
    }

    @Test
    public void testGetExtensionImplementedInterfaces() {
        List<String> implementedInterfaces = extensionsRunner.getExtensionImplementedInterfaces();
        assertFalse(implementedInterfaces.isEmpty());
        assertTrue(implementedInterfaces.contains("Extension"));
    }

}
