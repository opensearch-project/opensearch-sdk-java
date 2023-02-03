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
import org.opensearch.sdk.SDKClusterService.SDKClusterSettings;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.TransportService;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        sdkClusterService.state();
        verify(extensionsRunner, times(1)).getExtensionTransportService();

        ArgumentCaptor<TransportService> argumentCaptor = ArgumentCaptor.forClass(TransportService.class);
        verify(extensionsRunner, times(1)).sendClusterStateRequest(argumentCaptor.capture());
        assertNull(argumentCaptor.getValue());
    }

    @Test
    public void testGetClusterSettings() {
        assertInstanceOf(SDKClusterSettings.class, sdkClusterService.getClusterSettings());
    }

    @Test
    public void testAddSettingsUpdateConsumer() throws Exception {
        Setting<Boolean> boolSetting = Setting.boolSetting("test", false);
        Consumer<Boolean> boolConsumer = b -> {};
        sdkClusterService.getClusterSettings().addSettingsUpdateConsumer(boolSetting, boolConsumer);
        verify(extensionsRunner, times(1)).getExtensionTransportService();

        ArgumentCaptor<TransportService> transportServiceArgumentCaptor = ArgumentCaptor.forClass(TransportService.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Setting<?>, Consumer<?>>> updateConsumerArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(extensionsRunner, times(1)).sendAddSettingsUpdateConsumerRequest(
            transportServiceArgumentCaptor.capture(),
            updateConsumerArgumentCaptor.capture()
        );
        assertNull(transportServiceArgumentCaptor.getValue());
        assertEquals(Map.of(boolSetting, boolConsumer), updateConsumerArgumentCaptor.getValue());
    }
}
