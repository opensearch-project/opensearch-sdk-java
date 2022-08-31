package org.opensearch.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.Version;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.extensions.ExtensionsOrchestrator;
import org.opensearch.sdk.api.TransportActionsAPI;
import org.opensearch.sdk.handlers.GenericResponseHandler;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.transport.Transport;
import org.opensearch.transport.TransportService;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestExtensionTransportActionsAPI extends OpenSearchTestCase {

    private TransportService transportService;
    private TransportActionsAPI transportActionsAPI;
    private DiscoveryNode opensearchNode;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.transportActionsAPI = new TransportActionsAPI(Map.of("testAction", Map.class));
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
    }

    @Test
    public void testRegisterTransportAction() {
        transportActionsAPI.sendRegisterTransportActionsRequest(transportService, opensearchNode);
        verify(transportService, times(1)).sendRequest(
            any(),
            eq(ExtensionsOrchestrator.REQUEST_EXTENSION_REGISTER_TRANSPORT_ACTIONS),
            any(),
            any(GenericResponseHandler.class)
        );
    }
}
