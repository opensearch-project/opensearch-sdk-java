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
import org.opensearch.common.Nullable;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.TransportActionRequestFromExtension;
import org.opensearch.sdk.action.ProxyActionRequest;
import org.opensearch.sdk.handlers.TransportActionResponseToExtensionResponseHandler;
import org.opensearch.transport.TransportService;

/**
 * Wrapper class for {@link TranspoortService} and associated methods.
 *
 * TODO: Move all the sendFooRequest() methods here
 * TODO: Replace usages of getExtensionTransportService with this class
 * https://github.com/opensearch-project/opensearch-sdk-java/issues/585
 */
public class SDKTransportService {
    private final Logger logger = LogManager.getLogger(SDKTransportService.class);

    private TransportService transportService;
    private DiscoveryNode opensearchNode;
    private String uniqueId;

    /**
     * Requests that OpenSearch execute a Transport Actions on another extension.
     *
     * @param request The request to send
     * @return A buffer serializing the response from the remote action if successful, otherwise null
     */
    @Nullable
    public byte[] sendProxyActionRequest(ProxyActionRequest request) {
        logger.info("Sending ProxyAction request to OpenSearch for [" + request.getAction() + "]");
        TransportActionResponseToExtensionResponseHandler handleTransportActionResponseHandler =
            new TransportActionResponseToExtensionResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_HANDLE_TRANSPORT_ACTION,
                new TransportActionRequestFromExtension(request.getAction(), request.getRequestBytes(), uniqueId),
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
