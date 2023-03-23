/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionType;
import org.opensearch.action.support.TransportAction;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.extensions.rest.RegisterRestActionsRequest;
import org.opensearch.extensions.settings.RegisterCustomSettingsRequest;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.discovery.InitializeExtensionRequest;
import org.opensearch.extensions.DiscoveryExtensionNode;
import org.opensearch.extensions.AddSettingsUpdateConsumerRequest;
import org.opensearch.extensions.UpdateSettingsRequest;
import org.opensearch.extensions.action.ExtensionActionRequest;
import org.opensearch.extensions.ExtensionsManager.RequestType;
import org.opensearch.extensions.ExtensionRequest;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.index.IndicesModuleRequest;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.sdk.handlers.ClusterSettingsResponseHandler;
import org.opensearch.sdk.handlers.ClusterStateResponseHandler;
import org.opensearch.sdk.handlers.EnvironmentSettingsResponseHandler;
import org.opensearch.sdk.handlers.ExtensionActionRequestHandler;
import org.opensearch.sdk.action.SDKActionModule;
import org.opensearch.sdk.handlers.AcknowledgedResponseHandler;
import org.opensearch.sdk.handlers.ExtensionDependencyResponseHandler;
import org.opensearch.sdk.handlers.ExtensionsIndicesModuleNameRequestHandler;
import org.opensearch.sdk.handlers.ExtensionsIndicesModuleRequestHandler;
import org.opensearch.sdk.handlers.ExtensionsInitRequestHandler;
import org.opensearch.sdk.handlers.ExtensionsRestRequestHandler;
import org.opensearch.sdk.handlers.UpdateSettingsRequestHandler;
import org.opensearch.tasks.TaskManager;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportResponse;
import org.opensearch.transport.TransportResponseHandler;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.TransportSettings;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The primary class to run an extension.
 * <p>
 * During instantiation, this class reads extension points from the extension based on its implemented interfaces and registers necessary information with the {@link ExtensionsManager} or other OpenSearch classes.
 * <p>
 * Extensions initialize by passing an instance of themselves to the {@link #run(Extension)} method.
 */
public class ExtensionsRunner {

    private static final Logger logger = LogManager.getLogger(ExtensionsRunner.class);
    /**
     * The key for the extension runner's node name in its settings.
     */
    public static final String NODE_NAME_SETTING = "node.name";

    // The extension being run
    private final Extension extension;

    // Set when initialization is complete
    private boolean initialized = false;

    private String uniqueId;
    /**
     * This field is initialized by a call from {@link ExtensionsInitRequestHandler}.
     */
    public DiscoveryNode opensearchNode;
    private DiscoveryExtensionNode extensionNode;
    /**
     * This field is initialized by a call from {@link ExtensionsInitRequestHandler}.
     */
    private TransportService extensionTransportService = null;

    // The routes and classes which handle the REST requests
    private final ExtensionRestPathRegistry extensionRestPathRegistry = new ExtensionRestPathRegistry();
    /**
     * Custom namedXContent from the extension's getNamedXContent. This field is initialized in the constructor.
     */
    private final List<NamedXContentRegistry.Entry> customNamedXContent;
    /**
     * Custom settings from the extension's getSettings. This field is initialized in the constructor.
     */
    private final List<Setting<?>> customSettings;
    /**
     * Environment settings from OpenSearch. This field is initialized by a call from
     * {@link ExtensionsInitRequestHandler}.
     */
    private Settings environmentSettings = Settings.EMPTY;
    /**
     * Node name, host, and port. This field is initialized by a call from {@link ExtensionsInitRequestHandler}.
     */
    private final Settings settings;
    /**
     * A thread pool for the extension.
     */
    private final ThreadPool threadPool;
    /**
     * A task manager for the extension
     */
    private final TaskManager taskManager;
    /**
     * The Guice injector
     */
    private final Injector injector;

    private final SDKNamedXContentRegistry sdkNamedXContentRegistry;
    private final SDKClient sdkClient;
    private final SDKClusterService sdkClusterService;
    private final SDKTransportService sdkTransportService;
    private final SDKActionModule sdkActionModule;

    private ExtensionsInitRequestHandler extensionsInitRequestHandler = new ExtensionsInitRequestHandler(this);
    private ExtensionsIndicesModuleRequestHandler extensionsIndicesModuleRequestHandler = new ExtensionsIndicesModuleRequestHandler();
    private ExtensionsIndicesModuleNameRequestHandler extensionsIndicesModuleNameRequestHandler =
        new ExtensionsIndicesModuleNameRequestHandler();
    private ExtensionsRestRequestHandler extensionsRestRequestHandler = new ExtensionsRestRequestHandler(extensionRestPathRegistry);
    private ExtensionActionRequestHandler extensionsActionRequestHandler;

    /**
     * Instantiates a new update settings request handler
     */
    UpdateSettingsRequestHandler updateSettingsRequestHandler = new UpdateSettingsRequestHandler();

    /**
     * Instantiates a new Extensions Runner using the specified extension.
     *
     * @param extension The settings with which to start the runner.
     * @throws IOException if the runner failed to read settings or API.
     */
    protected ExtensionsRunner(Extension extension) throws IOException {
        // Link these classes together
        this.extension = extension;
        extension.setExtensionsRunner(this);

        // Initialize concrete classes needed by extensions
        // These must have getters from this class to be accessible via createComponents
        // If they require later initialization, create a concrete wrapper class and update the internals
        ExtensionSettings extensionSettings = extension.getExtensionSettings();
        this.settings = Settings.builder()
            .put(NODE_NAME_SETTING, extensionSettings.getExtensionName())
            .put(TransportSettings.BIND_HOST.getKey(), extensionSettings.getHostAddress())
            .put(TransportSettings.PORT.getKey(), extensionSettings.getHostPort())
            .build();
        this.threadPool = new ThreadPool(settings);
        this.taskManager = new TaskManager(settings, threadPool, Collections.emptySet());

        // save custom settings
        this.customSettings = extension.getSettings();
        // save custom namedXContent
        this.customNamedXContent = extension.getNamedXContent();
        // initialize NamedXContent Registry. Must happen after getting extension namedXContent
        this.sdkNamedXContentRegistry = new SDKNamedXContentRegistry(this);
        // initialize SDKClient. Must happen after getting extensionSettings
        this.sdkClient = new SDKClient(extensionSettings);
        // initialize SDKClusterService. Must happen after extension field assigned
        this.sdkClusterService = new SDKClusterService(this);
        // initialize SDKTransportService. Must happen after extension field assigned
        this.sdkTransportService = new SDKTransportService();

        // Create Guice modules for injection
        List<com.google.inject.Module> modules = new ArrayList<>();
        // Bind the concrete classes defined above, via getter
        modules.add(b -> {
            b.bind(ExtensionsRunner.class).toInstance(this);
            b.bind(Extension.class).toInstance(extension);

            b.bind(SDKNamedXContentRegistry.class).toInstance(getNamedXContentRegistry());
            b.bind(ThreadPool.class).toInstance(getThreadPool());
            b.bind(TaskManager.class).toInstance(getTaskManager());

            b.bind(SDKClient.class).toInstance(getSdkClient());
            b.bind(SDKClusterService.class).toInstance(getSdkClusterService());
            b.bind(SDKTransportService.class).toInstance(getSdkTransportService());
        });
        // Bind the return values from create components
        modules.add(this::injectComponents);
        // Bind actions from getActions
        this.sdkActionModule = new SDKActionModule(extension);
        modules.add(this.sdkActionModule);
        // Finally, perform the injection
        this.injector = Guice.createInjector(modules);

        // Perform other initialization. These should have access to injected classes.
        // initialize SDKClient action map
        initializeSdkClient();

        extensionsActionRequestHandler = new ExtensionActionRequestHandler(getSdkClient(), getSdkActionModule());

        if (extension instanceof ActionExtension) {
            // store REST handlers in the registry
            for (ExtensionRestHandler extensionRestHandler : ((ActionExtension) extension).getExtensionRestHandlers()) {
                for (Route route : extensionRestHandler.routes()) {
                    extensionRestPathRegistry.registerHandler(route.getMethod(), route.getPath(), extensionRestHandler);
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void injectComponents(Binder b) {
        extension.createComponents(this).stream().forEach(p -> b.bind((Class) p.getClass()).toInstance(p));
    }

    @SuppressWarnings("rawtypes")
    private void initializeSdkClient() {
        sdkClient.initialize(this.injector.getInstance(new Key<Map<ActionType, TransportAction>>() {
        }));
    }

    /**
     * Gets the {@link Extension} this class is running.
     * @return the extension.
     */
    public Extension getExtension() {
        return this.extension;
    }

    /**
     * Marks the extension initialized.
     */
    public void setInitialized() {
        this.initialized = true;
        logger.info("Extension initialization is complete!");
    }

    /**
     * Reports if the extension has finished initializing.
     *
     * @return true if the extension has been initialized
     */
    boolean isInitialized() {
        return this.initialized;
    }

    /**
     * Sets the TransportService. Called from {@link ExtensionsInitRequestHandler}.
     *
     * @param extensionTransportService assign value for extensionTransportService
     */
    void setExtensionTransportService(TransportService extensionTransportService) {
        this.extensionTransportService = extensionTransportService;
    }

    /**
     * Sets the Environment Settings. Called from {@link ExtensionsInitRequestHandler}.
     *
     * @param settings assign value for environmentSettings
     */
    public void setEnvironmentSettings(Settings settings) {
        this.environmentSettings = settings;
    }

    /**
     * Gets the Environment Settings. Only valid if {@link #isInitialized()} returns true.
     *
     * @return the environment settings if initialized, an empty settings object otherwise.
     */
    public Settings getEnvironmentSettings() {
        return this.environmentSettings;
    }

    /**
     * Updates the NamedXContentRegistry. Called from {@link ExtensionsInitRequestHandler}.
     */
    public void updateNamedXContentRegistry() {
        this.sdkNamedXContentRegistry.updateNamedXContentRegistry(this);
    }

    /**
     * Gets the NamedXContentRegistry. Only valid if {@link #isInitialized()} returns true.
     *
     * @return the NamedXContentRegistry if initialized, an empty registry otherwise.
     */
    public SDKNamedXContentRegistry getNamedXContentRegistry() {
        return this.sdkNamedXContentRegistry;
    }

    /**
     * Sets the Unique ID, used in REST requests to uniquely identify this extension
     *
     * @param id assign value for id
     */
    public void setUniqueId(String id) {
        this.uniqueId = id;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setOpensearchNode(DiscoveryNode opensearchNode) {
        this.opensearchNode = opensearchNode;
    }

    public void setExtensionNode(DiscoveryExtensionNode extensionNode) {
        this.extensionNode = extensionNode;
    }

    /**
     * Returns the discovery extension node set during extension initialization
     *
     * @return the extensionNode
     */
    public DiscoveryExtensionNode getExtensionNode() {
        return this.extensionNode;
    }

    public DiscoveryNode getOpensearchNode() {
        return this.opensearchNode;
    }

    public List<NamedXContentRegistry.Entry> getCustomNamedXContent() {
        return this.customNamedXContent;
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
            ExtensionsManager.REQUEST_EXTENSION_ACTION_NAME,
            ThreadPool.Names.GENERIC,
            false,
            false,
            InitializeExtensionRequest::new,
            (request, channel, task) -> channel.sendResponse(extensionsInitRequestHandler.handleExtensionInitRequest(request))
        );

        transportService.registerRequestHandler(
            ExtensionsManager.INDICES_EXTENSION_POINT_ACTION_NAME,
            ThreadPool.Names.GENERIC,
            false,
            false,
            IndicesModuleRequest::new,
            ((request, channel, task) -> channel.sendResponse(
                extensionsIndicesModuleRequestHandler.handleIndicesModuleRequest(request, transportService)
            ))

        );

        transportService.registerRequestHandler(
            ExtensionsManager.INDICES_EXTENSION_NAME_ACTION_NAME,
            ThreadPool.Names.GENERIC,
            false,
            false,
            IndicesModuleRequest::new,
            ((request, channel, task) -> channel.sendResponse(
                extensionsIndicesModuleNameRequestHandler.handleIndicesModuleNameRequest(request)
            ))
        );

        transportService.registerRequestHandler(
            ExtensionsManager.REQUEST_REST_EXECUTE_ON_EXTENSION_ACTION,
            ThreadPool.Names.GENERIC,
            false,
            false,
            ExtensionRestRequest::new,
            ((request, channel, task) -> channel.sendResponse(extensionsRestRequestHandler.handleRestExecuteOnExtensionRequest(request)))
        );

        transportService.registerRequestHandler(
            ExtensionsManager.REQUEST_EXTENSION_UPDATE_SETTINGS,
            ThreadPool.Names.GENERIC,
            false,
            false,
            UpdateSettingsRequest::new,
            ((request, channel, task) -> channel.sendResponse(updateSettingsRequestHandler.handleUpdateSettingsRequest(request)))
        );

        // This handles a remote extension request from OpenSearch or a plugin, sending an ExtensionActionResponse
        transportService.registerRequestHandler(
            ExtensionsManager.REQUEST_EXTENSION_HANDLE_TRANSPORT_ACTION,
            ThreadPool.Names.GENERIC,
            false,
            false,
            ExtensionActionRequest::new,
            ((request, channel, task) -> channel.sendResponse(extensionsActionRequestHandler.handleExtensionActionRequest(request)))
        );

        // This handles a remote extension request from another extension, sending a RemoteExtensionActionResponse
        transportService.registerRequestHandler(
            ExtensionsManager.REQUEST_EXTENSION_HANDLE_REMOTE_TRANSPORT_ACTION,
            ThreadPool.Names.GENERIC,
            false,
            false,
            ExtensionActionRequest::new,
            ((request, channel, task) -> channel.sendResponse(extensionsActionRequestHandler.handleRemoteExtensionActionRequest(request)))
        );
    }

    /**
     * Returns a list of interfaces implemented by the corresponding {@link Extension}.
     *
     * @return A list of strings matching the interface name.
     */
    public List<String> getExtensionImplementedInterfaces() {
        Set<Class<?>> interfaceSet = new HashSet<>();
        Class<?> extensionClass = getExtension().getClass();
        do {
            interfaceSet.addAll(Arrays.stream(extensionClass.getInterfaces()).collect(Collectors.toSet()));
            extensionClass = extensionClass.getSuperclass();
        } while (extensionClass != null);

        // we are making an assumption here that all the other Interfaces will be in the same package ( or will be in subpackage ) in which
        // Extension Interface belongs.
        String extensionInterfacePackageName = Extension.class.getPackageName();
        return interfaceSet.stream()
            .filter(i -> i.getPackageName().startsWith(extensionInterfacePackageName))
            .map(Class::getSimpleName)
            .collect(Collectors.toList());
    }

    /**
     * Requests that OpenSearch register the REST Actions for this extension.
     *
     * @param transportService  The TransportService defining the connection to OpenSearch.
     */
    public void sendRegisterRestActionsRequest(TransportService transportService) {
        List<String> extensionRestPaths = extensionRestPathRegistry.getRegisteredPaths();
        logger.info("Sending Register REST Actions request to OpenSearch for " + extensionRestPaths);
        AcknowledgedResponseHandler registerActionsResponseHandler = new AcknowledgedResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_REGISTER_REST_ACTIONS,
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
     * @param transportService  The TransportService defining the connection to OpenSearch.
     */
    public void sendRegisterCustomSettingsRequest(TransportService transportService) {
        logger.info("Sending Settings request to OpenSearch");
        AcknowledgedResponseHandler registerCustomSettingsResponseHandler = new AcknowledgedResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_REGISTER_CUSTOM_SETTINGS,
                new RegisterCustomSettingsRequest(getUniqueId(), customSettings),
                registerCustomSettingsResponseHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send Register Settings request to OpenSearch", e);
        }
    }

    private void sendGenericRequestWithExceptionHandling(
        TransportService transportService,
        RequestType requestType,
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
     * @param transportService  The TransportService defining the connection to OpenSearch.
     * @return The cluster state of OpenSearch
     */

    public ClusterState sendClusterStateRequest(TransportService transportService) {
        logger.info("Sending Cluster State request to OpenSearch");
        ClusterStateResponseHandler clusterStateResponseHandler = new ClusterStateResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_CLUSTER_STATE,
                new ExtensionRequest(ExtensionsManager.RequestType.REQUEST_EXTENSION_CLUSTER_STATE),
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
     * @param transportService  The TransportService defining the connection to OpenSearch
     * @return A List contains details of this extension's dependencies
     */
    public List<DiscoveryExtensionNode> sendExtensionDependencyRequest(TransportService transportService) {
        logger.info("Sending Extension Dependency Information request to Opensearch");
        ExtensionDependencyResponseHandler extensionDependencyResponseHandler = new ExtensionDependencyResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_DEPENDENCY_INFORMATION,
                new ExtensionRequest(ExtensionsManager.RequestType.REQUEST_EXTENSION_DEPENDENCY_INFORMATION, uniqueId),
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
     *
     * @param transportService  The TransportService defining the connection to OpenSearch.
     */
    public void sendClusterSettingsRequest(TransportService transportService) {
        sendGenericRequestWithExceptionHandling(
            transportService,
            ExtensionsManager.RequestType.REQUEST_EXTENSION_CLUSTER_SETTINGS,
            ExtensionsManager.REQUEST_EXTENSION_CLUSTER_SETTINGS,
            new ClusterSettingsResponseHandler()
        );
    }

    /**
     * Requests the environment settings from OpenSearch. The result will be handled by a {@link EnvironmentSettingsResponseHandler}.
     *
     * @param transportService  The TransportService defining the connection to OpenSearch.
     * @return A Setting object from the OpenSearch Node environment
     */
    public Settings sendEnvironmentSettingsRequest(TransportService transportService) {
        logger.info("Sending Environment Settings request to OpenSearch");
        EnvironmentSettingsResponseHandler environmentSettingsResponseHandler = new EnvironmentSettingsResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_ENVIRONMENT_SETTINGS,
                new ExtensionRequest(ExtensionsManager.RequestType.REQUEST_EXTENSION_ENVIRONMENT_SETTINGS),
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
     * @param transportService  The TransportService defining the connection to OpenSearch.
     * @param settingUpdateConsumers A map of setting objects and their corresponding consumers
     */
    public void sendAddSettingsUpdateConsumerRequest(
        TransportService transportService,
        Map<Setting<?>, Consumer<?>> settingUpdateConsumers
    ) {
        // Determine if there are setting update consumers to be registered
        if (!settingUpdateConsumers.isEmpty()) {
            // Register setting update consumers to UpdateSettingsRequestHandler
            this.updateSettingsRequestHandler.registerSettingUpdateConsumer(settingUpdateConsumers);

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
                new AddSettingsUpdateConsumerRequest(this.extensionNode, componentSettings),
                acknowledgedResponseHandler
            );
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public SDKClient getSdkClient() {
        return sdkClient;
    }

    public SDKClusterService getSdkClusterService() {
        return sdkClusterService;
    }

    /**
     * Updates the SDKClusterService. Called from {@link ExtensionsInitRequestHandler}.
     */
    public void updateSdkClusterService() {
        this.sdkClusterService.updateSdkClusterSettings();
    }

    public SDKActionModule getSdkActionModule() {
        return sdkActionModule;
    }

    public TransportService getExtensionTransportService() {
        return extensionTransportService;
    }

    public SDKTransportService getSdkTransportService() {
        return sdkTransportService;
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
        ExtensionsRunner runner = new ExtensionsRunner(extension);
        // initialize the transport service
        NettyTransport nettyTransport = new NettyTransport(runner);
        runner.extensionTransportService = nettyTransport.initializeExtensionTransportService(runner.getSettings(), runner.getThreadPool());
        // TODO: merge above line with below line when refactoring out extensionTransportService
        runner.getSdkTransportService().setTransportService(runner.extensionTransportService);
        runner.startActionListener(0);
    }

}
