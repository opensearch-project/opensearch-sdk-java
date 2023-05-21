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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.extensions.AddSettingsUpdateConsumerRequest;
import org.opensearch.extensions.DiscoveryExtensionNode;
import org.opensearch.extensions.ExtensionRequest;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.RegisterTransportActionsRequest;
import org.opensearch.extensions.action.RemoteExtensionActionResponse;
import org.opensearch.extensions.action.TransportActionRequestFromExtension;
import org.opensearch.extensions.proto.ExtensionRequestProto;
import org.opensearch.extensions.rest.RegisterRestActionsRequest;
import org.opensearch.extensions.settings.RegisterCustomSettingsRequest;
import org.opensearch.sdk.api.ActionExtension.ActionHandler;
import org.opensearch.sdk.action.RemoteExtensionActionRequest;
import org.opensearch.sdk.action.SDKActionModule;
import org.opensearch.sdk.handlers.AcknowledgedResponseHandler;
import org.opensearch.sdk.handlers.ClusterSettingsResponseHandler;
import org.opensearch.sdk.handlers.ClusterStateResponseHandler;
import org.opensearch.sdk.handlers.EnvironmentSettingsResponseHandler;
import org.opensearch.sdk.handlers.ExtensionActionResponseHandler;
import org.opensearch.sdk.handlers.ExtensionDependencyResponseHandler;
import org.opensearch.sdk.handlers.UpdateSettingsRequestHandler;
import org.opensearch.sdk.rest.ExtensionRestPathRegistry;
import org.opensearch.transport.TransportResponse;
import org.opensearch.transport.TransportResponseHandler;
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
            logger.error("Failed to send Register Transport Actions request to OpenSearch", e);
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
            .put(RemoteExtensionActionRequest.UNIT_SEPARATOR)
            .put(request.getRequestBytes())
            .array();
        ExtensionActionResponseHandler extensionActionResponseHandler = new ExtensionActionResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.TRANSPORT_ACTION_REQUEST_FROM_EXTENSION,
                new TransportActionRequestFromExtension(request.getAction(), ByteString.copyFrom(proxyRequestBytes), uniqueId),
                extensionActionResponseHandler
            );
            // Wait on response
            extensionActionResponseHandler.awaitResponse();
        } catch (TimeoutException e) {
            logger.error("Failed to receive Remote Extension Action response from OpenSearch", e);
        } catch (Exception e) {
            logger.error("Failed to send Remote Extension Action request to OpenSearch", e);
        }
        // At this point, response handler has read in the response bytes
        return new RemoteExtensionActionResponse(
            extensionActionResponseHandler.isSuccess(),
            extensionActionResponseHandler.getResponseBytes()
        );
    }

    /**
     * Requests that OpenSearch register the REST Actions for this extension.
     */
    public void sendRegisterRestActionsRequest(ExtensionRestPathRegistry extensionRestPathRegistry) {
        List<String> extensionRestPaths = extensionRestPathRegistry.getRegisteredPaths();
        List<String> extensionDeprecatedRestPaths = extensionRestPathRegistry.getRegisteredDeprecatedPaths();
        logger.info(
            "Sending Register REST Actions request to OpenSearch for "
                + extensionRestPaths
                + " and deprecated paths "
                + extensionDeprecatedRestPaths
        );
        AcknowledgedResponseHandler registerActionsResponseHandler = new AcknowledgedResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_REGISTER_REST_ACTIONS,
                new RegisterRestActionsRequest(uniqueId, extensionRestPaths, extensionDeprecatedRestPaths),
                registerActionsResponseHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send Register REST Actions request to OpenSearch", e);
        }
    }

    /**
     * Requests that OpenSearch register the custom settings for this extension.
     */
    public void sendRegisterCustomSettingsRequest(List<Setting<?>> customSettings) {
        logger.info("Sending Settings request to OpenSearch");
        AcknowledgedResponseHandler registerCustomSettingsResponseHandler = new AcknowledgedResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_REGISTER_CUSTOM_SETTINGS,
                new RegisterCustomSettingsRequest(uniqueId, customSettings),
                registerCustomSettingsResponseHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send Register Settings request to OpenSearch", e);
        }
    }

    private void sendGenericRequestWithExceptionHandling(
        ExtensionRequestProto.RequestType requestType,
        String orchestratorNameString,
        TransportResponseHandler<? extends TransportResponse> responseHandler
    ) {
        logger.info("Sending " + requestType + " request to OpenSearch");
        try {
            transportService.sendRequest(opensearchNode, orchestratorNameString, new ExtensionRequest(requestType), responseHandler);
        } catch (Exception e) {
            logger.info("Failed to send " + requestType + " request to OpenSearch", e);
        }
    }

    /**
     * Requests the cluster state from OpenSearch.  The result will be handled by a {@link ClusterStateResponseHandler}.
     *
     * @return The cluster state of OpenSearch
     */

    public ClusterState sendClusterStateRequest() {
        logger.info("Sending Cluster State request to OpenSearch");
        ClusterStateResponseHandler clusterStateResponseHandler = new ClusterStateResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_CLUSTER_STATE,
                new ExtensionRequest(ExtensionRequestProto.RequestType.REQUEST_EXTENSION_CLUSTER_STATE),
                clusterStateResponseHandler
            );
            // Wait on cluster state response
            clusterStateResponseHandler.awaitResponse();
        } catch (TimeoutException e) {
            logger.info("Failed to receive Cluster State response from OpenSearch", e);
        } catch (Exception e) {
            logger.info("Failed to send Cluster State request to OpenSearch", e);
        }

        // At this point, response handler has read in the cluster state
        return clusterStateResponseHandler.getClusterState();
    }

    /**
     * Request the Dependency Information from Opensearch. The result will be handled by a {@link ExtensionDependencyResponseHandler}.
     *
     * @return A List contains details of this extension's dependencies
     */
    public List<DiscoveryExtensionNode> sendExtensionDependencyRequest() {
        logger.info("Sending Extension Dependency Information request to Opensearch");
        ExtensionDependencyResponseHandler extensionDependencyResponseHandler = new ExtensionDependencyResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_DEPENDENCY_INFORMATION,
                new ExtensionRequest(ExtensionRequestProto.RequestType.REQUEST_EXTENSION_DEPENDENCY_INFORMATION, uniqueId),
                extensionDependencyResponseHandler
            );
            // Wait on Extension Dependency response
            extensionDependencyResponseHandler.awaitResponse();
        } catch (TimeoutException e) {
            logger.info("Failed to receive Extension Dependency response from OpenSearch", e);
        } catch (Exception e) {
            logger.info("Failed to send Extension Dependency request to OpenSearch", e);
        }

        // At this point, response handler has read in the extension dependency
        return extensionDependencyResponseHandler.getExtensionDependencies();
    }

    /**
     * Requests the cluster settings from OpenSearch.  The result will be handled by a {@link ClusterSettingsResponseHandler}.
     */
    public void sendClusterSettingsRequest() {
        sendGenericRequestWithExceptionHandling(
            ExtensionRequestProto.RequestType.REQUEST_EXTENSION_CLUSTER_SETTINGS,
            ExtensionsManager.REQUEST_EXTENSION_CLUSTER_SETTINGS,
            new ClusterSettingsResponseHandler()
        );
    }

    /**
     * Requests the environment settings from OpenSearch. The result will be handled by a {@link EnvironmentSettingsResponseHandler}.
     *
     * @return A Setting object from the OpenSearch Node environment
     */
    public Settings sendEnvironmentSettingsRequest() {
        logger.info("Sending Environment Settings request to OpenSearch");
        EnvironmentSettingsResponseHandler environmentSettingsResponseHandler = new EnvironmentSettingsResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_ENVIRONMENT_SETTINGS,
                new ExtensionRequest(ExtensionRequestProto.RequestType.REQUEST_EXTENSION_ENVIRONMENT_SETTINGS),
                environmentSettingsResponseHandler
            );
            // Wait on environment settings response
            environmentSettingsResponseHandler.awaitResponse();
        } catch (TimeoutException e) {
            logger.info("Failed to receive Environment Settings response from OpenSearch", e);
        } catch (Exception e) {
            logger.info("Failed to send Environment Settings request to OpenSearch", e);
        }

        // At this point, response handler has read in the environment settings
        return environmentSettingsResponseHandler.getEnvironmentSettings();
    }

    /**
     * Registers settings and setting consumers with the {@link UpdateSettingsRequestHandler} and then sends a request to OpenSearch to register these Setting objects with a callback to this extension.
     * The result will be handled by a {@link AcknowledgedResponseHandler}.
     *
     * @param settingUpdateConsumers A map of setting objects and their corresponding consumers
     * @param updateSettingsRequestHandler A update settings request handler
     * @param extensionNode A extension node
     */
    public void sendAddSettingsUpdateConsumerRequest(
        Map<Setting<?>, Consumer<?>> settingUpdateConsumers,
        UpdateSettingsRequestHandler updateSettingsRequestHandler,
        DiscoveryExtensionNode extensionNode
    ) {
        // Determine if there are setting update consumers to be registered
        if (!settingUpdateConsumers.isEmpty()) {
            // Register setting update consumers to UpdateSettingsRequestHandler
            updateSettingsRequestHandler.registerSettingUpdateConsumer(settingUpdateConsumers);

            // Extract registered settings from setting update consumer map
            List<Setting<?>> componentSettings = new ArrayList<>(settingUpdateConsumers.size());
            componentSettings.addAll(settingUpdateConsumers.keySet());
            logger.info(
                "Sending Add Settings Update Consumer request to OpenSearch for {}",
                componentSettings.stream().map(Setting::getKey).toArray()
            );

            AcknowledgedResponseHandler acknowledgedResponseHandler = new AcknowledgedResponseHandler();
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_ADD_SETTINGS_UPDATE_CONSUMER,
                new AddSettingsUpdateConsumerRequest(extensionNode, componentSettings),
                acknowledgedResponseHandler
            );
        }
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
