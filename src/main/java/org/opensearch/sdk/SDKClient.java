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
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;

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
     * @throws IOException if client failed
     * @return SDKClient which is internally an OpenSearchClient. The user is responsible for calling {@link #doCloseRestClient()} when finished with the client
     */
    public OpenSearchClient initializeClient(String hostAddress, int port) throws IOException {
        RestClientBuilder builder = RestClient.builder(new HttpHost(hostAddress, port));
        builder.setStrictDeprecationMode(true);
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            try {
                return httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        restClient = builder.build();

        // Create Client
        OpenSearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
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
