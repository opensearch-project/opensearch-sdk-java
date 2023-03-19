/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.action;

import java.nio.charset.StandardCharsets;

import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamInput;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.test.OpenSearchTestCase;

public class TestProxyActionRequest extends OpenSearchTestCase {
    public void testProxyActionRequest() throws Exception {
        String expectedAction = "test-action";
        byte[] expectedRequestBytes = "request-bytes".getBytes(StandardCharsets.UTF_8);
        ProxyActionRequest request = new ProxyActionRequest(expectedAction, expectedRequestBytes);

        assertEquals(expectedAction, request.getAction());
        assertEquals(expectedRequestBytes, request.getRequestBytes());

        try (BytesStreamOutput out = new BytesStreamOutput()) {
            request.writeTo(out);
            out.flush();
            try (BytesStreamInput in = new BytesStreamInput(BytesReference.toBytes(out.bytes()))) {
                request = new ProxyActionRequest(in);

                assertEquals(expectedAction, request.getAction());
                assertArrayEquals(expectedRequestBytes, request.getRequestBytes());
            }
        }
    }
}
