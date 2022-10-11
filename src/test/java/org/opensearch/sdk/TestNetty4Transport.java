/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.sdk;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.opensearch.common.component.Lifecycle;
import org.opensearch.common.settings.Settings;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.TransportSettings;

import org.opensearch.transport.netty4.Netty4Transport;

@ResourceLock("transportService")
public class TestNetty4Transport extends OpenSearchTestCase {

    private ThreadPool threadPool;
    private ExtensionsRunner extensionsRunner;
    private NettyTransport nettyTransport;
    private TransportService initialTransportService;

    @BeforeEach
    public void setUp() throws IOException {
        this.threadPool = new TestThreadPool("test");
        this.extensionsRunner = new ExtensionsRunnerForTest();
        this.initialTransportService = extensionsRunner.extensionTransportService;
        this.nettyTransport = new NettyTransport(extensionsRunner);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        if (initialTransportService != null) {
            this.initialTransportService.stop();
            this.initialTransportService.close();
            Thread.sleep(1000);
        }
    }

    // test Netty can bind to multiple ports, default and additional client
    @Test
    public void testNettyCanBindToMultiplePorts() throws IOException {

        Settings settings = Settings.builder()
            .put("node.name", "netty_test")
            .put(TransportSettings.BIND_HOST.getKey(), "127.0.0.1")
            .put("transport.profiles.default.port", 0)
            .put("transport.profiles.client1.port", 0)
            .build();

        Netty4Transport transport = nettyTransport.getNetty4Transport(settings, threadPool);

        try {
            startNetty4Transport(transport);
            assertEquals(1, transport.profileBoundAddresses().size());
            assertEquals(1, transport.boundAddress().boundAddresses().length);
        } finally {
            stopNetty4Transport(transport);
            terminate(threadPool);
        }
    }

    // test that default profile inherits from standard settings
    @Test
    public void testDefaultProfileInheritsFomStandardSettings() throws IOException {

        // omit transport.profiles.default.port setting to determine if default port is automatically set
        Settings settings = Settings.builder()
            .put("node.name", "netty_test")
            .put(TransportSettings.BIND_HOST.getKey(), "127.0.0.1")
            .put("transport.profiles.client1.port", 0)
            .build();

        Netty4Transport transport = nettyTransport.getNetty4Transport(settings, threadPool);

        try {
            startNetty4Transport(transport);
            assertEquals(1, transport.profileBoundAddresses().size());
            assertEquals(1, transport.boundAddress().boundAddresses().length);
        } finally {
            stopNetty4Transport(transport);
            terminate(threadPool);
        }
    }

    // test profile without port settings fails
    @Test
    public void testThatProfileWithoutPortFails() throws IOException {

        // settings without port for profile no_port
        Settings settings = Settings.builder()
            .put("node.name", "netty_test")
            .put(TransportSettings.BIND_HOST.getKey(), "127.0.0.1")
            .put("transport.profiles.no_port.foo", "bar")
            .build();

        try {
            // attempt creating netty object with invalid settings
            IllegalStateException ex = expectThrows(
                IllegalStateException.class,
                () -> nettyTransport.getNetty4Transport(settings, threadPool)
            );
            assertEquals("profile [no_port] has no port configured", ex.getMessage());
        } finally {
            terminate(threadPool);
        }
    }

    // test default profile port overrides general config
    @Test
    public void testDefaultProfilePortOverridesGeneralConfiguration() throws IOException {
        Settings settings = Settings.builder()
            .put("node.name", "netty_test")
            .put(TransportSettings.BIND_HOST.getKey(), "127.0.0.1")
            .put(TransportSettings.PORT.getKey(), "22") // attempt to bind SSH port will throw exception
            .put("transport.profiles.default.port", 0) // default port configuration will overwrite attempt
            .build();

        Netty4Transport transport = nettyTransport.getNetty4Transport(settings, threadPool);

        try {
            startNetty4Transport(transport);
            assertEquals(0, transport.profileBoundAddresses().size());
            assertEquals(1, transport.boundAddress().boundAddresses().length);
        } finally {
            terminate(threadPool);
        }
    }

    // helper method to ensure netty transport was started
    private void startNetty4Transport(Netty4Transport transport) {
        transport.start();
        assertEquals(Lifecycle.State.STARTED, transport.lifecycleState());
    }

    private void stopNetty4Transport(Netty4Transport transport) {
        transport.close();
        assertEquals(Lifecycle.State.CLOSED, transport.lifecycleState());
    }
}
