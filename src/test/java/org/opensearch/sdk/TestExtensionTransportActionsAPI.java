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
import org.opensearch.Version;
import org.opensearch.action.ActionListener;
import org.opensearch.action.main.MainRequest;
import org.opensearch.action.main.MainResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.sdk.handlers.AcknowledgedResponseHandler;
import org.opensearch.tasks.Task;
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
    private TransportActions transportActions;
    private DiscoveryNode opensearchNode;

    private class TestTransportAction extends HandledTransportAction<MainRequest, MainResponse> {

        protected TestTransportAction(
            String actionName,
            TransportService transportService,
            ActionFilters actionFilters,
            Writeable.Reader<MainRequest> mainRequestReader
        ) {
            super(actionName, transportService, actionFilters, mainRequestReader);
        }

        @Override
        protected void doExecute(Task task, MainRequest request, ActionListener<MainResponse> actionListener) {

        }
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.transportActions = new TransportActions(Map.of("testAction", TestTransportAction.class));
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
        transportActions.sendRegisterTransportActionsRequest(transportService, opensearchNode, "test-extension");
        verify(transportService, times(1)).sendRequest(
            any(),
            eq(ExtensionsManager.REQUEST_EXTENSION_REGISTER_TRANSPORT_ACTIONS),
            any(),
            any(AcknowledgedResponseHandler.class)
        );
    }
}
