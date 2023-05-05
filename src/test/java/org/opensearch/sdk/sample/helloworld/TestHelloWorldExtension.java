/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.ActionType;
import org.opensearch.action.support.TransportAction;
import org.opensearch.client.Node;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.sdk.api.ActionExtension.ActionHandler;
import org.opensearch.sdk.rest.ExtensionRestHandler;
import org.opensearch.sdk.sample.helloworld.transport.SampleAction;
import org.opensearch.sdk.sample.helloworld.transport.SampleRequest;
import org.opensearch.sdk.sample.helloworld.transport.SampleResponse;
import org.opensearch.tasks.TaskManager;
import org.opensearch.sdk.ExtensionSettings;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.SDKClient;
import org.opensearch.sdk.SDKClient.SDKRestClient;
import org.opensearch.sdk.action.SDKActionModule;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.ThreadPool;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;

import static org.opensearch.sdk.sample.helloworld.ExampleCustomSettingConfig.VALIDATED_SETTING;

@SuppressWarnings("deprecation")
public class TestHelloWorldExtension extends OpenSearchTestCase {

    private HelloWorldExtension extension;
    private Injector injector;
    private SDKClient sdkClient;
    private SDKRestClient sdkRestClient;
    private final ExtensionSettings extensionSettings = new ExtensionSettings("", "", "", "localhost", "9200");

    static class UnregisteredAction extends ActionType<SampleResponse> {
        public static final String NAME = "helloworld/unregistered";
        public static final UnregisteredAction INSTANCE = new UnregisteredAction();

        private UnregisteredAction() {
            super(NAME, SampleResponse::new);
        }
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.extension = new HelloWorldExtension();

        // Do portions of Guice injection needed for this test
        Settings settings = Settings.builder().put(ExtensionsRunner.NODE_NAME_SETTING, "test").build();
        ThreadPool threadPool = new ThreadPool(settings);
        TaskManager taskManager = new TaskManager(settings, threadPool, Collections.emptySet());
        this.sdkClient = new SDKClient(extensionSettings);
        this.injector = Guice.createInjector(new SDKActionModule(extension), b -> {
            b.bind(ThreadPool.class).toInstance(threadPool);
            b.bind(TaskManager.class).toInstance(taskManager);
            b.bind(SDKClient.class).toInstance(sdkClient);
        });
        initializeSdkClient();
        this.sdkRestClient = sdkClient.initializeRestClient("localhost", 9200);
    }

    @SuppressWarnings("rawtypes")
    private void initializeSdkClient() {
        sdkClient.initialize(this.injector.getInstance(new Key<Map<ActionType, TransportAction>>() {
        }));
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        this.sdkRestClient.close();
        this.injector = null;
    }

    @Test
    public void testExtensionSettings() {
        // This effectively tests the Extension interface helper method
        ExtensionSettings extensionSettings = extension.getExtensionSettings();
        ExtensionSettings expected = new ExtensionSettings("hello-world", "127.0.0.1", "4532", "127.0.0.1", "9200");
        assertEquals(expected.getExtensionName(), extensionSettings.getExtensionName());
        assertEquals(expected.getHostAddress(), extensionSettings.getHostAddress());
        assertEquals(expected.getHostPort(), extensionSettings.getHostPort());
    }

    @Test
    public void testExtensionSettingsUpdate() {
        List<Node> nodes = this.sdkClient.getSdkRestClient().getRestHighLevelClient().getLowLevelClient().getNodes();
        assertEquals(1, nodes.size());
        HttpHost host = nodes.get(0).getHost();
        assertEquals("localhost", host.getHostName());
        assertEquals(9200, host.getPort());

        this.sdkClient.updateOpenSearchNodeSettings(new TransportAddress(new InetSocketAddress("10.10.10.10", 9300)));
        nodes = this.sdkClient.getSdkRestClient().getRestHighLevelClient().getLowLevelClient().getNodes();
        assertEquals(1, nodes.size());
        host = nodes.get(0).getHost();
        assertEquals("10.10.10.10", host.getHostName());
        assertEquals(9300, host.getPort());
    }

    @Test
    public void testExtensionRestHandlers() {
        List<ExtensionRestHandler> extensionRestHandlers = extension.getExtensionRestHandlers();
        assertEquals(2, extensionRestHandlers.size());
        assertEquals(4, extensionRestHandlers.get(0).routes().size());
        assertEquals(1, extensionRestHandlers.get(1).routes().size());
    }

    @Test
    public void testGetActions() {
        List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> actions = extension.getActions();
        assertEquals(1, actions.size());
    }

    @Test
    public void testClientGetActionFromClassName() {
        ActionType<SampleResponse> action = SampleAction.INSTANCE;
        assertEquals(action, sdkClient.getActionFromClassName(action.getClass().getName()));
    }

    @Test
    public void testClientExecuteSampleActions() throws Exception {
        String expectedName = "world";
        String expectedGreeting = "Hello, " + expectedName;
        SampleRequest request = new SampleRequest(expectedName);
        CompletableFuture<SampleResponse> responseFuture = new CompletableFuture<>();

        sdkClient.execute(SampleAction.INSTANCE, request, new ActionListener<SampleResponse>() {
            @Override
            public void onResponse(SampleResponse response) {
                responseFuture.complete(response);
            }

            @Override
            public void onFailure(Exception e) {
                responseFuture.completeExceptionally(e);
            }
        });

        SampleResponse response = responseFuture.get(1, TimeUnit.SECONDS);
        assertEquals(expectedGreeting, response.getGreeting());
    }

    @Test
    public void testRestClientExecuteSampleActions() throws Exception {
        String expectedName = "world";
        String expectedGreeting = "Hello, " + expectedName;
        SampleRequest request = new SampleRequest(expectedName);
        CompletableFuture<SampleResponse> responseFuture = new CompletableFuture<>();

        sdkRestClient.execute(SampleAction.INSTANCE, request, new ActionListener<SampleResponse>() {
            @Override
            public void onResponse(SampleResponse response) {
                responseFuture.complete(response);
            }

            @Override
            public void onFailure(Exception e) {
                responseFuture.completeExceptionally(e);
            }
        });

        SampleResponse response = responseFuture.get(1, TimeUnit.SECONDS);
        assertEquals(expectedGreeting, response.getGreeting());
    }

    @Test
    public void testExceptionalClientExecuteSampleActions() throws Exception {
        String expectedName = "";
        SampleRequest request = new SampleRequest(expectedName);
        CompletableFuture<SampleResponse> responseFuture = new CompletableFuture<>();

        sdkClient.execute(SampleAction.INSTANCE, request, new ActionListener<SampleResponse>() {
            @Override
            public void onResponse(SampleResponse response) {
                responseFuture.complete(response);
            }

            @Override
            public void onFailure(Exception e) {
                responseFuture.completeExceptionally(e);
            }
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> responseFuture.get(1, TimeUnit.SECONDS));
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof IllegalArgumentException);
        assertEquals("The request name is blank.", cause.getMessage());
    }

    @Test
    public void testExceptionalRestClientExecuteSampleActions() throws Exception {
        String expectedName = "";
        SampleRequest request = new SampleRequest(expectedName);
        CompletableFuture<SampleResponse> responseFuture = new CompletableFuture<>();

        sdkRestClient.execute(SampleAction.INSTANCE, request, new ActionListener<SampleResponse>() {
            @Override
            public void onResponse(SampleResponse response) {
                responseFuture.complete(response);
            }

            @Override
            public void onFailure(Exception e) {
                responseFuture.completeExceptionally(e);
            }
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> responseFuture.get(1, TimeUnit.SECONDS));
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof IllegalArgumentException);
        assertEquals("The request name is blank.", cause.getMessage());
    }

    @Test
    public void testSampleActionDoesNotExist() throws Exception {
        String expectedName = "";
        SampleRequest request = new SampleRequest(expectedName);
        CompletableFuture<SampleResponse> responseFuture = new CompletableFuture<>();

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> sdkClient.execute(UnregisteredAction.INSTANCE, request, new ActionListener<SampleResponse>() {
                @Override
                public void onResponse(SampleResponse response) {
                    responseFuture.complete(response);
                }

                @Override
                public void onFailure(Exception e) {
                    responseFuture.completeExceptionally(e);
                }
            })
        );

        assertEquals("failed to find action [" + UnregisteredAction.INSTANCE + "] to execute", ex.getMessage());
    }

    @Test
    public void testValidatedSettings() {
        final String expected = "foo";
        final String actual = VALIDATED_SETTING.get(Settings.builder().put(VALIDATED_SETTING.getKey(), expected).build());
        assertEquals(expected, actual);

        final IllegalArgumentException exceptionTrue = expectThrows(
            IllegalArgumentException.class,
            () -> VALIDATED_SETTING.get(Settings.builder().put(VALIDATED_SETTING.getKey(), "it's forbidden").build())
        );

        assertEquals("Setting [it's forbidden] must match regex [forbidden]", exceptionTrue.getMessage());
    }
}
