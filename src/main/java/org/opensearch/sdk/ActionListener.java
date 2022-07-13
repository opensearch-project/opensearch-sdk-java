/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.opensearch.common.SuppressForbidden;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.transport.ConnectionProfile;
import org.opensearch.transport.TransportRequestOptions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * A listener for actions on the local port.
 */
public class ActionListener {

    /**
     * Get the local ephemeral port.
     *
     * @return The socket address for localhost.
     * @throws UnknownHostException if the local host name could not be resolved into an address.
     */
    @SuppressForbidden(reason = "need local ephemeral port")
    protected static InetSocketAddress getLocalEphemeral() throws UnknownHostException {
        return new InetSocketAddress(InetAddress.getLocalHost(), 0);
    }

    /**
     * Run the action listener.
     * This is presently a placeholder; when it receives a byte on the listening port, it terminates.
     * Eventually some sort of operation will be added here.
     *
     * @param flag  If true, waits for the other side to send a message.
     * @param timeout  How long to wait, in milliseconds.  If zero, infinite timeout.
     */
    public void runActionListener(boolean flag, int timeout) {
        try (ServerSocket socket = new ServerSocket()) {

            // for testing considerations, otherwise zero which is interpreted as an infinite timeout
            socket.setSoTimeout(timeout);

            socket.bind(getLocalEphemeral(), 1);
            socket.setReuseAddress(true);

            Thread t = new Thread() {
                @Override
                public void run() {
                    try (Socket accept = socket.accept()) {
                        if (flag) { // sometimes wait until the other side sends the message
                            accept.getInputStream().read();
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            };
            t.start();
            ConnectionProfile.Builder builder = new ConnectionProfile.Builder();
            builder.addConnections(
                1,
                TransportRequestOptions.Type.BULK,
                TransportRequestOptions.Type.PING,
                TransportRequestOptions.Type.RECOVERY,
                TransportRequestOptions.Type.REG,
                TransportRequestOptions.Type.STATE
            );
            builder.setHandshakeTimeout(TimeValue.timeValueHours(1));
            t.join();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
