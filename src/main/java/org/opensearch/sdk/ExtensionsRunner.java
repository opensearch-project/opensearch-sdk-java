/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
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
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.network.NetworkModule;
import org.opensearch.common.network.NetworkService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.PageCacheRecycler;
import org.opensearch.discovery.PluginRequest;
import org.opensearch.discovery.PluginResponse;
import org.opensearch.extensions.ExtensionRequest;
import org.opensearch.extensions.ExtensionsOrchestrator;
import org.opensearch.index.IndicesModuleNameResponse;
import org.opensearch.index.IndicesModuleRequest;
import org.opensearch.index.IndicesModuleResponse;
import org.opensearch.indices.IndicesModule;
import org.opensearch.indices.breaker.CircuitBreakerService;
import org.opensearch.indices.breaker.NoneCircuitBreakerService;
import org.opensearch.sdk.netty4.Netty4Transport;
import org.opensearch.sdk.netty4.SharedGroupFactory;
import org.opensearch.search.SearchModule;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
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
    private ExtensionSettings extensionSettings = readExtensionSettings();
    private DiscoveryNode opensearchNode;

    /**
     * Instantiates a new Extensions Runner.
     *
     * @throws IOException if the runner failed to connect to the OpenSearch cluster.
     */
    public ExtensionsRunner() throws IOException {}

    private final Settings settings = Settings.builder()
        .put("node.name", extensionSettings.getExtensionname())
        .put(TransportSettings.BIND_HOST.getKey(), extensionSettings.getHostaddress())
        .put(TransportSettings.PORT.getKey(), extensionSettings.getHostport())
        .build();
    private final Logger logger = LogManager.getLogger(ExtensionsRunner.class);
    private final TransportInterceptor NOOP_TRANSPORT_INTERCEPTOR = new TransportInterceptor() {
    };

    private ExtensionSettings readExtensionSettings() throws IOException {
        File file = new File(ExtensionSettings.EXTENSION_DESCRIPTOR);
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        ExtensionSettings extensionSettings = objectMapper.readValue(file, ExtensionSettings.class);
        return extensionSettings;
    }

    private void setOpensearchNode(DiscoveryNode opensearchNode) {
        this.opensearchNode = opensearchNode;
    }

    private DiscoveryNode getOpensearchNode() {
        return opensearchNode;
    }

    /**
     * Handles a plugin request from OpenSearch.  This is the first request for the transport communication and will initialize the extension and will be a part of OpenSearch bootstrap.
     *
     * @param pluginRequest  The request to handle.
     * @return A response to OpenSearch validating that this is an extension.
     */
    PluginResponse handlePluginsRequest(PluginRequest pluginRequest) {
        logger.info("Registering Plugin Request received from OpenSearch");
        PluginResponse pluginResponse = new PluginResponse("RealExtension");
        opensearchNode = pluginRequest.getSourceNode();
        setOpensearchNode(opensearchNode);
        return pluginResponse;
    }

    /**
     * Handles a request for extension point indices from OpenSearch.  The {@link #handlePluginsRequest(PluginRequest)} method must have been called first to initialize the extension.
     *
     * @param indicesModuleRequest  The request to handle.
     * @param transportService  The transport service communicating with OpenSearch.
     * @return A response to OpenSearch with this extension's index and search listeners.
     */
    IndicesModuleResponse handleIndicesModuleRequest(IndicesModuleRequest indicesModuleRequest, TransportService transportService) {
        logger.info("Registering Indices Module Request received from OpenSearch");
        IndicesModuleResponse indicesModuleResponse = new IndicesModuleResponse(true, true, true);

        // handlePluginsRequest will set the opensearchNode while bootstraping of OpenSearch
        DiscoveryNode opensearchNode = getOpensearchNode();
        transportService.connectToNode(opensearchNode);
        sendAddSettingsUpdateConsumer(transportService);
        return indicesModuleResponse;
    }

    /**
     * Handles a request for extension name from OpenSearch.  The {@link #handlePluginsRequest(PluginRequest)} method must have been called first to initialize the extension.
     *
     * @param indicesModuleRequest  The request to handle.
     * @return A response acknowledging the request.
     */
    IndicesModuleNameResponse handleIndicesModuleNameRequest(IndicesModuleRequest indicesModuleRequest) {
        // Works as beforeIndexRemoved
        logger.info("Registering Indices Module Name Request received from OpenSearch");
        IndicesModuleNameResponse indicesModuleNameResponse = new IndicesModuleNameResponse(true);
        return indicesModuleNameResponse;
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
     * Creates a TransportService object. This object will control communication between the extension and OpenSearch.
     *
     * @param settings  The transport settings to configure.
     * @return The configured TransportService object.
     */
    public TransportService createTransportService(Settings settings) {

        ThreadPool threadPool = new ThreadPool(settings);

        Netty4Transport transport = getNetty4Transport(settings, threadPool);

        final ConnectionManager connectionManager = new ClusterConnectionManager(settings, transport);

        // create transport service
        return new TransportService(
            settings,
            transport,
            threadPool,
            NOOP_TRANSPORT_INTERCEPTOR,
            boundAddress -> DiscoveryNode.createLocal(
                Settings.builder().put("node.name", extensionSettings.getExtensionname()).build(),
                boundAddress.publishAddress(),
                randomBase64UUID()
            ),
            null,
            emptySet(),
            connectionManager
        );
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
            PluginRequest::new,
            (request, channel, task) -> channel.sendResponse(handlePluginsRequest(request))
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

    public void sendAddSettingsUpdateConsumer(TransportService transportService) {
        Settings tmpsettings = Settings.builder().put("node.name", "my-node").build();
        try {
            transportService.sendRequest(
                    opensearchNode,
                    "internal:discovery/clustersettings/addsettingsupdateconsumer",
                    new ExtensionRequest(ExtensionsOrchestrator.RequestType.valueOf("internal:discovery/clustersettings/addsettingsupdateconsumer")),
                    new TransportResponseHandler<TransportResponse>() {
                        @Override
                        public void handleResponse(TransportResponse response) {
                            logger.info("Send request to OpenSearch");
                        }

                        @Override
                        public void handleException(TransportException exp) {
                            logger.info("Exception", exp);
                        }

                        @Override
                        public String executor() {
                            return ThreadPool.Names.GENERIC;
                        }

                        @Override
                        public TransportResponse read(StreamInput in) throws IOException {
                            return new TransportResponse() {
                                @Override
                                public void writeTo(StreamOutput out) throws IOException {
                                    Settings.writeSettingsToStream(tmpsettings, out);
                                }
                            };
                        }
                    }
            );
        } catch(Exception e) {
            logger.error(e.toString());
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
     * Run the Extension. Imports settings, establishes a connection with an OpenSearch cluster via a Transport Service, and sets up a listener for responses.
     *
     * @param args  Unused
     * @throws IOException if the runner failed to connect to the OpenSearch cluster.
     */
    public static void main(String[] args) throws IOException {

        ExtensionsRunner extensionsRunner = new ExtensionsRunner();

        // configure and retrieve transport service with settings
        Settings settings = extensionsRunner.getSettings();
        TransportService transportService = extensionsRunner.createTransportService(settings);

        // start transport service and action listener
        extensionsRunner.startTransportService(transportService);
        extensionsRunner.startActionListener(0);
    }

}
