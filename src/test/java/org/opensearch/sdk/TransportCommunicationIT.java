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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.common.component.Lifecycle;
import org.opensearch.common.network.NetworkAddress;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.transport.netty4.Netty4Transport;
import org.opensearch.test.OpenSearchIntegTestCase;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.TransportSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class TransportCommunicationIT extends OpenSearchIntegTestCase {

    private Settings settings;
    private final int port = 7777;
    private final String host = "127.0.0.1";
    private volatile String clientResult;
    private ExtensionsRunner extensionsRunner;
    private NettyTransport nettyTransport;

    @Override
    @BeforeEach
    public void setUp() throws IOException {

        // Configure settings for transport serivce using the same port number used to bind the client
        settings = Settings.builder()
            .put("node.name", "node_extension_test")
            .put(TransportSettings.BIND_HOST.getKey(), host)
            .put(TransportSettings.PORT.getKey(), port)
            .build();
        this.extensionsRunner = new ExtensionsRunnerForTest();
        this.nettyTransport = new NettyTransport(extensionsRunner);
    }

    @Test
    public void testSocketSetup() throws IOException {

        ThreadPool threadPool = new TestThreadPool("test");
        Netty4Transport transport = nettyTransport.getNetty4Transport(settings, threadPool);

        // start netty transport and ensure that address info is exposed
        try {
            transport.start();
            assertEquals(Lifecycle.State.STARTED, transport.lifecycleState());

            // check bound addresses
            for (TransportAddress transportAddress : transport.boundAddress().boundAddresses()) {
                assert (transportAddress instanceof TransportAddress);
                assertEquals(host, transportAddress.getAddress());
                assertEquals(port, transportAddress.getPort());
            }

            // check publish addresses
            assert (transport.boundAddress().publishAddress() instanceof TransportAddress);
            TransportAddress publishAddress = transport.boundAddress().publishAddress();
            assertEquals(host, NetworkAddress.format(publishAddress.address().getAddress()));
            assertEquals(port, publishAddress.address().getPort());

        } finally {
            // terminate server socket and thread pool
            transport.close();
            assertEquals(Lifecycle.State.CLOSED, transport.lifecycleState());

            terminate(threadPool);
        }
    }

    @Test
    public void testInvalidMessageFormat() throws IOException, InterruptedException {
        Thread client = new Thread() {
            @Override
            public void run() {
                try {

                    // Connect to the server
                    Socket socket = new Socket(host, port);

                    // Create input/output stream to read/write to server
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintStream out = new PrintStream(socket.getOutputStream());

                    // note : message validation is only done if message length >= 6 bytes
                    out.println("TRANSPORT_TEST");

                    // disconnection by foreign host indicated by a read return value of -1
                    clientResult = String.valueOf(in.read());

                    // Close stream and socket connection
                    out.close();
                    socket.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        // start transport service and attempt tcp client connection
        startTransportandClient(settings, client);

        // expecting -1 from client attempt to read from server, indicating connection closed by host
        assertEquals("-1", clientResult);
    }

    @Test
    public void testMismatchingPort() throws IOException, InterruptedException {

        Thread client = new Thread() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(host, 0);
                    socket.close();
                } catch (Exception e) {
                    clientResult = "connection refused";
                }

            }
        };

        // start transport service and attempt client connection
        startTransportandClient(settings, client);

        // confirm that connect exception was caught
        assertEquals("connection refused", clientResult);
    }

    private void startTransportandClient(Settings settings, Thread client) throws IOException, InterruptedException {

        // retrieve transport service
        ExtensionsRunner extensionsRunner = new ExtensionsRunnerForTest();
        // start transport service
        ThreadPool threadPool = new ThreadPool(settings);
        TransportService transportService = nettyTransport.initializeExtensionTransportService(settings, threadPool);

        assertEquals(Lifecycle.State.STARTED, transportService.lifecycleState());

        // connect client server to transport service
        client.start();

        // listen for messages, set timeout to close server socket connection
        extensionsRunner.startActionListener(1000);

        // wait for client thread to finish execution, then close server socket connection
        client.join();
        transportService.close();
        assertEquals(Lifecycle.State.CLOSED, transportService.lifecycleState());
    }

}
