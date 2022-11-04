/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;

import javax.net.ssl.SSLEngine;

/**
 * This class creates SDKClient for an extension to make requests to OpenSearch
 */
public class SDKClient {
    private OpenSearchClient javaClient;
    private RestClient restClient = null;

    /**
     * Creates OpenSearchClient for SDK. It also creates a restClient as a wrapper around Java OpenSearchClient
     * @param hostAddress The address of OpenSearch cluster, client can connect to
     * @param port The port of OpenSearch cluster
     * @return SDKClient which is internally an OpenSearchClient. The user is responsible for calling {@link #doCloseRestClient()} when finished with the client
     */
    public OpenSearchClient initializeClient(String hostAddress, int port) {
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

        restClient = builder.build();

        // Create Client
        OpenSearchTransport transport = new RestClientTransport(
            restClient,
            new JacksonJsonpMapper(new ObjectMapper().registerModule(new JavaTimeModule()))
        );
        javaClient = new OpenSearchClient(transport);
        return javaClient;
    }

    /**
     * Close this client.
     *
     * @throws IOException if closing the restClient fails
     */
    public void doCloseRestClient() throws IOException {
        if (restClient != null) {
            restClient.close();
        }
    }
}
