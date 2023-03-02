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
import org.opensearch.common.settings.Setting;
import org.opensearch.extensions.DiscoveryExtensionNode;
import org.opensearch.sdk.SDKClusterService.SDKClusterSettings;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.TransportService;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TestSDKClusterService extends OpenSearchTestCase {
    private ExtensionsRunner extensionsRunner;
    private SDKClusterService sdkClusterService;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.extensionsRunner = mock(ExtensionsRunner.class);
        this.sdkClusterService = new SDKClusterService(extensionsRunner);
    }

    @Test
    public void testState() throws Exception {
        // Before initialization should throw exception
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> sdkClusterService.state());
        assertEquals("The Extensions Runner has not been initialized.", ex.getMessage());

        // After initialization should be successful
        when(extensionsRunner.isInitialized()).thenReturn(true);
        sdkClusterService.state();
        verify(extensionsRunner, times(1)).getExtensionTransportService();

        ArgumentCaptor<TransportService> argumentCaptor = ArgumentCaptor.forClass(TransportService.class);
        verify(extensionsRunner, times(1)).sendClusterStateRequest(argumentCaptor.capture());
        assertNull(argumentCaptor.getValue());
    }

    @Test
    public void testLocalNode() {
        DiscoveryExtensionNode expectedLocalNode = extensionsRunner.getExtensionNode();
        DiscoveryExtensionNode localNode = sdkClusterService.localNode();
        assertEquals(expectedLocalNode, localNode);
    }

    @Test
    public void testGetClusterSettings() {
        assertInstanceOf(SDKClusterSettings.class, sdkClusterService.getClusterSettings());
    }

    @Test
    public void testAddSettingsUpdateConsumer() throws Exception {
        Setting<Boolean> boolSetting = Setting.boolSetting("test", false);
        Consumer<Boolean> boolConsumer = b -> {};

        // Before initialization should store pending update but do nothing
        sdkClusterService.getClusterSettings().addSettingsUpdateConsumer(boolSetting, boolConsumer);
        verify(extensionsRunner, times(0)).getExtensionTransportService();

        // After initialization should be able to send pending updates
        when(extensionsRunner.isInitialized()).thenReturn(true);
        sdkClusterService.getClusterSettings().sendPendingSettingsUpdateConsumers();
        verify(extensionsRunner, times(1)).getExtensionTransportService();

        // Once updates sent, map is empty, shouldn't send on retry (keep cumulative 1)
        sdkClusterService.getClusterSettings().sendPendingSettingsUpdateConsumers();
        verify(extensionsRunner, times(1)).getExtensionTransportService();

        // Sending a new update should send immediately (cumulative now 2)
        sdkClusterService.getClusterSettings().addSettingsUpdateConsumer(boolSetting, boolConsumer);
        verify(extensionsRunner, times(2)).getExtensionTransportService();

        ArgumentCaptor<TransportService> transportServiceArgumentCaptor = ArgumentCaptor.forClass(TransportService.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Setting<?>, Consumer<?>>> updateConsumerArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(extensionsRunner, times(2)).sendAddSettingsUpdateConsumerRequest(
            transportServiceArgumentCaptor.capture(),
            updateConsumerArgumentCaptor.capture()
        );
        assertNull(transportServiceArgumentCaptor.getValue());
        // Map will be cleared following this call
        assertTrue(updateConsumerArgumentCaptor.getValue().isEmpty());
    }

    @Test
    public void testAddSettingsUpdateConsumerMap() throws Exception {
        Setting<Boolean> boolSetting = Setting.boolSetting("test", false);
        Consumer<Boolean> boolConsumer = b -> {};
        Map<Setting<?>, Consumer<?>> settingsUpdateConsumersMap = new HashMap<>();
        settingsUpdateConsumersMap.put(boolSetting, boolConsumer);

        // Before initialization should store pending update but do nothing
        sdkClusterService.getClusterSettings().addSettingsUpdateConsumer(settingsUpdateConsumersMap);
        verify(extensionsRunner, times(0)).getExtensionTransportService();

        // After initialization should be able to send pending updates
        when(extensionsRunner.isInitialized()).thenReturn(true);
        sdkClusterService.getClusterSettings().sendPendingSettingsUpdateConsumers();
        verify(extensionsRunner, times(1)).getExtensionTransportService();

        // Once updates sent, map is empty, shouldn't send on retry (keep cumulative 1)
        sdkClusterService.getClusterSettings().sendPendingSettingsUpdateConsumers();
        verify(extensionsRunner, times(1)).getExtensionTransportService();

        // Sending a new update should send immediately (cumulative now 2)
        sdkClusterService.getClusterSettings().addSettingsUpdateConsumer(settingsUpdateConsumersMap);
        verify(extensionsRunner, times(2)).getExtensionTransportService();

        ArgumentCaptor<TransportService> transportServiceArgumentCaptor = ArgumentCaptor.forClass(TransportService.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Setting<?>, Consumer<?>>> updateConsumerArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(extensionsRunner, times(2)).sendAddSettingsUpdateConsumerRequest(
            transportServiceArgumentCaptor.capture(),
            updateConsumerArgumentCaptor.capture()
        );
        assertNull(transportServiceArgumentCaptor.getValue());
        // Map will be cleared following this call
        assertTrue(updateConsumerArgumentCaptor.getValue().isEmpty());
    }
}
