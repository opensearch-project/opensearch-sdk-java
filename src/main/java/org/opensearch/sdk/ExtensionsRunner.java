/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.Version;
import org.opensearch.cluster.ClusterModule;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.NamedWriteableRegistryParseRequest;
import org.opensearch.extensions.OpenSearchRequest;
import org.opensearch.extensions.rest.RegisterRestActionsRequest;
import org.opensearch.extensions.rest.RestExecuteOnExtensionRequest;
import org.opensearch.extensions.rest.RestExecuteOnExtensionResponse;
import org.opensearch.common.network.NetworkModule;
import org.opensearch.common.network.NetworkService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.PageCacheRecycler;
import org.opensearch.extensions.ExtensionBooleanResponse;
import org.opensearch.discovery.InitializeExtensionsRequest;
import org.opensearch.discovery.InitializeExtensionsResponse;
import org.opensearch.extensions.ExtensionRequest;
import org.opensearch.extensions.ExtensionsOrchestrator;
import org.opensearch.index.IndicesModuleRequest;
import org.opensearch.index.IndicesModuleResponse;
import org.opensearch.indices.IndicesModule;
import org.opensearch.indices.breaker.CircuitBreakerService;
import org.opensearch.indices.breaker.NoneCircuitBreakerService;
import org.opensearch.rest.RestStatus;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestResponse;
import org.opensearch.transport.netty4.Netty4Transport;
import org.opensearch.transport.SharedGroupFactory;
import org.opensearch.sdk.handlers.ClusterSettingsResponseHandler;
import org.opensearch.sdk.handlers.ClusterStateResponseHandler;
import org.opensearch.sdk.handlers.LocalNodeResponseHandler;
import org.opensearch.sdk.handlers.RegisterRestActionsResponseHandler;
import org.opensearch.search.SearchModule;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.ClusterConnectionManager;
import org.opensearch.transport.ConnectionManager;
import org.opensearch.transport.TransportInterceptor;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.TransportSettings;
import org.opensearch.transport.TransportResponse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static org.opensearch.common.UUIDs.randomBase64UUID;

/**
 * The primary class to run an extension.
 * <p>
 * This class Javadoc will eventually be expanded with a full description/tutorial for users.
 */
public class ExtensionsRunner {

    private static final Logger logger = LogManager.getLogger(ExtensionsRunner.class);
    private static final String NODE_NAME_SETTING = "node.name";

    private Map<String, ExtensionRestHandler> extensionRestPathMap = new HashMap<>();
    private String uniqueId;
    private DiscoveryNode opensearchNode;
    private TransportService extensionTransportService = null;

    private final Settings settings;
    private final TransportInterceptor NOOP_TRANSPORT_INTERCEPTOR = new TransportInterceptor() {
    };
    private NamedWriteableRegistryAPI namedWriteableRegistryApi = new NamedWriteableRegistryAPI();

    /**
     * Instantiates a new Extensions Runner using test settings.
     *
     * @throws IOException if the runner failed to read settings or API.
     */
    public ExtensionsRunner() throws IOException {
        ExtensionSettings extensionSettings = readExtensionSettings();
        this.settings = Settings.builder()
            .put(NODE_NAME_SETTING, extensionSettings.getExtensionName())
            .put(TransportSettings.BIND_HOST.getKey(), extensionSettings.getHostAddress())
            .put(TransportSettings.PORT.getKey(), extensionSettings.getHostPort())
            .build();
    }

    /**
     * Instantiates a new Extensions Runner using the specified extension.
     *
     * @param extension  The settings with which to start the runner.
     * @throws IOException if the runner failed to read settings or API.
     */
    private ExtensionsRunner(Extension extension) throws IOException {
        ExtensionSettings extensionSettings = extension.getExtensionSettings();
        this.settings = Settings.builder()
            .put(NODE_NAME_SETTING, extensionSettings.getExtensionName())
            .put(TransportSettings.BIND_HOST.getKey(), extensionSettings.getHostAddress())
            .put(TransportSettings.PORT.getKey(), extensionSettings.getHostPort())
            .build();
        // store rest handlers in the map
        for (ExtensionRestHandler extensionRestHandler : extension.getExtensionRestHandlers()) {
            for (Route route : extensionRestHandler.routes()) {
                String restPath = route.getMethod().name() + " " + route.getPath();
                extensionRestPathMap.put(restPath, extensionRestHandler);
            }
        }
        // initialize the transport service
        this.initializeExtensionTransportService(this.getSettings());
        // start listening on configured port and wait for connection from OpenSearch
        this.startActionListener(0);
    }

    private static ExtensionSettings readExtensionSettings() throws IOException {
        File file = new File(ExtensionSettings.EXTENSION_DESCRIPTOR);
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(file, ExtensionSettings.class);
    }

    void setExtensionTransportService(TransportService extensionTransportService) {
        this.extensionTransportService = extensionTransportService;
    }

    private void setUniqueId(String id) {
        this.uniqueId = id;
    }

    String getUniqueId() {
        return uniqueId;
    }

    private void setOpensearchNode(DiscoveryNode opensearchNode) {
        this.opensearchNode = opensearchNode;
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
    InitializeExtensionsResponse handleExtensionInitRequest(InitializeExtensionsRequest extensionInitRequest) {
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
            sendRegisterRestActionsRequest(extensionTransportService);
        }
    }

    /**
     * Handles a request from OpenSearch and invokes the extension point API corresponding with the request type
     *
     * @param request  The request to handle.
     * @return A response to OpenSearch for the corresponding API
     * @throws Exception if the corresponding handler for the request is not present
     */
    TransportResponse handleOpenSearchRequest(OpenSearchRequest request) throws Exception {
        // Read enum
        switch (request.getRequestType()) {
            case REQUEST_OPENSEARCH_NAMED_WRITEABLE_REGISTRY:
                return namedWriteableRegistryApi.handleNamedWriteableRegistryRequest(request);
            // Add additional request handlers here
            default:
                throw new Exception("Handler not present for the provided request");
        }
    }

    /**
     * Handles a request for extension point indices from OpenSearch.  The {@link #handleExtensionInitRequest(InitializeExtensionsRequest)} method must have been called first to initialize the extension.
     *
     * @param indicesModuleRequest  The request to handle.
     * @param transportService  The transport service communicating with OpenSearch.
     * @return A response to OpenSearch with this extension's index and search listeners.
     */
    IndicesModuleResponse handleIndicesModuleRequest(IndicesModuleRequest indicesModuleRequest, TransportService transportService) {
        logger.info("Registering Indices Module Request received from OpenSearch");
        IndicesModuleResponse indicesModuleResponse = new IndicesModuleResponse(true, true, true);
        return indicesModuleResponse;
    }

    /**
     * Handles a request for extension name from OpenSearch.  The {@link #handleExtensionInitRequest(InitializeExtensionsRequest)} method must have been called first to initialize the extension.
     *
     * @param indicesModuleRequest  The request to handle.
     * @return A response acknowledging the request.
     */
    ExtensionBooleanResponse handleIndicesModuleNameRequest(IndicesModuleRequest indicesModuleRequest) {
        // Works as beforeIndexRemoved
        logger.info("Registering Indices Module Name Request received from OpenSearch");
        ExtensionBooleanResponse indicesModuleNameResponse = new ExtensionBooleanResponse(true);
        return indicesModuleNameResponse;
    }

    /**
     * Handles a request from OpenSearch to execute a REST request on the extension.
     *
     * @param request  The REST request to execute.
     * @return A response acknowledging the request.
     */
    RestExecuteOnExtensionResponse handleRestExecuteOnExtensionRequest(RestExecuteOnExtensionRequest request) {

        String restPath = request.getMethod().name() + " " + request.getUri();
        ExtensionRestHandler restHandler = extensionRestPathMap.get(restPath);
        if (restHandler == null) {
            return new RestExecuteOnExtensionResponse(RestStatus.INTERNAL_SERVER_ERROR, "No handler for " + restPath);
        }
        // Get response from extension
        RestResponse response = restHandler.handleRequest(request.getMethod(), request.getUri());
        logger.info("Sending extension response to OpenSearch: " + response.status());
        return new RestExecuteOnExtensionResponse(
            response.status(),
            response.contentType(),
            BytesReference.toBytes(response.content()),
            response.getHeaders()
        );
    }

    /**
     * Initializes a Netty4Transport object. This object will be wrapped in a {@link TransportService} object.
     *
     * @param settings  The transport settings to configure.
     * @param threadPool  A thread pool to use.
     * @return The configured Netty4Transport object.
     */
    public Netty4Transport getNetty4Transport(Settings settings, ThreadPool threadPool) {
        NetworkService networkService = new NetworkService(Collections.emptyList());
        PageCacheRecycler pageCacheRecycler = new PageCacheRecycler(settings);
        IndicesModule indicesModule = new IndicesModule(Collections.emptyList());
        SearchModule searchModule = new SearchModule(settings, Collections.emptyList());

        List<NamedWriteableRegistry.Entry> namedWriteables = Stream.of(
            NetworkModule.getNamedWriteables().stream(),
            indicesModule.getNamedWriteables().stream(),
            searchModule.getNamedWriteables().stream(),
            null,
            ClusterModule.getNamedWriteables().stream()
        ).flatMap(Function.identity()).collect(Collectors.toList());

        final NamedWriteableRegistry namedWriteableRegistry = new NamedWriteableRegistry(namedWriteables);

        final CircuitBreakerService circuitBreakerService = new NoneCircuitBreakerService();

        Netty4Transport transport = new Netty4Transport(
            settings,
            Version.CURRENT,
            threadPool,
            networkService,
            pageCacheRecycler,
            namedWriteableRegistry,
            circuitBreakerService,
            new SharedGroupFactory(settings)
        );

        return transport;
    }

    /**
     * Initializes the TransportService object for this extension. This object will control communication between the extension and OpenSearch.
     *
     * @param settings  The transport settings to configure.
     * @return The initialized TransportService object.
     */
    public TransportService initializeExtensionTransportService(Settings settings) {

        ThreadPool threadPool = new ThreadPool(settings);

        Netty4Transport transport = getNetty4Transport(settings, threadPool);

        final ConnectionManager connectionManager = new ClusterConnectionManager(settings, transport);

        // Stop any existing transport service
        if (extensionTransportService != null) {
            extensionTransportService.stop();
        }

        // create transport service
        extensionTransportService = new TransportService(
            settings,
            transport,
            threadPool,
            NOOP_TRANSPORT_INTERCEPTOR,
            boundAddress -> DiscoveryNode.createLocal(
                Settings.builder().put(NODE_NAME_SETTING, settings.get(NODE_NAME_SETTING)).build(),
                boundAddress.publishAddress(),
                randomBase64UUID()
            ),
            null,
            emptySet(),
            connectionManager
        );
        startTransportService(extensionTransportService);
        return extensionTransportService;
    }

    /**
     * Starts a TransportService.
     *
     * @param transportService  The TransportService to start.
     */
    public void startTransportService(TransportService transportService) {
        // start transport service and accept incoming requests
        transportService.start();
        transportService.acceptIncomingRequests();

        // Extension Request is the first request for the transport communication.
        // This request will initialize the extension and will be a part of OpenSearch bootstrap
        transportService.registerRequestHandler(
            ExtensionsOrchestrator.REQUEST_EXTENSION_ACTION_NAME,
            ThreadPool.Names.GENERIC,
            false,
            false,
            InitializeExtensionsRequest::new,
            (request, channel, task) -> channel.sendResponse(handleExtensionInitRequest(request))
        );

        transportService.registerRequestHandler(
            ExtensionsOrchestrator.REQUEST_OPENSEARCH_NAMED_WRITEABLE_REGISTRY,
            ThreadPool.Names.GENERIC,
            false,
            false,
            OpenSearchRequest::new,
            (request, channel, task) -> channel.sendResponse(handleOpenSearchRequest(request))
        );

        transportService.registerRequestHandler(
            ExtensionsOrchestrator.REQUEST_OPENSEARCH_PARSE_NAMED_WRITEABLE,
            ThreadPool.Names.GENERIC,
            false,
            false,
            NamedWriteableRegistryParseRequest::new,
            (request, channel, task) -> channel.sendResponse(namedWriteableRegistryApi.handleNamedWriteableRegistryParseRequest(request))
        );

        transportService.registerRequestHandler(
            ExtensionsOrchestrator.INDICES_EXTENSION_POINT_ACTION_NAME,
            ThreadPool.Names.GENERIC,
            false,
            false,
            IndicesModuleRequest::new,
            ((request, channel, task) -> channel.sendResponse(handleIndicesModuleRequest(request, transportService)))

        );

        transportService.registerRequestHandler(
            ExtensionsOrchestrator.INDICES_EXTENSION_NAME_ACTION_NAME,
            ThreadPool.Names.GENERIC,
            false,
            false,
            IndicesModuleRequest::new,
            ((request, channel, task) -> channel.sendResponse(handleIndicesModuleNameRequest(request)))
        );

        transportService.registerRequestHandler(
            ExtensionsOrchestrator.REQUEST_REST_EXECUTE_ON_EXTENSION_ACTION,
            ThreadPool.Names.GENERIC,
            false,
            false,
            RestExecuteOnExtensionRequest::new,
            ((request, channel, task) -> channel.sendResponse(handleRestExecuteOnExtensionRequest(request)))
        );

    }

    /**
     * Requests that OpenSearch register the REST Actions for this extension.
     *
     * @param transportService  The TransportService defining the connection to OpenSearch.
     */
    public void sendRegisterRestActionsRequest(TransportService transportService) {
        List<String> extensionRestPaths = new ArrayList<>(extensionRestPathMap.keySet());
        logger.info("Sending Register REST Actions request to OpenSearch for " + extensionRestPaths);
        RegisterRestActionsResponseHandler registerActionsResponseHandler = new RegisterRestActionsResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsOrchestrator.REQUEST_EXTENSION_REGISTER_REST_ACTIONS,
                new RegisterRestActionsRequest(getUniqueId(), extensionRestPaths),
                registerActionsResponseHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send Register REST Actions request to OpenSearch", e);
        }
    }

    /**
     * Requests the cluster state from OpenSearch.  The result will be handled by a {@link ClusterStateResponseHandler}.
     *
     * @param transportService  The TransportService defining the connection to OpenSearch.
     */
    public void sendClusterStateRequest(TransportService transportService) {
        logger.info("Sending Cluster State request to OpenSearch");
        ClusterStateResponseHandler clusterStateResponseHandler = new ClusterStateResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsOrchestrator.REQUEST_EXTENSION_CLUSTER_STATE,
                new ExtensionRequest(ExtensionsOrchestrator.RequestType.REQUEST_EXTENSION_CLUSTER_STATE),
                clusterStateResponseHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send Cluster State request to OpenSearch", e);
        }
    }

    /**
     * Requests the cluster settings from OpenSearch.  The result will be handled by a {@link ClusterSettingsResponseHandler}.
     *
     * @param transportService  The TransportService defining the connection to OpenSearch.
     */
    public void sendClusterSettingsRequest(TransportService transportService) {
        logger.info("Sending Cluster Settings request to OpenSearch");
        ClusterSettingsResponseHandler clusterSettingsResponseHandler = new ClusterSettingsResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsOrchestrator.REQUEST_EXTENSION_CLUSTER_SETTINGS,
                new ExtensionRequest(ExtensionsOrchestrator.RequestType.REQUEST_EXTENSION_CLUSTER_SETTINGS),
                clusterSettingsResponseHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send Cluster Settings request to OpenSearch", e);
        }
    }

    /**
     * Requests the local node from OpenSearch.  The result will be handled by a {@link LocalNodeResponseHandler}.
     *
     * @param transportService  The TransportService defining the connection to OpenSearch.
     */
    public void sendLocalNodeRequest(TransportService transportService) {
        logger.info("Sending Local Node request to OpenSearch");
        LocalNodeResponseHandler localNodeResponseHandler = new LocalNodeResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsOrchestrator.REQUEST_EXTENSION_LOCAL_NODE,
                new ExtensionRequest(ExtensionsOrchestrator.RequestType.REQUEST_EXTENSION_LOCAL_NODE),
                localNodeResponseHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send Cluster Settings request to OpenSearch", e);
        }
    }

    private Settings getSettings() {
        return settings;
    }

    /**
     * Starts an ActionListener.
     *
     * @param timeout  The timeout for the listener in milliseconds. A timeout of 0 means no timeout.
     */
    public void startActionListener(int timeout) {
        final ActionListener actionListener = new ActionListener();
        actionListener.runActionListener(true, timeout);
    }

    /**
     * Runs the specified extension.
     *
     * @param extension  The extension to run.
     * @throws IOException  on failure to bind ports.
     */
    public static void run(Extension extension) throws IOException {
        logger.info("Starting extension " + extension.getExtensionSettings().getExtensionName());
        @SuppressWarnings("unused")
        ExtensionsRunner runner = new ExtensionsRunner(extension);
    }

    /**
     * Run the Extension. For internal/testing purposes only. Imports settings and sets up Transport Service listening for incoming connections.
     *
     * @param args  Unused
     * @throws IOException if the runner failed to connect to the OpenSearch cluster.
     */
    public static void main(String[] args) throws IOException {

        ExtensionsRunner extensionsRunner = new ExtensionsRunner();

        // initialize the transport service
        extensionsRunner.initializeExtensionTransportService(extensionsRunner.getSettings());
        // start listening on configured port and wait for connection from OpenSearch
        extensionsRunner.startActionListener(0);
    }
}
