/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.handlers;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.settings.Settings;
import org.opensearch.discovery.InitializeExtensionsRequest;
import org.opensearch.discovery.InitializeExtensionsResponse;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.TransportActions;
import org.opensearch.transport.TransportService;

/**
 * This class handles the request from OpenSearch to a {@link ExtensionsRunner#startTransportService(TransportService transportService)} call.
 */

public class ExtensionsInitRequestHandler {
    private static final Logger logger = LogManager.getLogger(ExtensionsInitRequestHandler.class);
    private static final String NODE_NAME_SETTING = "node.name";
    private static DiscoveryNode opensearchNode;
    private static Settings settings;
    private static String uniqueId;
    private static TransportService extensionTransportService = null;
    private static TransportActions transportActions = new TransportActions(new HashMap<>());

    private static void setUniqueId(String id) {
        ExtensionsInitRequestHandler.uniqueId = id;
    }

    String getUniqueId() {
        return uniqueId;
    }

    private static void setOpensearchNode(DiscoveryNode opensearchNode) {
        ExtensionsInitRequestHandler.opensearchNode = opensearchNode;
    }

    DiscoveryNode getOpensearchNode() {
        return opensearchNode;
    }

    /**
     * Handles a extension request from OpenSearch.  This is the first request for the transport communication and will initialize the extension and will be a part of OpenSearch bootstrap.
     *
     * @param extensionInitRequest  The request to handle.
     * @return A response to OpenSearch validating that this is an extension.
     */
    public static InitializeExtensionsResponse handleExtensionInitRequest(InitializeExtensionsRequest extensionInitRequest) {
        logger.info("Registering Extension Request received from OpenSearch");
        opensearchNode = extensionInitRequest.getSourceNode();
        setUniqueId(extensionInitRequest.getExtension().getId());
        // Successfully initialized. Send the response.
        try {
            return new InitializeExtensionsResponse(settings.get(NODE_NAME_SETTING));
        } finally {
            // After sending successful response to initialization, send the REST API
            setOpensearchNode(opensearchNode);
            extensionTransportService.connectToNode(opensearchNode);
            ExtensionsRunner.sendRegisterRestActionsRequest(extensionTransportService);
            transportActions.sendRegisterTransportActionsRequest(extensionTransportService, opensearchNode);
        }
    }
}
