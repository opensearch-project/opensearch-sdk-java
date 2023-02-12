/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.inject;

import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.sdk.Extension;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.SDKClient;
import org.opensearch.sdk.SDKClusterService;
import org.opensearch.threadpool.ThreadPool;

import com.google.inject.AbstractModule;

/**
 * A Guice module defining the bindings injected and available to Extensions
 */
public class ExtensionModule extends AbstractModule {
    private final ExtensionsRunner extensionsRunner;

    /**
     * Instantiate this module.
     *
     * @param extensionsRunner the {@link ExtensionsRunner} running the extension.
     */
    public ExtensionModule(ExtensionsRunner extensionsRunner) {
        super();
        this.extensionsRunner = extensionsRunner;
    }

    @Override
    protected void configure() {
        bind(ExtensionsRunner.class).toInstance(extensionsRunner);
        bind(Extension.class).toInstance(extensionsRunner.getExtension());

        bind(NamedXContentRegistry.class).toInstance(extensionsRunner.getNamedXContentRegistry().getRegistry());
        bind(ThreadPool.class).toInstance(extensionsRunner.getThreadPool());

        bind(SDKClient.class).toInstance(new SDKClient());
        bind(SDKClusterService.class).toInstance(new SDKClusterService(extensionsRunner));
    }
}
