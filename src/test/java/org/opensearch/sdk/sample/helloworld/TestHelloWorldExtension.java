/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.helloworld;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.lucene.util.SetOnce;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.Client;
import org.opensearch.client.node.NodeClient;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.Setting.Property;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.env.NodeEnvironment;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.script.ScriptContext;
import org.opensearch.script.ScriptEngine;
import org.opensearch.script.ScriptService;
import org.opensearch.sdk.Extension;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.sdk.ExtensionSettings;
import org.opensearch.sdk.NamedWriteableRegistryAPI;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.ExecutorBuilder;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.watcher.ResourceWatcherService;

public class TestHelloWorldExtension extends OpenSearchTestCase {

    private Extension extension;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.extension = new HelloWorldExtension();
    }

    @Test
    public void testExtensionSettings() {
        // This effectively tests the Extension interface helper method
        ExtensionSettings extensionSettings = extension.getExtensionSettings();
        ExtensionSettings expected = new ExtensionSettings("hello-world", "127.0.0.1", "4532");
        assertEquals(expected.getExtensionName(), extensionSettings.getExtensionName());
        assertEquals(expected.getHostAddress(), extensionSettings.getHostAddress());
        assertEquals(expected.getHostPort(), extensionSettings.getHostPort());
    }

    @Test
    public void testExtensionRestHandlers() {
        List<ExtensionRestHandler> extensionRestHandlers = extension.getExtensionRestHandlers();
        assertEquals(1, extensionRestHandlers.size());
        List<Route> routes = extensionRestHandlers.get(0).routes();
        assertEquals(2, routes.size());
    }

    /**
    @Test
    public void testCreateComponents() throws IOException {
        Settings settings = Settings.builder().put("node.name", Property.NodeScope).build();
        ThreadPool threadPool = new ThreadPool(settings, new AtomicReference<>(), Collections.emptyList().toArray(new ExecutorBuilder[0]));
        Client client = new NodeClient(settings, threadPool);
        ClusterService clusterService = new ClusterService(settings, new ClusterSettings(settings, new HashSet<Setting<?>>()), threadPool);
        ResourceWatcherService resourceWatcherService = new ResourceWatcherService(settings, threadPool);
        ScriptService scriptService = new ScriptService(settings, new HashMap<String, ScriptEngine>(), new HashMap<String, ScriptContext<?>>());
        NamedXContentRegistry xContentRegistry = new NamedXContentRegistry(new ArrayList<NamedXContentRegistry.Entry>());
        Environment environment = new Environment(settings, Paths.get(""));
        NodeEnvironment nodeEnvironment = new NodeEnvironment(settings, environment);
        NamedWriteableRegistry namedWriteableRegistry = new NamedWriteableRegistryAPI().getRegistry();
        IndexNameExpressionResolver indexNameExpressionResolver = new IndexNameExpressionResolver(threadPool.getThreadContext());
        final SetOnce<RepositoriesService> repositoriesServiceReference = new SetOnce<>();
        Supplier<RepositoriesService> repositoriesServiceSupplier = repositoriesServiceReference::get;
        assertEquals(threadPool, extension.getThreadPool());
        assertEquals(client, extension.getClient());
        assertEquals(clusterService, extension.getClusterService());
        assertEquals(resourceWatcherService, extension.getResourceWatcherService());
        assertEquals(scriptService, extension.getScriptService());
        assertEquals(xContentRegistry, extension.getNamedXContentRegistry());
        assertEquals(environment, extension.getEnvironment());
        assertEquals(nodeEnvironment, extension.getNodeEnvironment());
        assertEquals(namedWriteableRegistry, extension.getNamedWriteableRegistry());
        assertEquals(indexNameExpressionResolver, extension.getIndexNameExpressionResolver());
        assertEquals(repositoriesServiceSupplier, extension.getRepositoriesServiceSupplier());
    }
    */

}
