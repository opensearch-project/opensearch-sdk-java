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
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.opensearch.Version;
import org.opensearch.cluster.ClusterModule;
import org.opensearch.cluster.ClusterServiceRequest;
import org.opensearch.cluster.CreateComponentResponse;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.network.NetworkModule;
import org.opensearch.common.network.NetworkService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.PageCacheRecycler;
import org.opensearch.discovery.PluginRequest;
import org.opensearch.discovery.PluginResponse;
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
import org.opensearch.transport.TransportException;
import org.opensearch.transport.TransportResponseHandler;

import org.opensearch.sdk.netty4.Netty4Transport;
import org.opensearch.sdk.netty4.SharedGroupFactory;

import org.opensearch.transport.TransportInterceptor;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static org.opensearch.common.UUIDs.randomBase64UUID;

public class ExtensionsRunner {

    public static final String REQUEST_EXTENSION_ACTION_NAME = "internal:discovery/extensions";

    private static ExtensionSettings extensionSettings = null;

    private DiscoveryNode opensearchNode = null;

    static {
        try {
            extensionSettings = getExtensionSettings();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final Settings settings = Settings.builder()
        .put("node.name", extensionSettings.getExtensionname())
        .put(TransportSettings.BIND_HOST.getKey(), extensionSettings.getHostaddress())
        .put(TransportSettings.PORT.getKey(), extensionSettings.getHostport())
        .build();
    private static final Logger logger = LogManager.getLogger(ExtensionsRunner.class);
    public static final TransportInterceptor NOOP_TRANSPORT_INTERCEPTOR = new TransportInterceptor() {
    };

    public ExtensionsRunner() throws IOException {}

    public static ExtensionSettings getExtensionSettings() throws IOException {
        File file = new File(ExtensionSettings.EXTENSION_DESCRIPTOR);
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        ExtensionSettings extensionSettings = objectMapper.readValue(file, ExtensionSettings.class);
        return extensionSettings;
    }

    PluginResponse handlePluginsRequest(PluginRequest pluginRequest) {
        logger.info("Handling Plugins Request");
        PluginResponse pluginResponse = new PluginResponse("RealExtension");
        opensearchNode = pluginRequest.getSourceNode();
        return pluginResponse;
    }

    IndicesModuleResponse handleIndicesModuleRequest(IndicesModuleRequest indicesModuleRequest, TransportService transportService) {
        logger.info("Indices Module Request");
        IndicesModuleResponse indicesModuleResponse = new IndicesModuleResponse(true, true, true);

        // CreateComponent
        transportService.connectToNode(opensearchNode);
        final CountDownLatch inProgressLatch = new CountDownLatch(1);

        final TransportResponseHandler<CreateComponentResponse> createComponentResponseHandler = new TransportResponseHandler<
            CreateComponentResponse>() {

            @Override
            public void handleResponse(CreateComponentResponse response) {
                logger.info("received {}", response);
                inProgressLatch.countDown();
            }

            @Override
            public void handleException(TransportException exp) {
                logger.debug(new ParameterizedMessage("CreateComponentRequest failed"), exp);
                // inProgressLatch.countDown();
            }

            @Override
            public String executor() {
                return ThreadPool.Names.GENERIC;
            }

            @Override
            public CreateComponentResponse read(StreamInput in) throws IOException {
                return new CreateComponentResponse(in);
            }
        };
        try {
            logger.info("Sending request to opensearch");
            transportService.connectToNode(opensearchNode);
            transportService.sendRequest(
                opensearchNode,
                ExtensionsOrchestrator.REQUEST_EXTENSION_CREATE_COMPONENT,
                new ClusterServiceRequest(true),
                createComponentResponseHandler
            );
            inProgressLatch.await(100, TimeUnit.SECONDS);
            logger.info("Received response from OpenSearch");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.toString());
        }

        return indicesModuleResponse;
    }

    // Works as beforeIndexRemoved
    IndicesModuleNameResponse handleIndicesModuleNameRequest(IndicesModuleRequest indicesModuleRequest) {
        logger.info("Indices Module Name Request");
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

    public TransportService getTransportService(Settings settings) throws IOException {

        ThreadPool threadPool = new ThreadPool(settings);

        Netty4Transport transport = getNetty4Transport(settings, threadPool);

        final ConnectionManager connectionManager = new ClusterConnectionManager(settings, transport);

        // create transport service
        final TransportService transportService = new TransportService(
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

        return transportService;
    }

    // manager method for transport service
    public void startTransportService(TransportService transportService) {

        // start transport service and accept incoming requests
        transportService.start();
        transportService.acceptIncomingRequests();
        transportService.registerRequestHandler(
            REQUEST_EXTENSION_ACTION_NAME,
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

    // manager method for action listener
    public void startActionListener(int timeout) {
        final ActionListener actionListener = new ActionListener();
        actionListener.runActionListener(true, timeout);
    }

    public static void main(String[] args) throws IOException {

        ExtensionsRunner extensionsRunner = new ExtensionsRunner();

        // configure and retrieve transport service with settings
        TransportService transportService = extensionsRunner.getTransportService(settings);

        // start transport service and action listener
        extensionsRunner.startTransportService(transportService);
        extensionsRunner.startActionListener(0);
    }

}
