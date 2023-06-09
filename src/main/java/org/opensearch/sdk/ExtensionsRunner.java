/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionType;
import org.opensearch.action.support.TransportAction;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.discovery.InitializeExtensionRequest;
import org.opensearch.extensions.DiscoveryExtensionNode;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.UpdateSettingsRequest;
import org.opensearch.extensions.action.ExtensionActionRequest;
import org.opensearch.sdk.action.SDKActionModule;
import org.opensearch.sdk.api.ActionExtension;
import org.opensearch.sdk.handlers.ExtensionActionRequestHandler;
import org.opensearch.sdk.handlers.ExtensionsIndicesModuleNameRequestHandler;
import org.opensearch.sdk.handlers.ExtensionsIndicesModuleRequestHandler;
import org.opensearch.sdk.handlers.ExtensionsInitRequestHandler;
import org.opensearch.sdk.handlers.ExtensionsRestRequestHandler;
import org.opensearch.sdk.handlers.UpdateSettingsRequestHandler;
import org.opensearch.sdk.rest.ExtensionRestHandler;
import org.opensearch.sdk.rest.ExtensionRestPathRegistry;
import org.opensearch.tasks.TaskManager;
import org.opensearch.threadpool.ExecutorBuilder;
import org.opensearch.threadpool.RunnableTaskExecutionListener;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.TransportSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_ENABLED;

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

    /**
     * This field is initialized by a call from {@link ExtensionsInitRequestHandler}.
     */
    private DiscoveryExtensionNode extensionNode;

    // The routes and classes which handle the REST requests
    private final ExtensionRestPathRegistry extensionRestPathRegistry = new ExtensionRestPathRegistry();
    /**
     * Custom namedXContent from the extension's getNamedXContent. This field is initialized in the constructor.
     */
    private final List<NamedXContentRegistry.Entry> customNamedXContent;
    /**
     * Custom namedWriteable from the extension's getNamedWriteable. This field is initialized in the constructor.
     */
    private final List<NamedWriteableRegistry.Entry> customNamedWriteables;

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
    private final SDKNamedWriteableRegistry sdkNamedWriteableRegistry;
    private final SDKClient sdkClient;
    private final SDKClusterService sdkClusterService;
    private final SDKTransportService sdkTransportService;
    private final SDKActionModule sdkActionModule;

    private final ExtensionsInitRequestHandler extensionsInitRequestHandler = new ExtensionsInitRequestHandler(this);
    private final ExtensionsIndicesModuleRequestHandler extensionsIndicesModuleRequestHandler = new ExtensionsIndicesModuleRequestHandler();
    private final ExtensionsIndicesModuleNameRequestHandler extensionsIndicesModuleNameRequestHandler =
        new ExtensionsIndicesModuleNameRequestHandler();
    private final ExtensionsRestRequestHandler extensionsRestRequestHandler;
    private final ExtensionActionRequestHandler extensionsActionRequestHandler;
    private final AtomicReference<RunnableTaskExecutionListener> runnableTaskListener;
    private final IndexNameExpressionResolver indexNameExpressionResolver;

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
        Settings.Builder settingsBuilder = Settings.builder()
            .put(NODE_NAME_SETTING, extensionSettings.getExtensionName())
            .put(TransportSettings.BIND_HOST.getKey(), extensionSettings.getHostAddress())
            .put(TransportSettings.PORT.getKey(), extensionSettings.getHostPort());
        boolean sslEnabled = extensionSettings.getSecuritySettings().containsKey(SSL_TRANSPORT_ENABLED)
            && "true".equals(extensionSettings.getSecuritySettings().get(SSL_TRANSPORT_ENABLED));
        if (sslEnabled) {
            for (String settingsKey : ExtensionSettings.SECURITY_SETTINGS_KEYS) {
                addSettingsToBuilder(settingsBuilder, settingsKey, extensionSettings);
            }
        }
        String sslText = sslEnabled ? "enabled" : "disabled";
        logger.info("SSL is " + sslText + " for transport");
        this.settings = settingsBuilder.build();

        final List<ExecutorBuilder<?>> executorBuilders = extension.getExecutorBuilders(settings);

        this.runnableTaskListener = new AtomicReference<>();
        this.threadPool = new ThreadPool(settings, runnableTaskListener, executorBuilders.toArray(new ExecutorBuilder[0]));
        this.indexNameExpressionResolver = new IndexNameExpressionResolver(this.threadPool.getThreadContext());
        this.taskManager = new TaskManager(settings, threadPool, Collections.emptySet());

        // save custom settings
        this.customSettings = extension.getSettings();
        // save custom namedXContent
        this.customNamedXContent = extension.getNamedXContent();
        // save custom namedWriteable
        this.customNamedWriteables = extension.getNamedWriteables();
        // initialize NamedXContent Registry.
        this.sdkNamedXContentRegistry = new SDKNamedXContentRegistry(this);
        // initialize RestRequest Handler. Must happen after instantiating SDKNamedXContentRegistry
        this.extensionsRestRequestHandler = new ExtensionsRestRequestHandler(extensionRestPathRegistry, sdkNamedXContentRegistry);
        // initialize NamedWriteable Registry. Must happen after getting extension namedWriteable
        this.sdkNamedWriteableRegistry = new SDKNamedWriteableRegistry(this);

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
            b.bind(IndexNameExpressionResolver.class).toInstance(indexNameExpressionResolver);

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

        extensionsActionRequestHandler = new ExtensionActionRequestHandler(getSdkClient());

        if (extension instanceof ActionExtension) {
            // store REST handlers in the registry
            for (ExtensionRestHandler extensionRestHandler : ((ActionExtension) extension).getExtensionRestHandlers()) {
                extensionRestPathRegistry.registerHandler(extensionRestHandler);
            }
        }
    }

    private void addSettingsToBuilder(Settings.Builder settingsBuilder, String settingKey, ExtensionSettings extensionSettings) {
        if (extensionSettings.getSecuritySettings().containsKey(settingKey)) {
            settingsBuilder.put(settingKey, extensionSettings.getSecuritySettings().get(settingKey));
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
     * Updates the NamedXContentRegistry. Called from {@link ExtensionsInitRequestHandler}.
     */
    public void updateNamedWriteableRegistry() {
        this.sdkNamedWriteableRegistry.updateNamedWriteableRegistry(this);
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
     * Gets the SDKNamedWriteableRegistry. Only valid if {@link #isInitialized()} returns true.
     *
     * @return the NamedWriteableRegistry if initialized, an empty registry otherwise.
     */
    public SDKNamedWriteableRegistry getNamedWriteableRegistry() {
        return this.sdkNamedWriteableRegistry;
    }

    /**
     * Sets the {@link DiscoveryExtensionNode} instance for this object.
     * This method must be called before any other method that depends on the extension node is invoked.
     *
     * @param extensionNode The {@link DiscoveryExtensionNode} instance to be set.
     */
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

    /**
     * Returns the custom NamedXContent registry entries registered by this module.
     * These entries are used to deserialize custom JSON content in requests and responses.
     *
     * @return The list of custom NamedXContent registry entries.
     */
    public List<NamedXContentRegistry.Entry> getCustomNamedXContent() {
        return this.customNamedXContent;
    }

    /**
     * Returns a list of custom named writeable registry entries.
     * These entries represent custom objects that can be serialized and deserialized using the NamedWriteable API in OpenSearch.
     *
     * @return A list of NamedWriteableRegistry.Entry objects representing custom named writeable registry entries.
     */
    public List<NamedWriteableRegistry.Entry> getCustomNamedWriteables() {
        return this.customNamedWriteables;
    }

    /**
     * Returns the {@link IndexNameExpressionResolver} instance associated with this object.
     *
     * @return the {@link IndexNameExpressionResolver} instance.
     */
    public IndexNameExpressionResolver getIndexNameExpressionResolver() {
        return this.indexNameExpressionResolver;
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
     * Returns the settings object associated with this instance.
     * The settings object contains the configuration options for this instance, such as the cluster name and node name.
     *
     * @return The settings object for this instance.
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Returns the custom settings from the extension's getSettings.
     *
     * @return The custom settings object for this instance.
     */
    public List<Setting<?>> getCustomSettings() {
        return customSettings;
    }

    /**
     * Returns the routes and classes which handle the REST requests
     *
     * @return The ExtensionRestPathRegistry object for this instance.
     */
    public ExtensionRestPathRegistry getExtensionRestPathRegistry() {
        return extensionRestPathRegistry;
    }

    /**
     * Returns the update settings request handler
     *
     * @return The UpdateSettingsRequestHandler object for this instance.
     */
    public UpdateSettingsRequestHandler getUpdateSettingsRequestHandler() {
        return updateSettingsRequestHandler;
    }

    /**
     * Returns the thread pool associated with this object.
     *
     * @return The thread pool instance.
     */
    public ThreadPool getThreadPool() {
        return threadPool;
    }

    /**
     * Returns the TaskManager instance associated with this object.
     *
     * @return The TaskManager instance.
     */
    public TaskManager getTaskManager() {
        return taskManager;
    }

    /**
     * Returns the SDK client instance used by this class.
     *
     * @return The SDK client instance.
     */
    public SDKClient getSdkClient() {
        return sdkClient;
    }

    /**
     * @return The SDKClusterService instance associated with this object.
     */
    public SDKClusterService getSdkClusterService() {
        return sdkClusterService;
    }

    /**
     * Updates the SDKClusterService. Called from {@link ExtensionsInitRequestHandler}.
     */
    public void updateSdkClusterService() {
        this.sdkClusterService.updateSdkClusterSettings();
        this.sdkClusterService.updateSdkClusterName();
    }

    /**
     * Returns the {@link SDKActionModule} associated with this instance.
     * This module provides access to SDK-specific actions and functionality.
     *
     * @return The {@link SDKActionModule} associated with this instance.
     */
    public SDKActionModule getSdkActionModule() {
        return sdkActionModule;
    }

    /**
     * Returns the SDKTransportService instance associated with this class.
     *
     * @return The SDKTransportService instance.
     */
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
        runner.getSdkTransportService()
            .setTransportService(nettyTransport.initializeExtensionTransportService(runner.getSettings(), runner.getThreadPool()));
        runner.startActionListener(0);
    }

}
