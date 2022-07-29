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
<<<<<<< HEAD
=======
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
>>>>>>> 1c06e87 (Integrated SDK Client to make request to OpenSearch)
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
<<<<<<< HEAD
=======
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
>>>>>>> 1c06e87 (Integrated SDK Client to make request to OpenSearch)
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;

/**
 * This class creates a Client for SDK to make requests to OpenSearch
 */
public class SDKClient {
    private final Logger logger = LogManager.getLogger(SDKClient.class);
    private OpenSearchClient client;
<<<<<<< HEAD
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
=======

    /**
     * Creates client for SDK
     * @throws IOException if client failed
     */
    public void createClient() throws IOException {
        String endpoint = "localhost";
        String username = "admin";
        String password = "admin";
        String protocol = "http";
        int port = 9200;
        RestClient restClient = null;
        try {
            RestClientBuilder builder = RestClient.builder(new HttpHost(endpoint, port, protocol));
            builder.setStrictDeprecationMode(true);
            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
                try {
                    return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                            .setSSLContext(SSLContextBuilder.create().loadTrustMaterial(null, (chains, authType) -> true).build());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            restClient = builder.build();

            // Create Client
            OpenSearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            OpenSearchClient client = new OpenSearchClient(transport);
        } finally {
            if (restClient != null) {
                restClient.close();
            }
        }
    }

    /**
     * Creates index on OpenSearch
     * @throws IOException if request failed
     */
    public void createIndex(String index) throws IOException {
            logger.info("Creating Index on OpenSearch");
            // Create Index
            CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder().index(index).build();
            CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest);
            logger.info("Created Index on OpenSearch", createIndexResponse);
>>>>>>> 1c06e87 (Integrated SDK Client to make request to OpenSearch)
    }
}
