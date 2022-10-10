/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.support.TransportAction;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.extensions.ExtensionsOrchestrator;
import org.opensearch.extensions.RegisterTransportActionsRequest;
import org.opensearch.sdk.handlers.ExtensionBooleanResponseHandler;
import org.opensearch.transport.TransportService;

import java.util.HashMap;
import java.util.Map;

/**
 * This class helps manage transport actions for SDK
 */
public class TransportActions {
    private final Logger logger = LogManager.getLogger(TransportActions.class);
    private Map<String, Class> transportActions;

    /**
     * Constructor for TransportActions. Creates a map of transportActions for this extension.
     *
     * @param <Request> the TransportAction request
     * @param <Response> the TransportAction response
     * @param transportActions is the list of actions the extension would like to register with OpenSearch.
     */
    public <Request extends ActionRequest, Response extends ActionResponse> TransportActions(
        Map<String, Class<? extends TransportAction<Request, Response>>> transportActions
    ) {
        this.transportActions = new HashMap<>(transportActions);
    }

    /**
     * Requests that OpenSearch register the Transport Actions for this extension.
     *
     * @param transportService  The TransportService defining the connection to OpenSearch.
     * @param opensearchNode The OpenSearch node where transport actions being registered.
     * @param uniqueId The identity used to
     */
    public void sendRegisterTransportActionsRequest(TransportService transportService, DiscoveryNode opensearchNode, String uniqueId) {
        logger.info("Sending Register Transport Actions request to OpenSearch");
        ExtensionBooleanResponseHandler registerTransportActionsResponseHandler = new ExtensionBooleanResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsOrchestrator.REQUEST_EXTENSION_REGISTER_TRANSPORT_ACTIONS,
                new RegisterTransportActionsRequest(uniqueId, transportActions),
                registerTransportActionsResponseHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send Register Transport Actions request to OpenSearch", e);
        }
    }
}
