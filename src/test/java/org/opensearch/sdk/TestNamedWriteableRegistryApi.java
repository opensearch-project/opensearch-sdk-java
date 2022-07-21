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

import static java.util.Collections.emptySet;
import static java.util.Collections.emptyMap;

import org.opensearch.Version;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.io.stream.NamedWriteable;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.NamedWriteableRegistryParseRequest;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.NamedWriteableRegistryResponse;
import org.opensearch.common.io.stream.OutputStreamStreamOutput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.extensions.BooleanResponse;
import org.opensearch.extensions.OpenSearchRequest;
import org.opensearch.extensions.ExtensionsOrchestrator.OpenSearchRequestType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestNamedWriteableRegistryApi extends OpenSearchTestCase {
    private List<NamedWriteableRegistry.Entry> namedWriteables;
    private NamedWriteableRegistryApi namedWriteableRegistryApi;

    private static class Example implements NamedWriteable {
        public static final String INVALID_NAME = "invalid_name";
        public static final String NAME = "example";
        private final String message;

        Example(String message) {
            this.message = message;
        }

        Example(StreamInput in) throws IOException {
            this.message = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(message);
        }

        @Override
        public String getWriteableName() {
            return NAME;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Example that = (Example) o;
            return Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(message);
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        this.namedWriteables = Collections.singletonList(new NamedWriteableRegistry.Entry(Example.class, Example.NAME, Example::new));
        this.namedWriteableRegistryApi = new NamedWriteableRegistryApi(namedWriteables);
    }

    @Test
    public void testNamedWriteableRegistryCreation() {
        assert (namedWriteableRegistryApi.getRegistry() instanceof NamedWriteableRegistry);
    }

    @Test
    public void testNamedWriteableRegistryRequest() throws UnknownHostException {
        DiscoveryNode sourceNode = new DiscoveryNode(
            "test_node",
            new TransportAddress(InetAddress.getByName("localhost"), 9876),
            emptyMap(),
            emptySet(),
            Version.CURRENT
        );
        OpenSearchRequest request = new OpenSearchRequest(OpenSearchRequestType.REQUEST_OPENSEARCH_NAMED_WRITEABLE_REGISTRY);
        NamedWriteableRegistryResponse response = namedWriteableRegistryApi.handleNamedWriteableRegistryRequest(request);

        // Verify that the api processes named writeable registry entries successfully within the response
        assertEquals(response.getRegistry().size(), 1);
        assertEquals(response.getRegistry().get(Example.NAME), Example.class);
    }

    @Test
    public void testNamedWriteableRegistryParseRequest() throws UnknownHostException {
        DiscoveryNode sourceNode = new DiscoveryNode(
            "test_node",
            new TransportAddress(InetAddress.getByName("localhost"), 9876),
            emptyMap(),
            emptySet(),
            Version.CURRENT
        );

        // convert stream output into byte array
        byte[] context = null;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        OutputStreamStreamOutput out = new OutputStreamStreamOutput(buf);
        try {

            // named writeables are always preceded by the writeable name within the streaminput
            out.writeString(Example.NAME);
            out.writeString("test_message");
            context = buf.toByteArray();

            NamedWriteableRegistryParseRequest request = new NamedWriteableRegistryParseRequest(Example.class.getName(), context);
            BooleanResponse response = namedWriteableRegistryApi.handleNamedWriteableRegistryParseRequest(request);

            // verify that byte array deserialization was successful
            assertEquals(response.getStatus(), true);
        } catch (IOException e) {}
    }

    @Test
    public void testInvalidNamedWriteableRegistryParseRequest() throws UnknownHostException {
        DiscoveryNode sourceNode = new DiscoveryNode(
            "test_node",
            new TransportAddress(InetAddress.getByName("localhost"), 9876),
            emptyMap(),
            emptySet(),
            Version.CURRENT
        );

        // convert stream output into byte array
        byte[] context = null;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        OutputStreamStreamOutput out = new OutputStreamStreamOutput(buf);
        try {

            // write the invalid name to the stream output
            out.writeString(Example.INVALID_NAME);
            out.writeString("test_message");
            context = buf.toByteArray();

            try {
                NamedWriteableRegistryParseRequest request = new NamedWriteableRegistryParseRequest(Example.class.getName(), context);
                BooleanResponse response = namedWriteableRegistryApi.handleNamedWriteableRegistryParseRequest(request);
            } catch (IllegalArgumentException e) {
                assertEquals(e.getMessage(), "Unknown NamedWriteable [" + Example.class.getName() + "][" + Example.INVALID_NAME + "]");
            }
        } catch (IOException e) {}
    }
}
