/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Supplier;

import org.opensearch.client.Client;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.env.NodeEnvironment;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.script.ScriptService;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.watcher.ResourceWatcherService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * This interface defines methods which an extension must provide. Extensions
 * will instantiate a class implementing this interface and pass it to the
 * {@link ExtensionsRunner} constructor to initialize.
 */
public interface Extension {

    /**
     * Gets the {@link ExtensionSettings} of this extension.
     *
     * @return the extension settings.
     */
    ExtensionSettings getExtensionSettings();

    /**
     * Gets a list of {@link ExtensionRestHandler} implementations this extension handles.
     *
     * @return a list of REST handlers (REST actions) this extension handles.
     */
    List<ExtensionRestHandler> getExtensionRestHandlers();

    /**
     * Gets the {@link Client} of this extension
     * 
     * @return the client
     */
    Client getClient();

    /**
     * Gets the {@link ClusterService} of this extension
     * 
     * @return the cluster service
     */
    ClusterService getClusterService();

    /**
     * Gets the {@link ThreadPool} of this extension
     * 
     * @return the thread pool
     */
    ThreadPool getThreadPool();

    /**
     * Gets the {@link ResourceWatcherService} of this extension
     * 
     * @return the resource watcher service
     */
    ResourceWatcherService getResourceWatcherService();

    /**
     * Gets the {@link ScriptService} of this extension
     * 
     * @return the script service
     */
    ScriptService getScriptService();

    /**
     * Gets the {@link NamedXContentRegistry} of this extension
     * 
     * @return the NamedXContentRegistry
     */
    NamedXContentRegistry getNamedXContentRegistry();

    /**
     * Gets the {@link Environment} of this extension
     * 
     * @return the environment
     */
    Environment getEnvironment();

    /**
     * Gets the {@link NodeEnvironment} of this extension
     * 
     * @return the node environment
     */
    NodeEnvironment getNodeEnvironment();

    /**
     * Gets the {@link NamedWritableRegistry} of this extension
     * 
     * @return the NamedWritableRegistry
     */
    NamedWriteableRegistry getNamedWriteableRegistry();

    /**
     * Gets the {@link IndexNameExpressionResolver} of this extension
     * 
     * @return the IndexNameExpressionResolver
     */
    IndexNameExpressionResolver getIndexNameExpressionResolver();

    /**
     * Gets the {@link Supplier} of {@link RepositoriesService} of this extension
     * 
     * @return the repositories service supplier
     */
    Supplier<RepositoriesService> getRepositoriesServiceSupplier();

    /**
     * Helper method to read extension settings from a YAML file.
     *
     * @param extensionSettingsPath
     *            The path (relative to the classpath) of the extension settings
     *            file.
     * @return A settings file encapsulating the extension host and port if the file
     *         exists, null otherwise.
     * @throws IOException
     *             if there is an error reading the file.
     */
    static ExtensionSettings readSettingsFromYaml(String extensionSettingsPath) throws IOException {
        URL resource = Extension.class.getResource(extensionSettingsPath);
        if (resource == null) {
            return null;
        }
        File file = new File(resource.getPath());
        if (!file.exists()) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(file, ExtensionSettings.class);
    }
}
