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
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.TransportService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
}
