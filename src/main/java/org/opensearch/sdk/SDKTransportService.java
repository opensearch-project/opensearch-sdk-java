/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.RegisterTransportActionsRequest;
import org.opensearch.extensions.action.RemoteExtensionActionResponse;
import org.opensearch.extensions.action.TransportActionRequestFromExtension;
import org.opensearch.sdk.ActionExtension.ActionHandler;
import org.opensearch.sdk.action.RemoteExtensionActionRequest;
import org.opensearch.sdk.action.SDKActionModule;
import org.opensearch.sdk.handlers.AcknowledgedResponseHandler;
import org.opensearch.sdk.handlers.ExtensionActionResponseHandler;
import org.opensearch.transport.TransportService;

/**
 * Wrapper class for {@link TransportService} and associated methods.
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
     * Requests that OpenSearch register the Transport Actions for this extension.
     *
     * @param actions The map of registered actions from {@link SDKActionModule#getActions()}
     */
    public void sendRegisterTransportActionsRequest(Map<String, ActionHandler<?, ?>> actions) {
        logger.info("Sending Register Transport Actions request to OpenSearch");
        Set<String> actionNameSet = actions.values()
            .stream()
            .filter(h -> !h.getAction().name().startsWith("internal"))
            .map(h -> h.getAction().getClass().getName())
            .collect(Collectors.toSet());
        AcknowledgedResponseHandler registerTransportActionsResponseHandler = new AcknowledgedResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_REGISTER_TRANSPORT_ACTIONS,
                new RegisterTransportActionsRequest(uniqueId, actionNameSet),
                registerTransportActionsResponseHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send Register Transport Actions request to OpenSearch", e);
        }
    }

    /**
     * Requests that OpenSearch execute a Transport Actions on another extension.
     *
     * @param request The request to send
     * @return A buffer serializing the response from the remote action if successful, otherwise null
     */
    public RemoteExtensionActionResponse sendRemoteExtensionActionRequest(RemoteExtensionActionRequest request) {
        logger.info("Sending Remote Extension Action request to OpenSearch for [" + request.getAction() + "]");
        // Combine class name string and request bytes
        byte[] requestClassBytes = request.getRequestClass().getBytes(StandardCharsets.UTF_8);
        byte[] proxyRequestBytes = ByteBuffer.allocate(requestClassBytes.length + 1 + request.getRequestBytes().length)
            .put(requestClassBytes)
            .put((byte) 0)
            .put(request.getRequestBytes())
            .array();
        ExtensionActionResponseHandler extensionActionResponseHandler = new ExtensionActionResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.TRANSPORT_ACTION_REQUEST_FROM_EXTENSION,
                new TransportActionRequestFromExtension(request.getAction(), proxyRequestBytes, uniqueId),
                extensionActionResponseHandler
            );
            // Wait on response
            extensionActionResponseHandler.awaitResponse();
        } catch (TimeoutException e) {
            logger.info("Failed to receive Remote Extension Action response from OpenSearch", e);
        } catch (Exception e) {
            logger.info("Failed to send Remote Extension Action request to OpenSearch", e);
        }
        // At this point, response handler has read in the response bytes
        return new RemoteExtensionActionResponse(
            extensionActionResponseHandler.isSuccess(),
            extensionActionResponseHandler.getResponseBytes()
        );
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
