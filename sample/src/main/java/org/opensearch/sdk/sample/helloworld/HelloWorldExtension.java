/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.common.settings.Setting;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.sdk.BaseExtension;
import org.opensearch.sdk.Extension;
import org.opensearch.sdk.ExtensionSettings;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.SDKClient;
import org.opensearch.sdk.api.ActionExtension;
import org.opensearch.sdk.rest.ExtensionRestHandler;
import org.opensearch.sdk.sample.helloworld.rest.RestHelloAction;
import org.opensearch.sdk.sample.helloworld.rest.RestRemoteHelloAction;
import org.opensearch.sdk.sample.helloworld.schedule.GreetJob;
import org.opensearch.sdk.sample.helloworld.transport.HWJobParameterAction;
import org.opensearch.sdk.sample.helloworld.transport.HWJobParameterTransportAction;
import org.opensearch.sdk.sample.helloworld.transport.HWJobRunnerAction;
import org.opensearch.sdk.sample.helloworld.transport.HWJobRunnerTransportAction;
import org.opensearch.sdk.sample.helloworld.transport.SampleAction;
import org.opensearch.sdk.sample.helloworld.transport.SampleTransportAction;

/**
 * Sample class to demonstrate how to use the OpenSearch SDK for Java to create
 * an extension.
 * <p>
 * To create your own extension, implement the {@link #getExtensionSettings()} and {@link #getExtensionRestHandlers()} methods.
 * You may either create an {@link ExtensionSettings} object directly with the constructor, or read it from a YAML file on your class path.
 * <p>
 * To execute, pass an instantiated object of this class to {@link ExtensionsRunner#run(Extension)}.
 */
public class HelloWorldExtension extends BaseExtension implements ActionExtension {

    /**
     * Optional classpath-relative path to a yml file containing extension settings.
     */
    private static final String EXTENSION_SETTINGS_PATH = "/sample/helloworld-settings.yml";

    /**
     * Instantiate this extension, initializing the connection settings and REST actions.
     * The Extension must provide its settings to the ExtensionsRunner.
     * These may be optionally read from a YAML file on the class path.
     * Or you may directly instantiate with the ExtensionSettings constructor.
     *
     */
    public HelloWorldExtension() {
        super(EXTENSION_SETTINGS_PATH);
    }

    @Override
    public List<ExtensionRestHandler> getExtensionRestHandlers() {
        return List.of(new RestHelloAction(), new RestRemoteHelloAction(extensionsRunner()));
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return Arrays.asList(
            new ActionHandler<>(SampleAction.INSTANCE, SampleTransportAction.class),
            new ActionHandler<>(HWJobRunnerAction.INSTANCE, HWJobRunnerTransportAction.class),
            new ActionHandler<>(HWJobParameterAction.INSTANCE, HWJobParameterTransportAction.class)
        );
    }

    @Override
    public List<NamedXContentRegistry.Entry> getNamedXContent() {
        return ImmutableList.of(GreetJob.XCONTENT_REGISTRY);
    }

    @Deprecated
    private SDKClient.SDKRestClient createRestClient(ExtensionsRunner runner) {
        @SuppressWarnings("resource")
        SDKClient.SDKRestClient client = runner.getSdkClient().initializeRestClient();
        return client;
    }

    @Override
    public Collection<Object> createComponents(ExtensionsRunner runner) {
        SDKClient.SDKRestClient sdkRestClient = createRestClient(runner);

        return Collections.singletonList(sdkRestClient);
    }

    /**
     * A list of object that includes a single instance of Validator for Custom Setting
     */
    public List<Setting<?>> getSettings() {
        return Arrays.asList(ExampleCustomSettingConfig.VALIDATED_SETTING);
    }

    /**
     * Entry point to execute an extension.
     *
     * @param args  Unused.
     * @throws IOException on a failure in the ExtensionsRunner
     */
    public static void main(String[] args) throws IOException {
        // Execute this extension by instantiating it and passing to ExtensionsRunner
        ExtensionsRunner.run(new HelloWorldExtension());
    }
}
