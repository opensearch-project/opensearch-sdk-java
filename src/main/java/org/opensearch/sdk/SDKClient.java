/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import java.io.IOException;

import org.apache.http.HttpHost;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;

/**
 * This class creates a Client for SDK to make requests to OpenSearch
 */
public class SDKClient {
    private final Logger logger = LogManager.getLogger(SDKClient.class);
    private OpenSearchClient client;
    private RestClient restClient = null;

    /**
     * Creates client for SDK
     * @param hostAddress The address client can connect to
     * @param port The port of the address
     * @throws IOException if client failed
     * @return SDKClient
     */
    public OpenSearchClient createClient(String hostAddress, int port) throws IOException {
        RestClientBuilder builder = RestClient.builder(new HttpHost(hostAddress, port));
        builder.setStrictDeprecationMode(true);
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            // credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            try {
                return httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .setSSLContext(SSLContextBuilder.create().loadTrustMaterial(null, (chains, authType) -> true).build());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        restClient = builder.build();

        // Create Client
        OpenSearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        client = new OpenSearchClient(transport);
        return client;
    }

    /**
     *
     * @throws IOException if closing the client fails
     */
    public void doClose() throws IOException {
        if (restClient != null) {
            restClient.close();
        }
    }
}
