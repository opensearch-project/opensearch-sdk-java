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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Inject;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.ActionType;
import org.opensearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.get.MultiGetRequest;
import org.opensearch.action.get.MultiGetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.support.TransportAction;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.action.update.UpdateResponse;
import org.opensearch.client.Cancellable;
import org.opensearch.client.Client;
import org.opensearch.client.ClusterAdminClient;
import org.opensearch.client.ClusterClient;
import org.opensearch.client.GetAliasesResponse;
import org.opensearch.client.IndicesClient;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Requests;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.client.indices.GetMappingsRequest;
import org.opensearch.client.indices.GetMappingsResponse;
import org.opensearch.client.indices.PutMappingRequest;
import org.opensearch.client.indices.rollover.RolloverRequest;
import org.opensearch.client.indices.rollover.RolloverResponse;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;

/**
 * This class creates SDKClient for an extension to make requests to OpenSearch
 */
public class SDKClient implements Closeable {
    private OpenSearchClient javaClient;
    private RestClient restClient;
    private RestHighLevelClient sdkRestClient;

    // Used by client.execute
    @SuppressWarnings("rawtypes")
    @Inject
    private Map<ActionType, TransportAction> actions;

    /**
     * Instantiate this client.
     */
    public SDKClient() {}

    /**
     * Create and configure a RestClientBuilder
     *
     * @param hostAddress The address the client should connect to
     * @param port The port the client should connect to
     * @return An instance of the builder
     */
    private static RestClientBuilder builder(String hostAddress, int port) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(hostAddress, port));
        builder.setStrictDeprecationMode(true);
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            try {
                return httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return builder;
    }

    /**
     * Initializes an OpenSearchClient using OpenSearch JavaClient
     *
     * @param settings The Extension settings
     * @return The SDKClient implementation of OpenSearchClient. The user is responsible for calling
     *         {@link #doCloseJavaClient()} when finished with the client
     */
    public OpenSearchClient initializeJavaClient(ExtensionSettings settings) {
        return initializeJavaClient(settings.getOpensearchAddress(), Integer.parseInt(settings.getOpensearchPort()));
    }

    /**
     * Initializes an OpenSearchClient using OpenSearch JavaClient
     *
     * @param hostAddress The address of OpenSearch cluster, client can connect to
     * @param port The port of OpenSearch cluster
     * @return The SDKClient implementation of OpenSearchClient. The user is responsible for calling
     *         {@link #doCloseJavaClient()} when finished with the client
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
     * Initializes a SDK Rest Client wrapping the {@link RestHighLevelClient}.
     * <p>
     * The purpose of this client is to provide a drop-in replacement for the syntax of the {@link Client}
     * implementation in existing plugins with a minimum of code changes.
     * <p>
     * Do not use this client for new development.
     *
     * @param settings The Extension settings
     * @return The SDKClient implementation of RestHighLevelClient. The user is responsible for calling
     *         {@link #doCloseHighLevelClient()} when finished with the client
     * @deprecated Provided for compatibility with existing plugins to permit migration. Use
     *             {@link #initializeJavaClient} for new development.
     */
    @Deprecated
    public SDKRestClient initializeRestClient(ExtensionSettings settings) {
        return initializeRestClient(settings.getOpensearchAddress(), Integer.parseInt(settings.getOpensearchPort()));
    }

    /**
     * Initializes a SDK Rest Client wrapping the {@link RestHighLevelClient}.
     * <p>
     * The purpose of this client is to provide a drop-in replacement for the syntax of the {@link Client}
     * implementation in existing plugins with a minimum of code changes.
     * <p>
     * Do not use this client for new development.
     *
     * @param hostAddress The address of OpenSearch cluster, client can connect to
     * @param port        The port of OpenSearch cluster
     * @return The SDKClient implementation of RestHighLevelClient. The user is responsible for calling
     *         {@link #doCloseHighLevelClient()} when finished with the client
     * @deprecated Provided for compatibility with existing plugins to permit migration. Use
     *             {@link #initializeJavaClient} for new development.
     */
    @Deprecated
    public SDKRestClient initializeRestClient(String hostAddress, int port) {
        return new SDKRestClient(this, new RestHighLevelClient(builder(hostAddress, port)));
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
        if (sdkRestClient != null) {
            sdkRestClient.close();
        }
    }

    @Override
    public void close() throws IOException {
        doCloseJavaClient();
        doCloseHighLevelClient();
    }

    /**
     * Executes a generic action, denoted by an {@link ActionType}.
     *
     * @param action The action type to execute.
     * @param request The action request.
     * @param listener The listener to receive the response back.
     * @param <Request> The request type.
     * @param <Response> The response type.
     */
    public final <Request extends ActionRequest, Response extends ActionResponse> void execute(
        ActionType<Response> action,
        Request request,
        ActionListener<Response> listener
    ) {
        @SuppressWarnings("unchecked")
        TransportAction<Request, Response> transportAction = actions.get(action);
        if (transportAction == null) {
            throw new IllegalStateException("failed to find action [" + action + "] to execute");
        }
        transportAction.execute(request, listener);
    }

    /**
     * Wraps an internal {@link RestHighLevelClient} using method signatures expected by {@link Client} and {@link org.opensearch.client.AdminClient} syntax, providing a drop-in replacement in existing plugins with a minimum of code changes.
     * <p>
     * While some {@link Client} interface methods are implemented here, the interface is intentionally not fully implemented as it is intended to be deprecated.
     * <p>
     * Do not use this client for new development.
     *
     * @deprecated Use {@link org.opensearch.client.opensearch.OpenSearchClient}.
     * @see <a href="https://github.com/opensearch-project/OpenSearch/issues/5424">OpenSearch Issue 5424</a>
     */
    @Deprecated
    public static class SDKRestClient implements Closeable {

        private final SDKClient sdkClient;
        private final RestHighLevelClient restHighLevelClient;

        /**
         * Instantiate this class wrapping a {@link RestHighLevelClient}.
         *
         * @param sdkClient The SDKClient instance.
         * @param restHighLevelClient The client to wrap.
         */
        public SDKRestClient(SDKClient sdkClient, RestHighLevelClient restHighLevelClient) {
            this.sdkClient = sdkClient;
            this.restHighLevelClient = restHighLevelClient;
        }

        /**
         * The admin client that can be used to perform administrative operations.
         */
        public SDKRestClient admin() {
            return this;
        }

        /**
         * A client allowing to perform actions/operations against the cluster.
         */
        public SDKClusterAdminClient cluster() {
            return new SDKClusterAdminClient(restHighLevelClient.cluster());
        }

        /**
         * Executes a generic action, denoted by an {@link ActionType}.
         *
         * @param action The action type to execute.
         * @param request The action request.
         * @param listener The listener to receive the response back.
         * @param <Request> The request type.
         * @param <Response> The response type.
         */
        public final <Request extends ActionRequest, Response extends ActionResponse> void execute(
            ActionType<Response> action,
            Request request,
            ActionListener<Response> listener
        ) {
            this.sdkClient.execute(action, request, listener);
        }

        /**
         * A client allowing to perform actions/operations against the indices.
         */
        public SDKIndicesClient indices() {
            return new SDKIndicesClient(restHighLevelClient.indices());
        }

        /**
         * Index a document associated with a given index.
         * <p>
         * The id is optional, if it is not provided, one will be generated automatically.
         *
         * @param request  The index request
         * @param listener A listener to be notified with a result
         * @see Requests#indexRequest(String)
         */
        public void index(IndexRequest request, ActionListener<IndexResponse> listener) {
            restHighLevelClient.indexAsync(request, RequestOptions.DEFAULT, listener);
        }

        /**
         * Gets the document that was indexed from an index with an id.
         *
         * @param request  The get request
         * @param listener A listener to be notified with a result
         * @see Requests#getRequest(String)
         */
        public void get(GetRequest request, ActionListener<GetResponse> listener) {
            restHighLevelClient.getAsync(request, RequestOptions.DEFAULT, listener);
        }

        /**
         * Gets all the documents that match the criteria
         *
         * @param request The multiGet Request
         * @param listener A listener to be notified with a result
         */
        public void multiGet(MultiGetRequest request, ActionListener<MultiGetResponse> listener) {
            restHighLevelClient.mgetAsync(request, RequestOptions.DEFAULT, listener);
        }

        /**
         * Updates a document based on a script.
         *
         * @param request  The update request
         * @param listener A listener to be notified with a result
         */
        public void update(UpdateRequest request, ActionListener<UpdateResponse> listener) {
            restHighLevelClient.updateAsync(request, RequestOptions.DEFAULT, listener);
        }

        /**
         * Deletes a document from the index based on the index, and id.
         *
         * @param request The delete request
         * @param listener A listener to be notified with a result
         * @see Requests#deleteRequest(String)
         */
        public void delete(DeleteRequest request, ActionListener<DeleteResponse> listener) {
            restHighLevelClient.deleteAsync(request, RequestOptions.DEFAULT, listener);
        }

        /**
         * Search across one or more indices with a query.
         *
         * @param request The search request
         * @param listener A listener to be notified of the result
         * @see Requests#searchRequest(String...)
         */
        public void search(SearchRequest request, ActionListener<SearchResponse> listener) {
            restHighLevelClient.searchAsync(request, RequestOptions.DEFAULT, listener);
        }

        @Override
        public void close() throws IOException {
            restHighLevelClient.close();
        }
    }

    /**
     * Wraps an internal {@link ClusterAdminClient}, providing a drop-in replacement in existing plugins with a minimum of code changes.
     * <p>
     * Do not use this client for new development.
     */
    public static class SDKClusterAdminClient {

        private final ClusterClient clusterClient;

        /**
         * Instantiate this class using a {@link ClusterClient}.
         *
         * @param clusterClient The client to wrap
         */
        public SDKClusterAdminClient(ClusterClient clusterClient) {
            this.clusterClient = clusterClient;
        }

        // TODO: Implement state()
        // https://github.com/opensearch-project/opensearch-sdk-java/issues/354

    }

    /**
     * Wraps an internal {@link IndicesClient}, providing a drop-in replacement in existing plugins with a minimum of code changes.
     * <p>
     * Do not use this client for new development.
     */
    public static class SDKIndicesClient {

        private final IndicesClient indicesClient;

        /**
         * Instantiate this class wrapping an {@link IndicesClient}.
         *
         * @param indicesClient The client to wrap
         */
        public SDKIndicesClient(IndicesClient indicesClient) {
            this.indicesClient = indicesClient;
        }

        /**
         * Asynchronously creates an index using the Create Index API.
         *
         * @param createIndexRequest the request
         * @param listener the listener to be notified upon request completion
         * @return cancellable that may be used to cancel the request
         */
        public Cancellable create(CreateIndexRequest createIndexRequest, ActionListener<CreateIndexResponse> listener) {
            return indicesClient.createAsync(createIndexRequest, RequestOptions.DEFAULT, listener);
        }

        /**
         * Asynchronously deletes an index using the Delete Index API.
         *
         * @param deleteIndexRequest the request
         * @param listener the listener to be notified upon request completion
         * @return cancellable that may be used to cancel the request
         */
        public Cancellable delete(DeleteIndexRequest deleteIndexRequest, ActionListener<AcknowledgedResponse> listener) {
            return indicesClient.deleteAsync(deleteIndexRequest, RequestOptions.DEFAULT, listener);
        }

        /**
         * Asynchronously updates the mappings on an index using the Put Mapping API.
         *
         * @param putMappingRequest the request
         * @param listener the listener to be notified upon request completion
         * @return cancellable that may be used to cancel the request
         */
        public Cancellable putMapping(PutMappingRequest putMappingRequest, ActionListener<AcknowledgedResponse> listener) {
            return this.indicesClient.putMappingAsync(putMappingRequest, RequestOptions.DEFAULT, listener);
        }

        /**
         * Asynchronously retrieves the mappings on an index on indices using the Get Mapping API.
         *
         * @param getMappingsRequest the request
         * @param listener the listener to be notified upon request completion
         * @return cancellable that may be used to cancel the request
         */
        public Cancellable getMapping(GetMappingsRequest getMappingsRequest, ActionListener<GetMappingsResponse> listener) {
            return this.indicesClient.getMappingAsync(getMappingsRequest, RequestOptions.DEFAULT, listener);
        }

        /**
         * Asynchronously rolls over an index using the Rollover Index API.
         *
         * @param rolloverRequest the request
         * @param listener the listener to be notified upon request completion
         * @return cancellable that may be used to cancel the request
         */
        public Cancellable rolloverIndex(RolloverRequest rolloverRequest, ActionListener<RolloverResponse> listener) {
            return this.indicesClient.rolloverAsync(rolloverRequest, RequestOptions.DEFAULT, listener);
        }

        /**
         * Asynchronously gets one or more aliases using the Get Index Aliases API.
         *
         * @param getAliasesRequest the request
         * @param listener the listener to be notified upon request completion
         * @return cancellable that may be used to cancel the request
         */
        public Cancellable getAliases(GetAliasesRequest getAliasesRequest, ActionListener<GetAliasesResponse> listener) {
            return this.indicesClient.getAliasAsync(getAliasesRequest, RequestOptions.DEFAULT, listener);
        }
    }
}
