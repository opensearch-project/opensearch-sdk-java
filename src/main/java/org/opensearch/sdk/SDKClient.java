/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.io.Closeable;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.apache.hc.core5.function.Factory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.reactor.ssl.TlsDetails;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;

import javax.net.ssl.SSLEngine;

/**
 * This class creates SDKClient for an extension to make requests to OpenSearch
 */
public class SDKClient implements Closeable {
    private OpenSearchClient javaClient;
    private RestClient restClient;
    private RestHighLevelClient highLevelClient;

    private RestClientBuilder builder(String hostAddress, int port) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(hostAddress, port));
        builder.setStrictDeprecationMode(true);
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            try {
                final TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                    .setSslContext(SSLContextBuilder.create().loadTrustMaterial(null, (chains, authType) -> true).build())
                    // disable the certificate since our cluster currently just uses the default security configuration
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    // See please https://issues.apache.org/jira/browse/HTTPCLIENT-2219
                    .setTlsDetailsFactory(new Factory<SSLEngine, TlsDetails>() {
                        @Override
                        public TlsDetails create(final SSLEngine sslEngine) {
                            return new TlsDetails(sslEngine.getSession(), sslEngine.getApplicationProtocol());
                        }
                    })
                    .build();

                final PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                    .setTlsStrategy(tlsStrategy)
                    .build();
                return httpClientBuilder.setConnectionManager(connectionManager);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return builder;
    }

    /**
     * Creates OpenSearchClient for SDK. It also creates a restClient as a wrapper around Java OpenSearchClient
     * @param hostAddress The address of OpenSearch cluster, client can connect to
     * @param port The port of OpenSearch cluster
     * @return The SDKClient implementation of OpenSearchClient. The user is responsible for calling {@link #doCloseJavaClient()} when finished with the client
     */
    public OpenSearchClient initializeJavaClient(String hostAddress, int port) {
        RestClientBuilder builder = builder(hostAddress, port);

        restClient = builder.build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new GuavaModule());
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY);
        mapper.configure(MapperFeature.USE_GETTERS_AS_SETTERS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Create Client
        OpenSearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(mapper));
        javaClient = new OpenSearchClient(transport);
        return javaClient;
    }

    /**
     * @deprecated Provided for compatibility with existing plugins to permit migration. New development should not use this client
     * Creates High Level Rest Client for SDK.
     * @param hostAddress The address of OpenSearch cluster, client can connect to
     * @param port The port of OpenSearch cluster
     * @return The SDKClient implementation of RestHighLevelClient. The user is responsible for calling {@link #doCloseHighLevelClient()} when finished with the client
     */
    @Deprecated
    public RestHighLevelClient initializeRestClient(String hostAddress, int port) {
        RestClientBuilder builder = builder(hostAddress, port);

        highLevelClient = new RestHighLevelClient(builder);
        return highLevelClient;
    }

    /**
     * Close java client.
     *
     * @throws IOException if closing the restClient fails
     */
    public void doCloseJavaClient() throws IOException {
        if (restClient != null) {
            restClient.close();
        }
    }

    /**
     * Close high level rest client.
     *
     * @throws IOException if closing the highLevelClient fails
     */
    public void doCloseHighLevelClient() throws IOException {
        if (highLevelClient != null) {
            highLevelClient.close();
        }
    }

    @Override
    public void close() throws IOException {
        doCloseJavaClient();
        doCloseHighLevelClient();
    }
}
