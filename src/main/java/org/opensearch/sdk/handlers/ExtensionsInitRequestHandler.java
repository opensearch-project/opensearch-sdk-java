/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.discovery.InitializeExtensionsRequest;
import org.opensearch.discovery.InitializeExtensionsResponse;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.transport.TransportService;

/**
 * This class handles the request from OpenSearch to a {@link ExtensionsRunner#startTransportService(TransportService transportService)} call.
 */

public class ExtensionsInitRequestHandler {
    private static final Logger logger = LogManager.getLogger(ExtensionsInitRequestHandler.class);
    private static final String NODE_NAME_SETTING = "node.name";

    /**
     * Handles a extension request from OpenSearch.  This is the first request for the transport communication and will initialize the extension and will be a part of OpenSearch bootstrap.
     *
     * @param extensionInitRequest  The request to handle.
     * @param extensionsRunner The method call from handler.
     * @return A response to OpenSearch validating that this is an extension.
     */
    public InitializeExtensionsResponse handleExtensionInitRequest(
        InitializeExtensionsRequest extensionInitRequest,
        ExtensionsRunner extensionsRunner
    ) {
        logger.info("Registering Extension Request received from OpenSearch");
        extensionsRunner.opensearchNode = extensionInitRequest.getSourceNode();
        extensionsRunner.setUniqueId(extensionInitRequest.getExtension().getId());
        // Successfully initialized. Send the response.
        try {
            return new InitializeExtensionsResponse(extensionsRunner.settings.get(NODE_NAME_SETTING));
        } finally {
            // After sending successful response to initialization, send the REST API and Settings
            extensionsRunner.setOpensearchNode(extensionsRunner.opensearchNode);
            extensionsRunner.setExtensionNode(extensionInitRequest.getExtension());
            extensionsRunner.extensionTransportService.connectToNode(extensionsRunner.opensearchNode);
            extensionsRunner.sendRegisterRestActionsRequest(extensionsRunner.extensionTransportService);
            extensionsRunner.sendRegisterCustomSettingsRequest(extensionsRunner.extensionTransportService);
            extensionsRunner.transportActions.sendRegisterTransportActionsRequest(
                extensionsRunner.extensionTransportService,
                extensionsRunner.opensearchNode,
                extensionsRunner.getUniqueId()
            );
        }
    }
}
