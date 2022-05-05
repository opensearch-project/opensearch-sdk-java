/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package opensearchSDK.transport;

import org.opensearch.common.SuppressForbidden;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.transport.ConnectionProfile;
import org.opensearch.transport.TransportRequestOptions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;

public class ActionListener {

    @SuppressForbidden(reason = "need local ephemeral port")
    protected static InetSocketAddress getLocalEphemeral() throws UnknownHostException {
        return new InetSocketAddress(InetAddress.getLocalHost(), 0);
    }

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
