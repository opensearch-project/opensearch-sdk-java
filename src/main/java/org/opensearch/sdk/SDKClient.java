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
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.ActionType;
import org.opensearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.get.MultiGetRequest;
import org.opensearch.action.get.MultiGetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.MultiSearchRequest;
import org.opensearch.action.search.MultiSearchResponse;
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
import org.opensearch.client.RestClient;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseListener;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.client.indices.GetFieldMappingsRequest;
import org.opensearch.client.indices.GetFieldMappingsResponse;
import org.opensearch.client.indices.GetMappingsRequest;
import org.opensearch.client.indices.GetMappingsResponse;
import org.opensearch.client.indices.PutMappingRequest;
import org.opensearch.client.indices.rollover.RolloverRequest;
import org.opensearch.client.indices.rollover.RolloverResponse;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.opensearch.index.reindex.BulkByScrollResponse;
import org.opensearch.index.reindex.DeleteByQueryRequest;

import javax.net.ssl.SSLEngine;

/**
 * This class creates SDKClient for an extension to make requests to OpenSearch
 */
public class SDKClient implements Closeable {
    private OpenSearchClient javaClient;
    private RestClient restClient;
    private RestHighLevelClient sdkRestClient;
    private OpenSearchAsyncClient javaAsyncClient;
    private final ExtensionSettings extensionSettings;

    /**
    * Instantiates this class with a copy of the extension settings.
    *
    * @param extensionSettings The Extension settings
    */
    public SDKClient(ExtensionSettings extensionSettings) {
        this.extensionSettings = extensionSettings;
    }

    // Used by client.execute, populated by initialize method
    @SuppressWarnings("rawtypes")
    private Map<ActionType, TransportAction> actions = Collections.emptyMap();
    // Used by remote client execution where we get a string for the class name
    @SuppressWarnings("rawtypes")
    private Map<String, ActionType> actionClassToInstanceMap = Collections.emptyMap();

    /**
     * Initialize this client.
     *
     * @param actions The injected map of ActionType instances to TransportAction.
     */
    @SuppressWarnings("rawtypes")
    public void initialize(Map<ActionType, TransportAction> actions) {
        this.actions = actions;
        this.actionClassToInstanceMap = actions.keySet().stream().collect(Collectors.toMap(a -> a.getClass().getName(), a -> a));
    }

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
                final TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                    .setSslContext(SSLContextBuilder.create().loadTrustMaterial(null, (chains, authType) -> true).build())
                    // disable the certificate since our cluster currently just uses the default security
                    // configuration
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
     * Initializes an OpenSearchTransport using RestClientTransport. This is required for JavaClient and JavaAsyncClient
     *
     * @param hostAddress The address of OpenSearch cluster, client can connect to
     * @param port The port of OpenSearch cluster
     * @return The OpenSearchTransport implementation of RestClientTransport.
     */
    private OpenSearchTransport initializeTransport(String hostAddress, int port) {
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
        return transport;
    }

    /**
     * Initializes an OpenSearchClient using OpenSearch JavaClient
     *
     * @return The SDKClient implementation of OpenSearchClient. The user is responsible for calling
     *         {@link #doCloseJavaClients()} when finished with the client
     */
    public OpenSearchClient initializeJavaClient() {
        return initializeJavaClient(extensionSettings.getOpensearchAddress(), Integer.parseInt(extensionSettings.getOpensearchPort()));
    }

    /**
     * Initializes an OpenSearchClient using OpenSearch JavaClient
     *
     * @param hostAddress The address of OpenSearch cluster, client can connect to
     * @param port The port of OpenSearch cluster
     * @return The SDKClient implementation of OpenSearchClient. The user is responsible for calling
     *         {@link #doCloseJavaClients()} when finished with the client
     */
    public OpenSearchClient initializeJavaClient(String hostAddress, int port) {
        OpenSearchTransport transport = initializeTransport(hostAddress, port);
        javaClient = new OpenSearchClient(transport);
        return javaClient;
    }

    /**
     * Initializes an OpenAsyncSearchClient using OpenSearch JavaClient
     *
     * @return The SDKClient implementation of OpenSearchAsyncClient. The user is responsible for calling
     *         {@link #doCloseJavaClients()} when finished with the client as JavaClient and JavaAsyncClient uses the same close method
     */
    public OpenSearchAsyncClient initializeJavaAsyncClient() {
        return initalizeJavaAsyncClient(extensionSettings.getOpensearchAddress(), Integer.parseInt(extensionSettings.getOpensearchPort()));
    }

    /**
     * Initializes an OpenAsyncSearchClient using OpenSearch JavaClient
     *
     * @param hostAddress The address of OpenSearch cluster, client can connect to
     * @param port The port of OpenSearch cluster
     * @return The SDKClient implementation of OpenSearchAsyncClient. The user is responsible for calling
     *         {@link #doCloseJavaClients()} when finished with the client
     */
    public OpenSearchAsyncClient initalizeJavaAsyncClient(String hostAddress, int port) {
        OpenSearchTransport transport = initializeTransport(hostAddress, port);
        javaAsyncClient = new OpenSearchAsyncClient(transport);
        return javaAsyncClient;
    }

    /**
     * Initializes a SDK Rest Client wrapping the {@link RestHighLevelClient}.
     * <p>
     * The purpose of this client is to provide a drop-in replacement for the syntax of the {@link Client}
     * implementation in existing plugins with a minimum of code changes.
     * <p>
     * Do not use this client for new development.
     *
     * @return The SDKClient implementation of RestHighLevelClient. The user is responsible for calling
     *         {@link #doCloseHighLevelClient()} when finished with the client
     * @deprecated Provided for compatibility with existing plugins to permit migration. Use
     *             {@link #initializeJavaClient} for new development.
     */
    @Deprecated
    public SDKRestClient initializeRestClient() {
        return initializeRestClient(extensionSettings.getOpensearchAddress(), Integer.parseInt(extensionSettings.getOpensearchPort()));
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
    public void doCloseJavaClients() throws IOException {
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
        doCloseJavaClients();
        doCloseHighLevelClient();
    }

    /**
     * Gets an instance of {@link ActionType} from its corresponding class name, suitable for using as the first parameter in {@link #execute(ActionType, ActionRequest, ActionListener)}.
     *
     * @param className The class name of the action type
     * @return The instance corresponding to the class name
     */
    @SuppressWarnings("unchecked")
    public ActionType<? extends ActionResponse> getActionFromClassName(String className) {
        return actionClassToInstanceMap.get(className);
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
        if (actions == null) {
            throw new IllegalStateException("SDKClient was not initialized because the Extension does not implement ActionExtension.");
        }
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
        private RequestOptions options = RequestOptions.DEFAULT;

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

        public void setOptions(RequestOptions options) {
            this.options = options;
        }

        /**
         * The admin client that can be used to perform administrative operations.
         *
         * @return An instance of this client. Method provided for backwards compatibility.
         */
        public SDKRestClient admin() {
            return this;
        }

        /**
         * A client allowing to perform actions/operations against the cluster.
         *
         * @return An instance of a cluster admin client.
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
         *
         * @return An instance of an indices client.
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
            restHighLevelClient.indexAsync(request, options, listener);
        }

        /**
         * Gets the document that was indexed from an index with an id.
         *
         * @param request  The get request
         * @param listener A listener to be notified with a result
         * @see Requests#getRequest(String)
         */
        public void get(GetRequest request, ActionListener<GetResponse> listener) {
            restHighLevelClient.getAsync(request, options, listener);
        }

        /**
         * Gets all the documents that match the criteria
         *
         * @param request The multiGet Request
         * @param listener A listener to be notified with a result
         */
        public void multiGet(MultiGetRequest request, ActionListener<MultiGetResponse> listener) {
            restHighLevelClient.mgetAsync(request, options, listener);
        }

        /**
         * Updates a document based on a script.
         *
         * @param request  The update request
         * @param listener A listener to be notified with a result
         */
        public void update(UpdateRequest request, ActionListener<UpdateResponse> listener) {
            restHighLevelClient.updateAsync(request, options, listener);
        }

        /**
         * Deletes a document from the index based on the index, and id.
         *
         * @param request The delete request
         * @param listener A listener to be notified with a result
         * @see Requests#deleteRequest(String)
         */
        public void delete(DeleteRequest request, ActionListener<DeleteResponse> listener) {
            restHighLevelClient.deleteAsync(request, options, listener);
        }

        /**
         * Deletes a document from the index based on the query.
         *
         * @param request The delete by query request
         * @param listener A listener to be notified with a result
         *
         */
        public void deleteByQuery(DeleteByQueryRequest request, ActionListener<BulkByScrollResponse> listener) {
            restHighLevelClient.deleteByQueryAsync(request, options, listener);
        }

        /**
         * Search across one or more indices with a query.
         *
         * @param request The search request
         * @param listener A listener to be notified of the result
         * @see Requests#searchRequest(String...)
         */
        public void search(SearchRequest request, ActionListener<SearchResponse> listener) {
            restHighLevelClient.searchAsync(request, options, listener);
        }

        /**
         * Search across all documents that match the criteria
         *
         * @param request The multiSearch Request
         * @param listener A listener to be notified with a result
         */
        public void multiSearch(MultiSearchRequest request, ActionListener<MultiSearchResponse> listener) {
            restHighLevelClient.msearchAsync(request, options, listener);
        }

        /**
         * Executes a bulk request using the Bulk API.
         *
         * @param request the request
         * @param listener A listener to be notified of a result
         */
        public void bulk(BulkRequest request, ActionListener<BulkResponse> listener) {
            restHighLevelClient.bulkAsync(request, options, listener);
        }

        /**
         * Sends a request to the OpenSearch cluster that the client points to.
         * @param request the request to perform
         * @return the response returned by OpenSearch
         * @throws IOException in case of a problem or the connection was aborted
         */
        public Response performRequest(Request request) throws IOException {
            return restHighLevelClient.getLowLevelClient().performRequest(request);
        }

        /**
         * Sends a request to the OpenSearch cluster that the client points to.
         * The request is executed asynchronously and the provided
         * {@link ResponseListener} gets notified upon request completion or
         * failure. Selects a host out of the provided ones in a round-robin
         * fashion. Failing hosts are marked dead and retried after a certain
         * amount of time (minimum 1 minute, maximum 30 minutes), depending on how
         * many times they previously failed (the more failures, the later they
         * will be retried). In case of failures all of the alive nodes (or dead
         * nodes that deserve a retry) are retried until one responds or none of
         * them does, in which case an {@link IOException} will be thrown.
         *
         * @param request the request to perform
         * @param responseListener the {@link ResponseListener} to notify when the
         *      request is completed or fails
         * @return Cancellable instance that may be used to cancel the request
         */
        public Cancellable performRequestAsync(Request request, ResponseListener responseListener) {
            return restHighLevelClient.getLowLevelClient().performRequestAsync(request, responseListener);
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
        private RequestOptions options = RequestOptions.DEFAULT;

        public void setOptions(RequestOptions options) {
            this.options = options;
        }

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
            return indicesClient.createAsync(createIndexRequest, options, listener);
        }

        /**
         * Asynchronously deletes an index using the Delete Index API.
         *
         * @param deleteIndexRequest the request
         * @param listener the listener to be notified upon request completion
         * @return cancellable that may be used to cancel the request
         */
        public Cancellable delete(DeleteIndexRequest deleteIndexRequest, ActionListener<AcknowledgedResponse> listener) {
            return indicesClient.deleteAsync(deleteIndexRequest, options, listener);
        }

        /**
         * Asynchronously updates specific index level settings using the Update Indices Settings API.
         *
         * @param updateSettingsRequest the request
         * @param listener the listener to be notified upon request completion
         * @return cancellable that may be used to cancel the request
         */
        public Cancellable putSettings(UpdateSettingsRequest updateSettingsRequest, ActionListener<AcknowledgedResponse> listener) {
            return indicesClient.putSettingsAsync(updateSettingsRequest, options, listener);
        }

        /**
         * Asynchronously updates the mappings on an index using the Put Mapping API.
         *
         * @param putMappingRequest the request
         * @param listener the listener to be notified upon request completion
         * @return cancellable that may be used to cancel the request
         */
        public Cancellable putMapping(PutMappingRequest putMappingRequest, ActionListener<AcknowledgedResponse> listener) {
            return this.indicesClient.putMappingAsync(putMappingRequest, options, listener);
        }

        /**
         * Asynchronously retrieves the mappings on an index on indices using the Get Mapping API.
         *
         * @param getMappingsRequest the request
         * @param listener the listener to be notified upon request completion
         * @return cancellable that may be used to cancel the request
         */
        public Cancellable getMapping(GetMappingsRequest getMappingsRequest, ActionListener<GetMappingsResponse> listener) {
            return this.indicesClient.getMappingAsync(getMappingsRequest, options, listener);
        }

        /**
         * Asynchronously retrieves the field mappings on an index or indices using the Get Field Mapping API.
         *
         * @param getFieldMappingsRequest the request
         * @param listener the listener to be notified upon request completion
         * @return cancellable that may be used to cancel the request
         */
        public Cancellable getFieldMapping(
            GetFieldMappingsRequest getFieldMappingsRequest,
            ActionListener<GetFieldMappingsResponse> listener
        ) {
            return this.indicesClient.getFieldMappingAsync(getFieldMappingsRequest, options, listener);
        }

        /**
         * Asynchronously rolls over an index using the Rollover Index API.
         *
         * @param rolloverRequest the request
         * @param listener the listener to be notified upon request completion
         * @return cancellable that may be used to cancel the request
         */
        public Cancellable rolloverIndex(RolloverRequest rolloverRequest, ActionListener<RolloverResponse> listener) {
            return this.indicesClient.rolloverAsync(rolloverRequest, options, listener);
        }

        /**
         * Asynchronously gets one or more aliases using the Get Index Aliases API.
         *
         * @param getAliasesRequest the request
         * @param listener the listener to be notified upon request completion
         * @return cancellable that may be used to cancel the request
         */
        public Cancellable getAliases(GetAliasesRequest getAliasesRequest, ActionListener<GetAliasesResponse> listener) {
            return this.indicesClient.getAliasAsync(getAliasesRequest, options, listener);
        }
    }
}
