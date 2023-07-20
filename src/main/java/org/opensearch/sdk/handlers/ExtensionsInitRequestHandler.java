/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.WarningFailureException;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.common.settings.Settings;
import org.opensearch.discovery.InitializeExtensionRequest;
import org.opensearch.discovery.InitializeExtensionResponse;
import org.opensearch.discovery.InitializeExtensionSecurityRequest;
import org.opensearch.discovery.InitializeExtensionSecurityResponse;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.SDKTransportService;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.opensearch.sdk.ExtensionsRunner.NODE_NAME_SETTING;

/**
 * This class handles the request from OpenSearch to a {@link ExtensionsRunner#startTransportService(TransportService transportService)} call.
 */

public class ExtensionsInitRequestHandler {
    private static final Logger logger = LogManager.getLogger(ExtensionsInitRequestHandler.class);

    // The default http port setting of OpenSearch
    private static final String DEFAULT_HTTP_PORT = "9200";

    // The configured http port setting of opensearch.yml
    private static final String HTTP_PORT_SETTING = "http.port";

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
        extensionsRunner.getThreadPool().getThreadContext().putHeader("extension_unique_id", extensionInitRequest.getExtension().getId());
        SDKTransportService sdkTransportService = extensionsRunner.getSdkTransportService();
        sdkTransportService.setOpensearchNode(extensionInitRequest.getSourceNode());
        sdkTransportService.setUniqueId(extensionInitRequest.getExtension().getId());
        // Successfully initialized. Send the response.
        try {
            return new InitializeExtensionResponse(
                extensionsRunner.getSettings().get(NODE_NAME_SETTING),
                extensionsRunner.getExtensionImplementedInterfaces()
            );
        } finally {
            // After sending successful response to initialization, send the REST API and Settings
            extensionsRunner.setExtensionNode(extensionInitRequest.getExtension());

            TransportService extensionTransportService = sdkTransportService.getTransportService();
            extensionTransportService.connectToNodeAsExtension(
                extensionInitRequest.getSourceNode(),
                extensionInitRequest.getExtension().getId()
            );
            sdkTransportService.sendRegisterRestActionsRequest(extensionsRunner.getExtensionRestPathRegistry());
            sdkTransportService.sendRegisterCustomSettingsRequest(extensionsRunner.getCustomSettings());
            sdkTransportService.sendRegisterTransportActionsRequest(extensionsRunner.getSdkActionModule().getActions());
            // Get OpenSearch Settings and set values on ExtensionsRunner
            Settings settings = sdkTransportService.sendEnvironmentSettingsRequest();
            extensionsRunner.setEnvironmentSettings(settings);
            extensionsRunner.updateNamedXContentRegistry();
            extensionsRunner.updateSdkClusterService();
            // Use OpenSearch Settings to update client REST Connections
            String openSearchNodeAddress = extensionInitRequest.getSourceNode().getAddress().getAddress();
            String openSearchNodeHttpPort = settings.get(HTTP_PORT_SETTING) != null ? settings.get(HTTP_PORT_SETTING) : DEFAULT_HTTP_PORT;
            extensionsRunner.getSdkClient().updateOpenSearchNodeSettings(openSearchNodeAddress, openSearchNodeHttpPort);

            // Last step of initialization
            // TODO: make sure all the other sendX methods have completed
            // https://github.com/opensearch-project/opensearch-sdk-java/issues/17
            extensionsRunner.setInitialized();

            // Trigger pending updates requiring completion of the above actions
            extensionsRunner.getSdkClusterService().getClusterSettings().sendPendingSettingsUpdateConsumers();
        }
    }

    /**
     * Handles a extension request from OpenSearch. This is the first request for the transport communication and will initialize the extension and will be a part of OpenSearch bootstrap.
     *
     * @param extensionInitSecurityRequest  The request to handle.
     * @return A response to OpenSearch validating that this is an extension.
     */
    public InitializeExtensionSecurityResponse handleExtensionSecurityInitRequest(
        InitializeExtensionSecurityRequest extensionInitSecurityRequest
    ) {
        logger.info("Registering Extension Request received from OpenSearch");

        System.out.println("Service Account Token: " + extensionInitSecurityRequest.getServiceAccountToken());

        // Uncomment the lines below to try out different actions utilizing the service account token

//        OpenSearchClient restClient1 = extensionsRunner.getSdkClient()
//            .initializeJavaClientWithHeaders(
//                Map.of("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)))
//            );
//
//        try {
//            restClient1.indices().create(new CreateIndexRequest.Builder().index(".hello-world-jobs").build());
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        } catch (WarningFailureException e2) {
//            System.out.println(e2.getMessage());
//        }
//
//        OpenSearchClient restClient2 = extensionsRunner.getSdkClient()
//            .initializeJavaClientWithHeaders(Map.of("Authorization", "Bearer " + extensionInitSecurityRequest.getServiceAccountToken()));
//
//        try {
//            restClient2.indices().delete(new DeleteIndexRequest.Builder().index(".hello-world-jobs").build());
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        } catch (WarningFailureException e2) {
//            System.out.println(e2.getMessage());
//        }
//
//        // Try reading from index with service account token
//
//        try {
//            restClient1.indices().create(new CreateIndexRequest.Builder().index("logs-123").build());
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        } catch (WarningFailureException e2) {
//            System.out.println(e2.getMessage());
//        }
//
//        try {
//            SearchRequest searchRequest = new SearchRequest.Builder()
//                    .index("logs-123")
//                    .build();
//            SearchResponse<JsonNode> searchResponse = restClient2.search(searchRequest, JsonNode.class);
//            System.out.println("SearchResponse: " + searchResponse);
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        } catch (WarningFailureException e2) {
//            System.out.println(e2.getMessage());
//        }
//
//        try {
//            IndexData indexData = new IndexData("John", "Doe");
//            IndexRequest<IndexData> indexRequest = new IndexRequest.Builder<IndexData>().index("logs-123").id("1").document(indexData).build();
//            restClient2.index(indexRequest);
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        } catch (WarningFailureException e2) {
//            System.out.println(e2.getMessage());
//        }

        return new InitializeExtensionSecurityResponse(extensionsRunner.getExtensionNode().getId());
    }

    static class IndexData {
        private String firstName;
        private String lastName;

        public IndexData(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        @Override
        public String toString() {
            return String.format("IndexData{first name='%s', last name='%s'}", firstName, lastName);
        }
    }
}
