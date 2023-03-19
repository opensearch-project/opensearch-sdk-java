/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.TransportActionRequestFromExtension;
import org.opensearch.sdk.handlers.TransportActionResponseToExtensionResponseHandler;
import org.opensearch.transport.TransportService;

/**
 * Wrapper class for {@link TranspoortService} and associated methods.
 *
 * TODO: Move all the sendFooRequest() methods here
 * TODO: Replace usages of getExtensionTransportService with this class
 */
public class SDKTransportService {
    private final Logger logger = LogManager.getLogger(SDKTransportService.class);

    private TransportService transportService;
    private DiscoveryNode opensearchNode;
    private String uniqueId;

    /**
     * Requests that OpenSearch execute a Transport Actions on another extension.
     *
     * @param action The fully qualified class name of the action to execute
     * @param requestBytes A buffer containing serialized parameters to be understood by the remote action
     * @return A buffer serializing the response from the remote action if successful, otherwise empty
     */
    public byte[] sendProxyActionRequest(String action, byte[] requestBytes) {
        logger.info("Sending ProxyAction request to OpenSearch for [" + action + "]");
        TransportActionResponseToExtensionResponseHandler handleTransportActionResponseHandler =
            new TransportActionResponseToExtensionResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_HANDLE_TRANSPORT_ACTION,
                new TransportActionRequestFromExtension(action, requestBytes, uniqueId),
                handleTransportActionResponseHandler
            );
            // Wait on response
            handleTransportActionResponseHandler.awaitResponse();
        } catch (TimeoutException e) {
            logger.info("Failed to receive ProxyAction response from OpenSearch", e);
        } catch (Exception e) {
            logger.info("Failed to send ProxyAction request to OpenSearch", e);
        }
        // At this point, response handler has read in the response bytes
        return handleTransportActionResponseHandler.getResponseBytes();
    }

    public TransportService getTransportService() {
        return transportService;
    }

    public DiscoveryNode getOpensearchNode() {
        return opensearchNode;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setTransportService(TransportService transportService) {
        this.transportService = transportService;
    }

    public void setOpensearchNode(DiscoveryNode opensearchNode) {
        this.opensearchNode = opensearchNode;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}
