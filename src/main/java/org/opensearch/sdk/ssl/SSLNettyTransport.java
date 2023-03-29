/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.ssl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.SslHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.ExceptionsHelper;
import org.opensearch.Version;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.network.NetworkService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.PageCacheRecycler;
import org.opensearch.indices.breaker.CircuitBreakerService;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.SharedGroupFactory;
import org.opensearch.transport.TcpChannel;
import org.opensearch.transport.netty4.Netty4Transport;

public class SSLNettyTransport extends Netty4Transport {

    private static final Logger logger = LogManager.getLogger(SSLNettyTransport.class);
    private final SslKeyStore ossks;

    public SSLNettyTransport(final Settings settings, final Version version, final ThreadPool threadPool, final NetworkService networkService,
                             final PageCacheRecycler pageCacheRecycler, final NamedWriteableRegistry namedWriteableRegistry,
                             final CircuitBreakerService circuitBreakerService, final SslKeyStore ossks, SharedGroupFactory sharedGroupFactory) {
        super(settings, version, threadPool, networkService, pageCacheRecycler, namedWriteableRegistry, circuitBreakerService, sharedGroupFactory);

        this.ossks = ossks;
    }

    @Override
    public void onException(TcpChannel channel, Exception e) {

        Throwable cause = e;

        if (e instanceof DecoderException && e != null) {
            cause = e.getCause();
        }

        logger.error("Exception during establishing a SSL connection: " + cause, cause);

        super.onException(channel, e);
    }

    @Override
    protected ChannelHandler getServerChannelInitializer(String name) {
        return new SSLServerChannelInitializer(name);
    }

    @Override
    protected ChannelHandler getClientChannelInitializer(DiscoveryNode node) {
        return new SSLClientChannelInitializer(node);
    }

    protected class SSLServerChannelInitializer extends Netty4Transport.ServerChannelInitializer {

        public SSLServerChannelInitializer(String name) {
            super(name);
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            super.initChannel(ch);

            final SslHandler sslHandler = new SslHandler(ossks.createServerTransportSSLEngine());
            ch.pipeline().addFirst("ssl_server", sslHandler);
        }

        @Override
        public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (cause instanceof DecoderException && cause != null) {
                cause = cause.getCause();
            }

            logger.error("Exception during establishing a SSL connection: " + cause, cause);

            super.exceptionCaught(ctx, cause);
        }
    }

    protected static class ClientSSLHandler extends ChannelOutboundHandlerAdapter {
        private final Logger log = LogManager.getLogger(this.getClass());
        private final SslKeyStore sks;
        private final boolean hostnameVerificationEnabled;
        private final boolean hostnameVerificationResovleHostName;


        private ClientSSLHandler(final SslKeyStore sks, final boolean hostnameVerificationEnabled,
                                 final boolean hostnameVerificationResovleHostName) {
            this.sks = sks;
            this.hostnameVerificationEnabled = hostnameVerificationEnabled;
            this.hostnameVerificationResovleHostName = hostnameVerificationResovleHostName;
        }


        @Override
        public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (cause instanceof DecoderException && cause != null) {
                cause = cause.getCause();
            }

            logger.error("Exception during establishing a SSL connection: " + cause, cause);

            super.exceptionCaught(ctx, cause);
        }

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            SSLEngine engine = null;
            try {
                if (hostnameVerificationEnabled) {
                    final InetSocketAddress inetSocketAddress = (InetSocketAddress) remoteAddress;
                    String hostname = null;
                    if (hostnameVerificationResovleHostName) {
                        hostname = inetSocketAddress.getHostName();
                    } else {
                        hostname = inetSocketAddress.getHostString();
                    }

                    if(log.isDebugEnabled()) {
                        log.debug("Hostname of peer is {} ({}/{}) with hostnameVerificationResovleHostName: {}", hostname, inetSocketAddress.getHostName(), inetSocketAddress.getHostString(), hostnameVerificationResovleHostName);
                    }

                    engine = sks.createClientTransportSSLEngine(hostname, inetSocketAddress.getPort());
                } else {
                    engine = sks.createClientTransportSSLEngine(null, -1);
                }
            } catch (final SSLException e) {
                throw ExceptionsHelper.convertToOpenSearchException(e);
            }
            final SslHandler sslHandler = new SslHandler(engine);
            ctx.pipeline().replace(this, "ssl_client", sslHandler);
            super.connect(ctx, remoteAddress, localAddress, promise);
        }
    }

    protected class SSLClientChannelInitializer extends Netty4Transport.ClientChannelInitializer {
        private final boolean hostnameVerificationEnabled;
        private final boolean hostnameVerificationResovleHostName;
        private final DiscoveryNode node;
        private SSLConnectionTestResult connectionTestResult;

        @SuppressWarnings("removal")
        public SSLClientChannelInitializer(DiscoveryNode node) {
            this.node = node;
            hostnameVerificationEnabled = false;
            // settings.getAsBoolean(SSLConfigConstants.SECURITY_SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION, true);
            hostnameVerificationResovleHostName = false;
            // settings.getAsBoolean(SSLConfigConstants.SECURITY_SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION_RESOLVE_HOST_NAME, true);

            connectionTestResult = SSLConnectionTestResult.SSL_AVAILABLE;
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            super.initChannel(ch);

            if(connectionTestResult == SSLConnectionTestResult.OPENSEARCH_PING_FAILED) {
                logger.error("SSL dual mode is enabled but dual mode handshake and OpenSearch ping has failed during client connection setup, closing channel");
                ch.close();
                return;
            }

            if (connectionTestResult == SSLConnectionTestResult.SSL_AVAILABLE) {
                logger.debug("Connection to {} needs to be ssl, adding ssl handler to the client channel ", node.getHostName());
                ch.pipeline().addFirst("client_ssl_handler", new ClientSSLHandler(ossks, hostnameVerificationEnabled,
                        hostnameVerificationResovleHostName));
            } else {
                logger.debug("Connection to {} needs to be non ssl", node.getHostName());
            }
        }

        @Override
        public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (cause instanceof DecoderException && cause != null) {
                cause = cause.getCause();
            }


            logger.error("Exception during establishing a SSL connection: " + cause, cause);

            super.exceptionCaught(ctx, cause);
        }
    }
}

