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
import org.opensearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.MultiGetRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.MultiSearchRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.client.Cancellable;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseListener;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetFieldMappingsRequest;
import org.opensearch.client.indices.GetMappingsRequest;
import org.opensearch.client.indices.PutMappingRequest;
import org.opensearch.client.indices.rollover.RolloverRequest;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterAsyncClient;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterClient;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesAsyncClient;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.sdk.SDKClient.SDKClusterAdminClient;
import org.opensearch.sdk.SDKClient.SDKIndicesClient;
import org.opensearch.sdk.SDKClient.SDKRestClient;
import org.opensearch.test.OpenSearchTestCase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.ConnectException;

@SuppressWarnings("deprecation")
public class TestSDKClient extends OpenSearchTestCase {
    private SDKClient sdkClient;
    private final ExtensionSettings extensionSettings = new ExtensionSettings("", "", "", "localhost", "9200");

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.sdkClient = new SDKClient(extensionSettings);
    }

    @Test
    public void testCreateJavaClient() throws Exception {
        OpenSearchClient javaClient = sdkClient.initializeJavaClient();
        assertInstanceOf(OpenSearchIndicesClient.class, javaClient.indices());
        assertInstanceOf(OpenSearchClusterClient.class, javaClient.cluster());

        sdkClient.doCloseJavaClients();
    }

    @Test
    public void testCreateJavaAsyncClient() throws Exception {
        OpenSearchAsyncClient javaAsyncClient = sdkClient.initializeJavaAsyncClient();
        assertInstanceOf(OpenSearchIndicesAsyncClient.class, javaAsyncClient.indices());
        assertInstanceOf(OpenSearchClusterAsyncClient.class, javaAsyncClient.cluster());

        sdkClient.doCloseJavaClients();
    }

    @Test
    public void testCreateRestClient() throws Exception {
        SDKRestClient restClient = sdkClient.initializeRestClient();
        assertInstanceOf(SDKIndicesClient.class, restClient.indices());
        assertInstanceOf(SDKClusterAdminClient.class, restClient.cluster());
        assertEquals(restClient, restClient.admin());

        sdkClient.doCloseHighLevelClient();
    }

    @Test
    public void testSDKRestClient() throws Exception {
        SDKRestClient restClient = sdkClient.initializeRestClient();

        // Would really prefer to mock/verify the method calls but they are final
        assertDoesNotThrow(() -> restClient.index(new IndexRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertDoesNotThrow(() -> restClient.get(new GetRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertDoesNotThrow(() -> restClient.multiGet(new MultiGetRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertDoesNotThrow(() -> restClient.update(new UpdateRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertDoesNotThrow(() -> restClient.delete(new DeleteRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertDoesNotThrow(() -> restClient.search(new SearchRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertDoesNotThrow(() -> restClient.multiSearch(new MultiSearchRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertDoesNotThrow(() -> restClient.bulk(new BulkRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertDoesNotThrow(() -> restClient.performRequestAsync(new Request("GET", "/"), new ResponseListener() {

            @Override
            public void onSuccess(Response response) {}

            @Override
            public void onFailure(Exception exception) {}

        }));
        expectThrows(ConnectException.class, () -> restClient.performRequest(new Request("GET", "/")));

        sdkClient.doCloseHighLevelClient();
    }

    @Test
    public void testSDKIndicesClient() throws Exception {
        SDKRestClient restClient = sdkClient.initializeRestClient();
        SDKIndicesClient indicesClient = restClient.indices();

        // Would really prefer to mock/verify the method calls but the IndicesClient class is final
        assertInstanceOf(Cancellable.class, indicesClient.create(new CreateIndexRequest(""), ActionListener.wrap(r -> {}, e -> {})));
        assertInstanceOf(Cancellable.class, indicesClient.delete(new DeleteIndexRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertInstanceOf(Cancellable.class, indicesClient.putSettings(new UpdateSettingsRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertInstanceOf(Cancellable.class, indicesClient.getMapping(new GetMappingsRequest(), ActionListener.wrap(r -> {}, e -> {})));
        assertInstanceOf(
            Cancellable.class,
            indicesClient.getFieldMapping(new GetFieldMappingsRequest(), ActionListener.wrap(r -> {}, e -> {}))
        );
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
