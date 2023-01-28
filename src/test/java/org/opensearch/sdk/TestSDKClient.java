/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.junit.jupiter.api.Test;
import org.opensearch.action.ActionListener;
import org.opensearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.client.Cancellable;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetMappingsRequest;
import org.opensearch.client.indices.PutMappingRequest;
import org.opensearch.client.indices.rollover.RolloverRequest;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterClient;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.sdk.SDKClient.SDKClusterAdminClient;
import org.opensearch.sdk.SDKClient.SDKIndicesClient;
import org.opensearch.sdk.SDKClient.SDKRestClient;
import org.opensearch.test.OpenSearchTestCase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

@SuppressWarnings("deprecation")
public class TestSDKClient extends OpenSearchTestCase {
    private SDKClient sdkClient;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.sdkClient = new SDKClient();
    }

    @Test
    public void testCreateJavaClient() throws Exception {
        OpenSearchClient javaClient = sdkClient.initializeJavaClient("localhost", 9200);
        assertInstanceOf(OpenSearchIndicesClient.class, javaClient.indices());
        assertInstanceOf(OpenSearchClusterClient.class, javaClient.cluster());

        sdkClient.doCloseJavaClient();
    }

    @Test
    public void testCreateRestClient() throws Exception {
        SDKRestClient restClient = sdkClient.initializeRestClient("localhost", 9200);
        assertInstanceOf(SDKIndicesClient.class, restClient.indices());
        assertInstanceOf(SDKClusterAdminClient.class, restClient.cluster());

        sdkClient.doCloseHighLevelClient();
    }

    @Test
    public void testSDKRestClient() throws Exception {
        SDKRestClient restClient = sdkClient.initializeRestClient("localhost", 9200);

        // Would really prefer to mock/verify the method calls but they are final
        assertDoesNotThrow(() -> restClient.index(new IndexRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertDoesNotThrow(() -> restClient.get(new GetRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertDoesNotThrow(() -> restClient.delete(new DeleteRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertDoesNotThrow(() -> restClient.search(new SearchRequest(), ActionListener.wrap(r -> {}, e -> {})));

        sdkClient.doCloseHighLevelClient();
    }

    @Test
    public void testSDKIndicesClient() throws Exception {
        SDKRestClient restClient = sdkClient.initializeRestClient("localhost", 9200);
        SDKIndicesClient indicesClient = restClient.indices();

        // Would really prefer to mock/verify the method calls but the IndicesClient class is final
        assertInstanceOf(Cancellable.class, indicesClient.create(new CreateIndexRequest(""), ActionListener.wrap(r -> {}, e -> {})));
        assertInstanceOf(Cancellable.class, indicesClient.delete(new DeleteIndexRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertInstanceOf(Cancellable.class, indicesClient.getMapping(new GetMappingsRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertInstanceOf(Cancellable.class, indicesClient.putMapping(new PutMappingRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertInstanceOf(
            Cancellable.class,
            indicesClient.rolloverIndex(new RolloverRequest("", ""), ActionListener.wrap(r -> {}, e -> {}))
        );
        assertInstanceOf(Cancellable.class, indicesClient.getAliases(new GetAliasesRequest(), ActionListener.wrap(r -> {}, e -> {})));

        sdkClient.doCloseHighLevelClient();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        this.sdkClient.close();
    }

}
