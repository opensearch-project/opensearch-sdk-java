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
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.PageCacheRecycler;
import org.opensearch.extensions.ExtensionBooleanResponse;
import org.opensearch.discovery.InitializeExtensionsRequest;
import org.opensearch.discovery.InitializeExtensionsResponse;
import org.opensearch.extensions.DiscoveryExtension;
import org.opensearch.extensions.EnvironmentSettingsRequest;
import org.opensearch.extensions.AddSettingsUpdateConsumerRequest;
import org.opensearch.extensions.UpdateSettingsRequest;
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
import org.opensearch.sdk.handlers.ActionListenerOnFailureResponseHandler;
import org.opensearch.sdk.handlers.AddSettingsUpdateConsumerResponseHandler;
import org.opensearch.sdk.handlers.ClusterSettingsResponseHandler;
import org.opensearch.sdk.handlers.ClusterStateResponseHandler;
import org.opensearch.sdk.handlers.EnvironmentSettingsResponseHandler;
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
import org.opensearch.common.settings.WriteableSetting;
import org.opensearch.common.unit.ByteSizeValue;
import org.opensearch.common.unit.TimeValue;

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
import java.util.function.Consumer;

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

    private String uniqueId;
    private DiscoveryNode opensearchNode;
    private DiscoveryExtension extensionNode;
    private TransportService extensionTransportService = null;
    private ExtensionRestPathRegistry extensionRestPathRegistry = new ExtensionRestPathRegistry();

    private final Settings settings;
    private final TransportInterceptor NOOP_TRANSPORT_INTERCEPTOR = new TransportInterceptor() {
    };
    private NamedWriteableRegistryAPI namedWriteableRegistryApi = new NamedWriteableRegistryAPI();
    /*
     * TODO: expose an interface for extension to register actions
     * https://github.com/opensearch-project/opensearch-sdk-java/issues/119
     */
    private TransportActions transportActions = new TransportActions(new HashMap<>());
    private Map<Setting<?>, Consumer<?>> settingUpdateConsumers = new HashMap<>();

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
                extensionRestPathRegistry.registerHandler(route.getMethod(), route.getPath(), extensionRestHandler);
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

    private void setExtensionNode(DiscoveryExtension extensionNode) {
        this.extensionNode = extensionNode;
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
            setExtensionNode(extensionInitRequest.getExtension());
            extensionTransportService.connectToNode(opensearchNode);
            sendRegisterRestActionsRequest(extensionTransportService);
            transportActions.sendRegisterTransportActionsRequest(extensionTransportService, opensearchNode);
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
     * Handles a request to update a setting from OpenSearch.  Extensions must register their setting keys and consumers within the settingUpdateConsumer map
     *
     * @param updateSettingsRequest  The request to handle.
     * @return A response acknowledging the request.
     */
    @SuppressWarnings("unchecked")
    ExtensionBooleanResponse handleUpdateSettingsRequest(UpdateSettingsRequest updateSettingsRequest) {
        logger.info("Registering UpdateSettingsRequest received from OpenSearch");

        boolean settingUpdateStatus = true;

        WriteableSetting.SettingType settingType = updateSettingsRequest.getSettingType();
        Setting<?> componentSetting = updateSettingsRequest.getComponentSetting();
        Object data = updateSettingsRequest.getData();

        // Setting updater in OpenSearch performs setting change validation, only need to cast the consumer to the corresponding type and
        // invoke the consumer
        try {
            switch (settingType) {
                case Boolean:
                    Consumer<Boolean> boolConsumer = (Consumer<Boolean>) this.settingUpdateConsumers.get(componentSetting);
                    boolConsumer.accept(Boolean.parseBoolean(data.toString()));
                case Integer:
                    Consumer<Integer> intConsumer = (Consumer<Integer>) this.settingUpdateConsumers.get(componentSetting);
                    intConsumer.accept(Integer.parseInt(data.toString()));
                case Long:
                    Consumer<Long> longConsumer = (Consumer<Long>) this.settingUpdateConsumers.get(componentSetting);
                    longConsumer.accept(Long.parseLong(data.toString()));
                case Float:
                    Consumer<Float> floatConsumer = (Consumer<Float>) this.settingUpdateConsumers.get(componentSetting);
                    floatConsumer.accept(Float.parseFloat(data.toString()));
                case Double:
                    Consumer<Double> doubleConsumer = (Consumer<Double>) this.settingUpdateConsumers.get(componentSetting);
                    doubleConsumer.accept(Double.parseDouble(data.toString()));
                case String:
                    Consumer<String> stringConsumer = (Consumer<String>) this.settingUpdateConsumers.get(componentSetting);
                    stringConsumer.accept(data.toString());
                case TimeValue:
                    Consumer<TimeValue> timeValueConsumer = (Consumer<TimeValue>) this.settingUpdateConsumers.get(componentSetting);
                    timeValueConsumer.accept(TimeValue.parseTimeValue(data.toString(), componentSetting.getKey()));
                case ByteSizeValue:
                    Consumer<ByteSizeValue> byteSizeValueConsumer = (Consumer<ByteSizeValue>) this.settingUpdateConsumers.get(
                        componentSetting
                    );
                    byteSizeValueConsumer.accept(ByteSizeValue.parseBytesSizeValue(data.toString(), componentSetting.getKey()));
                case Version:
                    Consumer<Version> versionConsumer = (Consumer<Version>) this.settingUpdateConsumers.get(componentSetting);
                    versionConsumer.accept((Version) data);
                default:
                    throw new UnsupportedOperationException("Setting Update Consumer type does not exist and is not handled here");
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
            settingUpdateStatus = false;
        }

        return new ExtensionBooleanResponse(settingUpdateStatus);
    }

    /**
     * Handles a request from OpenSearch to execute a REST request on the extension.
     *
     * @param request  The REST request to execute.
     * @return A response acknowledging the request.
     */
    RestExecuteOnExtensionResponse handleRestExecuteOnExtensionRequest(RestExecuteOnExtensionRequest request) {

        ExtensionRestHandler restHandler = extensionRestPathRegistry.getHandler(request.getMethod(), request.getUri());
        if (restHandler == null) {
            return new RestExecuteOnExtensionResponse(
                RestStatus.NOT_FOUND,
                "No handler for " + ExtensionRestPathRegistry.restPathToString(request.getMethod(), request.getUri())
            );
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

        transportService.registerRequestHandler(
            ExtensionsOrchestrator.REQUEST_EXTENSION_UPDATE_SETTINGS,
            ThreadPool.Names.GENERIC,
            false,
            false,
            UpdateSettingsRequest::new,
            ((request, channel, task) -> channel.sendResponse(handleUpdateSettingsRequest(request)))
        );

    }

    /**
     * Requests that OpenSearch register the REST Actions for this extension.
     *
     * @param transportService  The TransportService defining the connection to OpenSearch.
     */
    public void sendRegisterRestActionsRequest(TransportService transportService) {
        List<String> extensionRestPaths = extensionRestPathRegistry.getRegisteredPaths();
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

    /**
     * Requests the ActionListener onFailure method to be run by OpenSearch.  The result will be handled by a {@link ActionListenerOnFailureResponseHandler}.
     *
     * @param transportService  The TransportService defining the connection to OpenSearch.
     * @param failureException The exception to be sent to OpenSearch
     */
    public void sendActionListenerOnFailureRequest(TransportService transportService, Exception failureException) {
        logger.info("Sending ActionListener onFailure request to OpenSearch");
        ActionListenerOnFailureResponseHandler listenerHandler = new ActionListenerOnFailureResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsOrchestrator.REQUEST_EXTENSION_ACTION_LISTENER_ON_FAILURE,
                new ExtensionRequest(
                    ExtensionsOrchestrator.RequestType.REQUEST_EXTENSION_ACTION_LISTENER_ON_FAILURE,
                    failureException.toString()
                ),
                listenerHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send ActionListener onFailure request to OpenSearch", e);
        }
    }

    /**
     * Requests the environment setting values from OpenSearch for the corresponding component settings. The result will be handled by a {@link EnvironmentSettingsResponseHandler}.
     *
     * @param componentSettings The component setting that correspond to the values provided by the environment settings
     * @param transportService  The TransportService defining the connection to OpenSearch.
     */
    public void sendEnvironmentSettingsRequest(TransportService transportService, List<Setting<?>> componentSettings) {
        logger.info("Sending Environment Settings request to OpenSearch");
        EnvironmentSettingsResponseHandler environmentSettingsResponseHandler = new EnvironmentSettingsResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsOrchestrator.REQUEST_EXTENSION_ENVIRONMENT_SETTINGS,
                new EnvironmentSettingsRequest(componentSettings),
                environmentSettingsResponseHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send Environment Settings request to OpenSearch", e);
        }
    }

    /**
     * Registers the component {@link Setting} and the corresponding consumer to the settingsUpdateConsumer map.
     * Extensions will use this method instead of invoking ClusterSettings#addSettingsUpdateConsumer
     *
     * @param componentSetting The component setting associated with the consumer
     * @param consumer  The setting update consumer associated with the component setting
     */
    public <T> void registerSettingUpdateConsumer(Setting<T> componentSetting, Consumer<T> consumer) {
        this.settingUpdateConsumers.put(componentSetting, consumer);
    }

    /**
     * Requests the environment setting values from OpenSearch for the corresponding component settings.
     * This should be called after extension components have registered all their setting update consumers with the settingUpdateConsumers map.
     * The result will be handled by a {@link EnvironmentSettingsResponseHandler}.
     *
     * @param transportService  The TransportService defining the connection to OpenSearch.
     * @throws Exception if there are no setting update consumers registered within the settingUpdateConsumers map
     */
    public void sendAddSettingsUpdateConsumerRequest(TransportService transportService) throws Exception {
        logger.info("Sending Add Settings Update Consumer request to OpenSearch");

        // Determine if setting update consumers have been registered
        if (this.settingUpdateConsumers.isEmpty()) {
            throw new Exception("There are no setting update consumers registered");
        } else {

            // Extract registered settings from setting update consumer map
            List<Setting<?>> componentSettings = new ArrayList<>(this.settingUpdateConsumers.size());
            componentSettings.addAll(this.settingUpdateConsumers.keySet());

            AddSettingsUpdateConsumerResponseHandler addSettingsUpdateConsumerResponseHandler =
                new AddSettingsUpdateConsumerResponseHandler();
            try {
                transportService.sendRequest(
                    opensearchNode,
                    ExtensionsOrchestrator.REQUEST_EXTENSION_ADD_SETTINGS_UPDATE_CONSUMER,
                    new AddSettingsUpdateConsumerRequest(this.extensionNode, componentSettings),
                    addSettingsUpdateConsumerResponseHandler
                );
            } catch (Exception e) {
                logger.info("Failed to send Add Settings Update Consumer request to OpenSearch", e);
            }
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
    public static void startActionListener(int timeout) {
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
