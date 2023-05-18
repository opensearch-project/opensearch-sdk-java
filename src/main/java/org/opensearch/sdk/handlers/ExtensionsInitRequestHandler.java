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
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.discovery.InitializeExtensionRequest;
import org.opensearch.discovery.InitializeExtensionResponse;
import org.opensearch.extensions.DiscoveryExtensionNode;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.SDKTransportService;
import org.opensearch.transport.TransportService;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.opensearch.sdk.ExtensionsRunner.NODE_NAME_SETTING;
import static org.opensearch.sdk.ExtensionsRunner.OPENSEARCH_HOST_SETTING;
import static org.opensearch.sdk.ExtensionsRunner.OPENSEARCH_PORT_SETTING;

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
        DiscoveryNode sourceNode = extensionInitRequest.getSourceNode();
        DiscoveryExtensionNode extensionNode = extensionInitRequest.getExtension();
        logger.info("Registering Extension Request received from OpenSearch node {}", sourceNode);
        // In some cases the source node has a localhost IP address which is unreachable from extension.
        String sourceHost = sourceNode.getAddress().address().getHostString();
        if ("127.0.0.1".equals(sourceHost)) {
            // pull from ExtensionSettings instead
            sourceHost = extensionsRunner.getSettings().get(OPENSEARCH_HOST_SETTING);
            try {
                int sourcePort = Integer.valueOf(extensionsRunner.getSettings().get(OPENSEARCH_PORT_SETTING));
                sourceNode = new DiscoveryNode(
                    sourceNode.getName(),
                    new TransportAddress(InetAddress.getByName(sourceHost), sourcePort),
                    sourceNode.getVersion()
                );
            } catch (NumberFormatException | UnknownHostException e) {
                // If configured defaults don't work, just keep original source node
            }
        }
        extensionsRunner.opensearchNode = sourceNode;
        extensionsRunner.getThreadPool().getThreadContext().putHeader("extension_unique_id", extensionNode.getId());
        extensionsRunner.setUniqueId(extensionNode.getId());
        // TODO: Remove above lines setting extensionRunner node and ID in favor of the below when refactoring
        // per https://github.com/opensearch-project/opensearch-sdk-java/issues/585
        SDKTransportService sdkTransportService = extensionsRunner.getSdkTransportService();
        sdkTransportService.setOpensearchNode(sourceNode);
        sdkTransportService.setUniqueId(extensionNode.getId());
        // Successfully initialized. Send the response.
        try {
            return new InitializeExtensionResponse(
                extensionsRunner.getSettings().get(NODE_NAME_SETTING),
                extensionsRunner.getExtensionImplementedInterfaces()
            );
        } finally {
            // After sending successful response to initialization, send the REST API and Settings
            extensionsRunner.setOpensearchNode(sourceNode);
            extensionsRunner.setExtensionNode(extensionInitRequest.getExtension());
            extensionsRunner.getSdkClient().updateOpenSearchNodeSettings(extensionInitRequest.getSourceNode().getAddress());

            // TODO: replace with sdkTransportService.getTransportService()
            TransportService extensionTransportService = extensionsRunner.getExtensionTransportService();
            extensionTransportService.connectToNodeAsExtension(
                extensionInitRequest.getSourceNode(),
                extensionInitRequest.getExtension().getId()
            );
            extensionsRunner.sendRegisterRestActionsRequest(extensionTransportService);
            extensionsRunner.sendRegisterCustomSettingsRequest(extensionTransportService);
            sdkTransportService.sendRegisterTransportActionsRequest(extensionsRunner.getSdkActionModule().getActions());
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
