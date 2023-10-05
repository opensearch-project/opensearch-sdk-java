/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.opensearch.Version;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.network.NetworkService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.PageCacheRecycler;
import org.opensearch.core.indices.breaker.CircuitBreakerService;
import org.opensearch.core.indices.breaker.NoneCircuitBreakerService;
import org.opensearch.sdk.ssl.DefaultSslKeyStore;
import org.opensearch.sdk.ssl.SSLConfigConstants;
import org.opensearch.sdk.ssl.SSLNettyTransport;
import org.opensearch.sdk.ssl.SslKeyStore;
import org.opensearch.telemetry.tracing.noop.NoopTracer;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.SharedGroupFactory;
import org.opensearch.transport.TransportInterceptor;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.netty4.Netty4Transport;

import java.nio.file.Path;
import java.util.Collections;

import static java.util.Collections.emptySet;
import static org.opensearch.common.UUIDs.randomBase64UUID;

/**
 * This class initializes a Netty4Transport object and control communication between the extension and OpenSearch.
 */

public class NettyTransport {
    private static final String NODE_NAME_SETTING = "node.name";
    private final ExtensionsRunner extensionsRunner;
    private final TransportInterceptor NOOP_TRANSPORT_INTERCEPTOR = new TransportInterceptor() {
    };

    /**
     * @param extensionsRunner Instantiate this object with a reference to the ExtensionsRunner.
     */
    public NettyTransport(ExtensionsRunner extensionsRunner) {
        this.extensionsRunner = extensionsRunner;
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

        final CircuitBreakerService circuitBreakerService = new NoneCircuitBreakerService();

        boolean transportSSLEnabled = settings.getAsBoolean(
            SSLConfigConstants.SSL_TRANSPORT_ENABLED,
            SSLConfigConstants.SSL_TRANSPORT_ENABLED_DEFAULT
        );

        Netty4Transport transport = new Netty4Transport(
            settings,
            Version.CURRENT,
            threadPool,
            networkService,
            pageCacheRecycler,
            extensionsRunner.getNamedWriteableRegistry().getRegistry(),
            circuitBreakerService,
            new SharedGroupFactory(settings),
            NoopTracer.INSTANCE
        );

        if (transportSSLEnabled) {
            Path configPath = Path.of("").toAbsolutePath().resolve("config");
            SslKeyStore sks = new DefaultSslKeyStore(settings, configPath);
            transport = new SSLNettyTransport(
                settings,
                Version.CURRENT,
                threadPool,
                networkService,
                pageCacheRecycler,
                extensionsRunner.getNamedWriteableRegistry().getRegistry(),
                circuitBreakerService,
                sks,
                new SharedGroupFactory(settings)
            );
        }

        return transport;
    }

    /**
     * Initializes the TransportService object for this extension. This object will control communication between the extension and OpenSearch.
     *
     * @param settings  The transport settings to configure.
     * @param threadPool The thread pool to use to start transport service.
     * @return The initialized TransportService object.
     */
    public TransportService initializeExtensionTransportService(Settings settings, ThreadPool threadPool) {

        Netty4Transport transport = getNetty4Transport(settings, threadPool);

        // create transport service
        TransportService transportService = new TransportService(
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
            NoopTracer.INSTANCE
        );
        extensionsRunner.startTransportService(transportService);
        return transportService;
    }

}
