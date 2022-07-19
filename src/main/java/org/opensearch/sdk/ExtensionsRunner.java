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
import org.opensearch.common.io.stream.InputStreamStreamInput;
import org.opensearch.common.io.stream.NamedWriteable;
import org.opensearch.common.io.stream.NamedWriteableAwareStreamInput;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.NamedWriteableRegistryParseRequest;
import org.opensearch.common.io.stream.NamedWriteableRegistryParseResponse;
import org.opensearch.extensions.DefaultExtensionPointRequest;
import org.opensearch.common.io.stream.NamedWriteableRegistryResponse;
import org.opensearch.common.io.stream.StreamInput;
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
import org.opensearch.search.SearchModule;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.ClusterConnectionManager;
import org.opensearch.transport.ConnectionManager;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.TransportSettings;
import org.opensearch.transport.TransportInterceptor;

import org.opensearch.sdk.netty4.Netty4Transport;
import org.opensearch.sdk.netty4.SharedGroupFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

public class ExtensionsRunner {
    private ExtensionSettings extensionSettings = readExtensionSettings();
    private DiscoveryNode opensearchNode;
    private List<NamedWriteableRegistry.Entry> namedWriteables = getNamedWriteables();
    final private NamedWriteableRegistry namedWriteableRegistry = new NamedWriteableRegistry(namedWriteables);

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

    // current placeholder for extension point override getNamedWriteables()
    private List<NamedWriteableRegistry.Entry> getNamedWriteables() {

        List<NamedWriteableRegistry.Entry> namedWriteables = new ArrayList<>();
        // extensions will add named writeable registry entries here
        return namedWriteables;
    }

    private void setOpensearchNode(DiscoveryNode opensearchNode) {
        this.opensearchNode = opensearchNode;
    }

    private DiscoveryNode getOpensearchNode() {
        return opensearchNode;
    }

    PluginResponse handlePluginsRequest(PluginRequest pluginRequest) {
        logger.info("Registering Plugin Request received from OpenSearch");
        PluginResponse pluginResponse = new PluginResponse("RealExtension");
        opensearchNode = pluginRequest.getSourceNode();
        setOpensearchNode(opensearchNode);
        return pluginResponse;
    }

    NamedWriteableRegistryResponse handleNamedWriteableRegistryRequest(DefaultExtensionPointRequest request) {

        logger.info("Registering Named Writeable Registry Request recieved from OpenSearch.");

        // iterate through Extensions's named writeables and add to extension entries
        Map<String, Class> extensionEntries = new HashMap<>();
        for (NamedWriteableRegistry.Entry entry : this.namedWriteables) {
            extensionEntries.put(entry.name, entry.categoryClass);
        }
        NamedWriteableRegistryResponse namedWriteableRegistryResponse = new NamedWriteableRegistryResponse(extensionEntries);
        return namedWriteableRegistryResponse;
    }

    <C extends NamedWriteable> NamedWriteableRegistryParseResponse handleNamedWriteableRegistryParseRequest(
        NamedWriteableRegistryParseRequest request
    ) throws IOException {

        logger.info("Registering Named Writeable Registry Parse request from OpenSearch");
        boolean status = false;

        // extract data from request and procress fully qualified category class name into class instance
        Class<C> categoryClass = (Class<C>) request.getCategoryClass();
        byte[] context = request.getContext();

        // transform byte array context into an input stream
        try (InputStream inputStream = new ByteArrayInputStream(context, 0, context.length)) {

            // convert input stream to stream input
            try (
                StreamInput streamInput = new NamedWriteableAwareStreamInput(
                    new InputStreamStreamInput(inputStream),
                    namedWriteableRegistry
                )
            ) {

                // NamedWriteableAwareStreamInput extracts name from StreamInput, then uses both category class and name to extract
                // reader from provided registry
                // reader is then applied to the StreamInput object generated from the byte array (context)
                try {
                    C c = streamInput.readNamedWriteable(categoryClass);

                    // TODO : current parse response to OpenSearch includes only the status of the parse request. Further research
                    // needed to determine the workflow for extensions to utilize parsed objects after deserialization but this will be
                    // within the scope of dynamic registration.
                    status = true;
                } catch (UnsupportedOperationException e) {
                    logger.info("Failed to parse named writeable", e);
                }

            }
        }

        NamedWriteableRegistryParseResponse namedWriteableRegistryParseResponse = new NamedWriteableRegistryParseResponse(status);
        return namedWriteableRegistryParseResponse;
    }

    IndicesModuleResponse handleIndicesModuleRequest(IndicesModuleRequest indicesModuleRequest, TransportService transportService) {
        logger.info("Registering Indices Module Request received from OpenSearch");
        IndicesModuleResponse indicesModuleResponse = new IndicesModuleResponse(true, true, true);

        // handlePluginsRequest will set the opensearchNode while bootstraping of OpenSearch
        DiscoveryNode opensearchNode = getOpensearchNode();
        transportService.connectToNode(opensearchNode);
        return indicesModuleResponse;
    }

    // Works as beforeIndexRemoved
    IndicesModuleNameResponse handleIndicesModuleNameRequest(IndicesModuleRequest indicesModuleRequest) {
        logger.info("Registering Indices Module Name Request received from OpenSearch");
        IndicesModuleNameResponse indicesModuleNameResponse = new IndicesModuleNameResponse(true);
        return indicesModuleNameResponse;
    }

    // method : build netty transport
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

    public TransportService createTransportService(Settings settings) throws IOException {

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

    // manager method for transport service
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
            ExtensionsOrchestrator.REQUEST_EXTENSION_NAMED_WRITEABLE_REGISTRY,
            ThreadPool.Names.GENERIC,
            false,
            false,
            DefaultExtensionPointRequest::new,
            (request, channel, task) -> channel.sendResponse(handleNamedWriteableRegistryRequest(request))
        );

        transportService.registerRequestHandler(
            ExtensionsOrchestrator.REQUEST_EXTENSION_PARSE_NAMED_WRITEABLE,
            ThreadPool.Names.GENERIC,
            NamedWriteableRegistryParseRequest::new,
            (request, channel, task) -> channel.sendResponse(handleNamedWriteableRegistryParseRequest(request))
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

    // Extension can use this API to get ClusterState from OpenSearch
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

    // Extension can use this API to get ClusterSettings from OpenSearch
    public void sendClusterSettingRequest(TransportService transportService) {
        logger.info("Sending Cluster Settings request to OpenSearch");
        ClusterSettingsResponseHandler clusterSettingResponseHandler = new ClusterSettingsResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsOrchestrator.REQUEST_EXTENSION_CLUSTER_SETTINGS,
                new ExtensionRequest(ExtensionsOrchestrator.RequestType.REQUEST_EXTENSION_CLUSTER_SETTINGS),
                clusterSettingResponseHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send Cluster Settings request to OpenSearch", e);
        }
    }

    // Extension can use this API to get LocalNode from OpenSearch
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

    // manager method for action listener
    public void startActionListener(int timeout) {
        final ActionListener actionListener = new ActionListener();
        actionListener.runActionListener(true, timeout);
    }

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
