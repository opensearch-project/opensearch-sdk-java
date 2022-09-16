/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.lucene.util.SetOnce;
import org.opensearch.client.Client;
import org.opensearch.client.node.NodeClient;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.NamedWriteableRegistry.Entry;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.Setting.Property;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.env.NodeEnvironment;
import org.opensearch.node.Node;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.script.ScriptContext;
import org.opensearch.script.ScriptEngine;
import org.opensearch.script.ScriptService;
import org.opensearch.threadpool.ExecutorBuilder;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.watcher.ResourceWatcherService;

public abstract class BaseExtension implements Extension {
    private Client client;
    private ClusterService clusterService;
    private ThreadPool threadPool;
    private ResourceWatcherService resourceWatcherService;
    private ScriptService scriptService;
    private NamedXContentRegistry xContentRegistry;
    private Environment environment;
    private NodeEnvironment nodeEnvironment;
    private NamedWriteableRegistry namedWriteableRegistry;
    private IndexNameExpressionResolver indexNameExpressionResolver;
    private Supplier<RepositoriesService> repositoriesServiceSupplier;

    /**
     * 
     */
    public BaseExtension() throws IOException {
        createComponents();
    }

    private void createComponents() throws IOException {
        //Settings.EMPTY will eventually be replaced with a getSettings method from the Extension interface
        Settings settings = Settings.builder().put("node.name", Property.NodeScope).build();
        this.threadPool = new ThreadPool(settings, new AtomicReference<>(), Collections.emptyList().toArray(new ExecutorBuilder[0]));
        this.client = new NodeClient(settings, threadPool);
        this.clusterService = new ClusterService(settings, new ClusterSettings(settings, new HashSet<Setting<?>>()), threadPool);
        this.resourceWatcherService = new ResourceWatcherService(settings, threadPool);
        this.scriptService = new ScriptService(settings, new HashMap<String, ScriptEngine>(), new HashMap<String, ScriptContext<?>>());
        //TODO replace NamedXContentRegistry.EMPTY with the actual xContentRegistry from OpenSearch.
        //This should not be used if {@link XContentParser#namedObject(Class, String, Object)} is being called, as calling the method will cause a failure.
        this.xContentRegistry = new NamedXContentRegistry(new ArrayList<NamedXContentRegistry.Entry>());
        this.environment = new Environment(settings, Paths.get(""));
        this.nodeEnvironment = new NodeEnvironment(settings, environment);
        this.namedWriteableRegistry = new NamedWriteableRegistryAPI().getRegistry();
        this.indexNameExpressionResolver = new IndexNameExpressionResolver(threadPool.getThreadContext());
        final SetOnce<RepositoriesService> repositoriesServiceReference = new SetOnce<>();
        this.repositoriesServiceSupplier = repositoriesServiceReference::get;
    }

    public Client getClient() {
        return client;
    }

    protected void setClient(Client client) {
        this.client = client;
    }

    public ClusterService getClusterService() {
        return clusterService;
    }

    protected void setClusterService(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    protected void setThreadPool(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    public ResourceWatcherService getResourceWatcherService() {
        return resourceWatcherService;
    }

    protected void setResourceWatcherService(ResourceWatcherService resourceWatcherService) {
        this.resourceWatcherService = resourceWatcherService;
    }

    public ScriptService getScriptService() {
        return scriptService;
    }

    protected void setScriptService(ScriptService scriptService) {
        this.scriptService = scriptService;
    }

    public NamedXContentRegistry getNamedXContentRegistry() {
        return xContentRegistry;
    }

    protected void setNamedXContentRegistry(NamedXContentRegistry xContentRegistry) {
        this.xContentRegistry = xContentRegistry;
    }

    public Environment getEnvironment() {
        return environment;
    }

    protected void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public NodeEnvironment getNodeEnvironment() {
        return nodeEnvironment;
    }

    protected void setNodeEnvironment(NodeEnvironment nodeEnvironment) {
        this.nodeEnvironment = nodeEnvironment;
    }

    public NamedWriteableRegistry getNamedWriteableRegistry() {
        return namedWriteableRegistry;
    }

    protected void setNamedWriteableRegistry(NamedWriteableRegistry namedWriteableRegistry) {
        this.namedWriteableRegistry = namedWriteableRegistry;
    }

    public IndexNameExpressionResolver getIndexNameExpressionResolver() {
        return indexNameExpressionResolver;
    }

    protected void setIndexNameExpressionResolver(IndexNameExpressionResolver indexNameExpressionResolver) {
        this.indexNameExpressionResolver = indexNameExpressionResolver;
    }

    public Supplier<RepositoriesService> getRepositoriesServiceSupplier() {
        return repositoriesServiceSupplier;
    }

    protected void setRepositoriesServiceSupplier(Supplier<RepositoriesService> repositoriesServiceSupplier) {
        this.repositoriesServiceSupplier = repositoriesServiceSupplier;
    }
}
