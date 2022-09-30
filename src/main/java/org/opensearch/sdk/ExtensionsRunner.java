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
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.NamedWriteableRegistryParseRequest;
import org.opensearch.extensions.OpenSearchRequest;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.extensions.rest.RegisterRestActionsRequest;
import org.opensearch.extensions.settings.RegisterCustomSettingsRequest;
import org.opensearch.common.network.NetworkModule;
import org.opensearch.common.network.NetworkService;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.PageCacheRecycler;
import org.opensearch.discovery.InitializeExtensionsRequest;
import org.opensearch.extensions.ExtensionActionListenerOnFailureRequest;
import org.opensearch.extensions.DiscoveryExtension;
import org.opensearch.extensions.EnvironmentSettingsRequest;
import org.opensearch.extensions.AddSettingsUpdateConsumerRequest;
import org.opensearch.extensions.UpdateSettingsRequest;
import org.opensearch.extensions.ExtensionRequest;
import org.opensearch.extensions.ExtensionsOrchestrator;
import org.opensearch.index.IndicesModuleRequest;
import org.opensearch.indices.IndicesModule;
import org.opensearch.indices.breaker.CircuitBreakerService;
import org.opensearch.indices.breaker.NoneCircuitBreakerService;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.transport.netty4.Netty4Transport;
import org.opensearch.transport.SharedGroupFactory;
import org.opensearch.sdk.handlers.ActionListenerOnFailureResponseHandler;
import org.opensearch.sdk.handlers.ClusterSettingsResponseHandler;
import org.opensearch.sdk.handlers.ClusterStateResponseHandler;
import org.opensearch.sdk.handlers.EnvironmentSettingsResponseHandler;
import org.opensearch.sdk.handlers.ExtensionBooleanResponseHandler;
import org.opensearch.sdk.handlers.ExtensionsIndicesModuleNameRequestHandler;
import org.opensearch.sdk.handlers.ExtensionsIndicesModuleRequestHandler;
import org.opensearch.sdk.handlers.ExtensionsInitRequestHandler;
import org.opensearch.sdk.handlers.ExtensionsRestRequestHandler;
import org.opensearch.sdk.handlers.LocalNodeResponseHandler;
import org.opensearch.sdk.handlers.UpdateSettingsRequestHandler;
import org.opensearch.sdk.handlers.ExtensionStringResponseHandler;
import org.opensearch.sdk.handlers.OpensearchRequestHandler;
import org.opensearch.search.SearchModule;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.ClusterConnectionManager;
import org.opensearch.transport.ConnectionManager;
import org.opensearch.transport.TransportInterceptor;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.TransportSettings;

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
 * This class Javadoc will eventually be expanded with a full
 * description/tutorial for users.
 */
public class ExtensionsRunner {

    private static final Logger logger = LogManager.getLogger(ExtensionsRunner.class);
    private static final String NODE_NAME_SETTING = "node.name";

    private String uniqueId;
    /**
     * This field is initialized by a call from {@link ExtensionsInitRequestHandler}.
     */
    public DiscoveryNode opensearchNode;
    private DiscoveryExtension extensionNode;
    /**
     * This field is initialized by a call from {@link ExtensionsInitRequestHandler}.
     */
    public TransportService extensionTransportService = null;
    // The routes and classes which handle the REST requests
    private final ExtensionRestPathRegistry extensionRestPathRegistry = new ExtensionRestPathRegistry();
    // Custom settings from the extension's getSettings
    /**
     * This field is initialized by a call from {@link ExtensionsInitRequestHandler}.
     */
    private final List<Setting<?>> customSettings;
    // Node name, host, and port
    /**
     * This field is initialized by a call from {@link ExtensionsInitRequestHandler}.
     */
    public final Settings settings;
    private final TransportInterceptor NOOP_TRANSPORT_INTERCEPTOR = new TransportInterceptor() {
    };
    private ExtensionNamedWriteableRegistry namedWriteableRegistryApi = new ExtensionNamedWriteableRegistry();
    private ExtensionsInitRequestHandler extensionsInitRequestHandler = new ExtensionsInitRequestHandler();
    private OpensearchRequestHandler opensearchRequestHandler = new OpensearchRequestHandler();
    private ExtensionsIndicesModuleRequestHandler extensionsIndicesModuleRequestHandler = new ExtensionsIndicesModuleRequestHandler();
    private ExtensionsIndicesModuleNameRequestHandler extensionsIndicesModuleNameRequestHandler =
        new ExtensionsIndicesModuleNameRequestHandler();
    private ExtensionsRestRequestHandler extensionsRestRequestHandler = new ExtensionsRestRequestHandler();

    /*
     * TODO: expose an interface for extension to register actions
     * https://github.com/opensearch-project/opensearch-sdk-java/issues/119
     */
    /**
     * Instantiates a new transportActions
     */
    public TransportActions transportActions = new TransportActions(new HashMap<>());

    /**
     * Instantiates a new update settings request handler
     */
    UpdateSettingsRequestHandler updateSettingsRequestHandler = new UpdateSettingsRequestHandler();

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
        this.customSettings = Collections.emptyList();
    }

    /**
     * Instantiates a new Extensions Runner using the specified extension.
     *
     * @param extension The settings with which to start the runner.
     * @throws IOException if the runner failed to read settings or API.
     */
    private ExtensionsRunner(Extension extension) throws IOException {
        ExtensionSettings extensionSettings = extension.getExtensionSettings();
        this.settings = Settings.builder()
            .put(NODE_NAME_SETTING, extensionSettings.getExtensionName())
            .put(TransportSettings.BIND_HOST.getKey(), extensionSettings.getHostAddress())
            .put(TransportSettings.PORT.getKey(), extensionSettings.getHostPort())
            .build();
        // store REST handlers in the registry
        for (ExtensionRestHandler extensionRestHandler : extension.getExtensionRestHandlers()) {
            for (Route route : extensionRestHandler.routes()) {
                extensionRestPathRegistry.registerHandler(route.getMethod(), route.getPath(), extensionRestHandler);
            }
        }
        // save custom settings
        this.customSettings = extension.getSettings();
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

    /**
     * This method is call from {@link ExtensionsInitRequestHandler}.
     * @param extensionTransportService assign value for extensionTransportService
     */
    void setExtensionTransportService(TransportService extensionTransportService) {
        this.extensionTransportService = extensionTransportService;
    }

    /**
     * Sets the Unique ID, used in REST requests to uniquely identify this extension
     * @param id assign value for id
     */
    public void setUniqueId(String id) {
        this.uniqueId = id;
    }

    String getUniqueId() {
        return uniqueId;
    }

    public void setOpensearchNode(DiscoveryNode opensearchNode) {
        this.opensearchNode = opensearchNode;
    }

    public void setExtensionNode(DiscoveryExtension extensionNode) {
        this.extensionNode = extensionNode;
    }

    DiscoveryNode getOpensearchNode() {
        return opensearchNode;
    }

    /**
     * Initializes a Netty4Transport object. This object will be wrapped in a
     * {@link TransportService} object.
     *
     * @param settings
     *            The transport settings to configure.
     * @param threadPool
     *            A thread pool to use.
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
     * Initializes the TransportService object for this extension. This object will
     * control communication between the extension and OpenSearch.
     *
     * @param settings The transport settings to configure.
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
     * @param transportService The TransportService to start.
     */
    public void startTransportService(TransportService transportService) {
        // start transport service and accept incoming requests
        transportService.start();
        transportService.acceptIncomingRequests();

        // Extension Request is the first request for the transport communication.
        // This request will initialize the extension and will be a part of OpenSearch
        // bootstrap
        transportService.registerRequestHandler(
            ExtensionsOrchestrator.REQUEST_EXTENSION_ACTION_NAME,
            ThreadPool.Names.GENERIC,
            false,
            false,
            InitializeExtensionsRequest::new,
            (request, channel, task) -> channel.sendResponse(extensionsInitRequestHandler.handleExtensionInitRequest(request, this))
        );

        transportService.registerRequestHandler(
            ExtensionsOrchestrator.REQUEST_OPENSEARCH_NAMED_WRITEABLE_REGISTRY,
            ThreadPool.Names.GENERIC,
            false,
            false,
            OpenSearchRequest::new,
            (request, channel, task) -> channel.sendResponse(opensearchRequestHandler.handleOpenSearchRequest(request))
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
            ((request, channel, task) -> channel.sendResponse(
                extensionsIndicesModuleRequestHandler.handleIndicesModuleRequest(request, transportService)
            ))

        );

        transportService.registerRequestHandler(
            ExtensionsOrchestrator.INDICES_EXTENSION_NAME_ACTION_NAME,
            ThreadPool.Names.GENERIC,
            false,
            false,
            IndicesModuleRequest::new,
            ((request, channel, task) -> channel.sendResponse(
                extensionsIndicesModuleNameRequestHandler.handleIndicesModuleNameRequest(request)
            ))
        );

        transportService.registerRequestHandler(
            ExtensionsOrchestrator.REQUEST_REST_EXECUTE_ON_EXTENSION_ACTION,
            ThreadPool.Names.GENERIC,
            false,
            false,
            ExtensionRestRequest::new,
            ((request, channel, task) -> channel.sendResponse(extensionsRestRequestHandler.handleRestExecuteOnExtensionRequest(request)))
        );

        transportService.registerRequestHandler(
            ExtensionsOrchestrator.REQUEST_EXTENSION_UPDATE_SETTINGS,
            ThreadPool.Names.GENERIC,
            false,
            false,
            UpdateSettingsRequest::new,
            ((request, channel, task) -> channel.sendResponse(updateSettingsRequestHandler.handleUpdateSettingsRequest(request)))
        );

    }

    /**
     * Requests that OpenSearch register the REST Actions for this extension.
     *
     * @param transportService The TransportService defining the connection to OpenSearch.
     */
    public void sendRegisterRestActionsRequest(TransportService transportService) {
        List<String> extensionRestPaths = extensionRestPathRegistry.getRegisteredPaths();
        logger.info("Sending Register REST Actions request to OpenSearch for " + extensionRestPaths);
        ExtensionStringResponseHandler registerActionsResponseHandler = new ExtensionStringResponseHandler();
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
     * Requests that OpenSearch register the custom settings for this extension.
     *
     * @param transportService The TransportService defining the connection to OpenSearch.
     */
    public void sendRegisterCustomSettingsRequest(TransportService transportService) {
        logger.info("Sending Settings request to OpenSearch");
        ExtensionStringResponseHandler registerCustomSettingsResponseHandler = new ExtensionStringResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsOrchestrator.REQUEST_EXTENSION_REGISTER_CUSTOM_SETTINGS,
                new RegisterCustomSettingsRequest(getUniqueId(), customSettings),
                registerCustomSettingsResponseHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send Register Settings request to OpenSearch", e);
        }
    }

    /**
     * Requests the cluster state from OpenSearch. The result will be handled by a
     * {@link ClusterStateResponseHandler}.
     *
     * @param transportService The TransportService defining the connection to OpenSearch.
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
     * Requests the cluster settings from OpenSearch. The result will be handled by
     * a {@link ClusterSettingsResponseHandler}.
     *
     * @param transportService The TransportService defining the connection to OpenSearch.
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
     * Requests the local node from OpenSearch. The result will be handled by a
     * {@link LocalNodeResponseHandler}.
     *
     * @param transportService The TransportService defining the connection to OpenSearch.
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
     * Requests the ActionListener onFailure method to be run by OpenSearch. The
     * result will be handled by a {@link ActionListenerOnFailureResponseHandler}.
     *
     * @param transportService The TransportService defining the connection to OpenSearch.
     * @param failureException The exception to be sent to OpenSearch
     */
    public void sendActionListenerOnFailureRequest(TransportService transportService, Exception failureException) {
        logger.info("Sending ActionListener onFailure request to OpenSearch");
        ActionListenerOnFailureResponseHandler listenerHandler = new ActionListenerOnFailureResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsOrchestrator.REQUEST_EXTENSION_ACTION_LISTENER_ON_FAILURE,
                new ExtensionActionListenerOnFailureRequest(failureException.toString()),
                listenerHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send ActionListener onFailure request to OpenSearch", e);
        }
    }

    /**
     * Requests the environment setting values from OpenSearch for the corresponding
     * component settings. The result will be handled by a
     * {@link EnvironmentSettingsResponseHandler}.
     *
     * @param componentSettings The component setting that correspond to the values provided by the environment settings
     * @param transportService The TransportService defining the connection to OpenSearch.
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
     * Registers settings and setting consumers with the
     * {@link UpdateSettingsRequestHandler} and then sends a request to OpenSearch
     * to register these Setting objects with a callback to this extension. The
     * result will be handled by a {@link ExtensionBooleanResponseHandler}.
     *
     * @param transportService The TransportService defining the connection to OpenSearch.
     * @param settingUpdateConsumers A map of setting objects and their corresponding consumers
     * @throws Exception if there are no setting update consumers within the settingUpdateConsumers map
     */
    public void sendAddSettingsUpdateConsumerRequest(TransportService transportService, Map<Setting<?>, Consumer<?>> settingUpdateConsumers)
        throws Exception {
        logger.info("Sending Add Settings Update Consumer request to OpenSearch");

        // Determine if there are setting update consumers to be registered
        if (settingUpdateConsumers.isEmpty()) {
            throw new Exception("There are no setting update consumers to be registered");
        } else {

            // Register setting update consumers to UpdateSettingsRequestHandler
            this.updateSettingsRequestHandler.registerSettingUpdateConsumer(settingUpdateConsumers);

            // Extract registered settings from setting update consumer map
            List<Setting<?>> componentSettings = new ArrayList<>(settingUpdateConsumers.size());
            componentSettings.addAll(settingUpdateConsumers.keySet());

            ExtensionBooleanResponseHandler extensionBooleanResponseHandler = new ExtensionBooleanResponseHandler();
            try {
                transportService.sendRequest(
                    opensearchNode,
                    ExtensionsOrchestrator.REQUEST_EXTENSION_ADD_SETTINGS_UPDATE_CONSUMER,
                    new AddSettingsUpdateConsumerRequest(this.extensionNode, componentSettings),
                    extensionBooleanResponseHandler
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
     * @param timeout The timeout for the listener in milliseconds. A timeout of 0 means no timeout.
     */
    public void startActionListener(int timeout) {
        final ActionListener actionListener = new ActionListener();
        actionListener.runActionListener(true, timeout);
    }

    /**
     * Runs the specified extension.
     *
     * @param extension The extension to run.
     * @throws IOException on failure to bind ports.
     */
    public static void run(Extension extension) throws IOException {
        logger.info("Starting extension " + extension.getExtensionSettings().getExtensionName());
        @SuppressWarnings("unused")
        ExtensionsRunner runner = new ExtensionsRunner(extension);
    }

    /**
     * Run the Extension. For internal/testing purposes only. Imports settings and
     * sets up Transport Service listening for incoming connections.
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
