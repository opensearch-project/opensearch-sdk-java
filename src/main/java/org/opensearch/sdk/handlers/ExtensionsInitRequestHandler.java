/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.handlers;

import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;
import org.opensearch.common.settings.Settings;
import org.opensearch.discovery.InitializeExtensionRequest;
import org.opensearch.discovery.InitializeExtensionResponse;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.transport.TransportService;

import static org.opensearch.sdk.ExtensionsRunner.NODE_NAME_SETTING;

/**
 * This class handles the request from OpenSearch to a {@link ExtensionsRunner#startTransportService(TransportService transportService)} call.
 */

public class ExtensionsInitRequestHandler {
    private static final Logger logger = LogManager.getLogger(ExtensionsInitRequestHandler.class);

    private final ExtensionsRunner extensionsRunner;

    /**
     * Instantiate this object with a reference to the ExtensionsRunner
     *
     * @param extensionsRunner the ExtensionsRunner instance
     */
    public ExtensionsInitRequestHandler(ExtensionsRunner extensionsRunner) {
        this.extensionsRunner = extensionsRunner;
    }

    /**
     * Handles a extension request from OpenSearch. This is the first request for the transport communication and will initialize the extension and will be a part of OpenSearch bootstrap.
     *
     * @param extensionInitRequest  The request to handle.
     * @return A response to OpenSearch validating that this is an extension.
     */
    public InitializeExtensionResponse handleExtensionInitRequest(InitializeExtensionRequest extensionInitRequest) {
        logger.info("Registering Extension Request received from OpenSearch");
        extensionsRunner.opensearchNode = extensionInitRequest.getSourceNode();
        extensionsRunner.setUniqueId(extensionInitRequest.getExtension().getId());
        // Successfully initialized. Send the response.
        try {
            return new InitializeExtensionResponse(
                extensionsRunner.getSettings().get(NODE_NAME_SETTING),
                extensionsRunner.getExtensionImplementedInterfaces()
            );
        } finally {
            // After sending successful response to initialization, send the REST API and Settings
            extensionsRunner.setOpensearchNode(extensionsRunner.opensearchNode);
            extensionsRunner.setExtensionNode(extensionInitRequest.getExtension());
            TransportService extensionTransportService = extensionsRunner.getExtensionTransportService();
            extensionTransportService.connectToNode(extensionsRunner.opensearchNode);
            extensionsRunner.sendRegisterRestActionsRequest(extensionTransportService);
            extensionsRunner.sendRegisterCustomSettingsRequest(extensionTransportService);
            extensionsRunner.getSdkActionModule()
                .sendRegisterTransportActionsRequest(
                    extensionTransportService,
                    extensionsRunner.opensearchNode,
                    extensionsRunner.getUniqueId()
                );
            // Get OpenSearch Settings and set values on ExtensionsRunner
            Settings settings = extensionsRunner.sendEnvironmentSettingsRequest(extensionTransportService);
            extensionsRunner.setEnvironmentSettings(settings);
            extensionsRunner.updateNamedXContentRegistry();
            extensionsRunner.updateSdkClusterService();

            // Last step of initialization
            // TODO: make sure all the other sendX methods have completed
            // https://github.com/opensearch-project/opensearch-sdk-java/issues/17
            extensionsRunner.setInitialized();

            // Trigger pending updates requiring completion of the above actions
            extensionsRunner.getSdkClusterService().getClusterSettings().sendPendingSettingsUpdateConsumers();
        }
    }
}
