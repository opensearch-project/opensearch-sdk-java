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

import static org.mockito.Mockito.times;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensearch.common.settings.Settings;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.TransportService;

import opensearchSDK.transport.ExtensionsRunner;

public class TestMainScript extends OpenSearchTestCase {

    private ExtensionsRunner extensionsRunner;
    private Settings settings;

    @BeforeEach
    public void setUp() throws IOException {

        this.extensionsRunner = new ExtensionsRunner();
        this.settings = Settings.builder().put("node.name", "MainScriptTests").build();
    }

    // test ExtensionsRunner getTransportService return type is transport service
    @Test
    public void testGetTransportService() throws IOException {
        assert (extensionsRunner.getTransportService(settings) instanceof TransportService);
    }

    // test manager method invokes start on transport service
    @Test
    public void testTransportServiceStarted() throws IOException {

        // retrieve and mock transport service
        TransportService transportService = Mockito.spy(extensionsRunner.getTransportService(settings));

        // verify mocked object interaction in manager method
        extensionsRunner.startTransportService(transportService);
        Mockito.verify(transportService, times(1)).start();
    }

    // test manager method invokes accept incoming requests on transport service
    @Test
    public void testTransportServiceAcceptedIncomingRequests() throws IOException {

        // retrieve and mock transport service
        TransportService transportService = Mockito.spy(extensionsRunner.getTransportService(settings));

        // verify mocked object interaction in manager method
        extensionsRunner.startTransportService(transportService);
        Mockito.verify(transportService, times(1)).acceptIncomingRequests();
    }
}
