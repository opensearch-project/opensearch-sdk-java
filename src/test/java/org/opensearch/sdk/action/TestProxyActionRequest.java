/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.action;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.ActionType;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamInput;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
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

    public void testProxyActionRequestWithClass() throws Exception {

        ProxyActionRequest request = new ProxyActionRequest(TestAction.INSTANCE, new TestRequest("test-action"));

        String expectedAction = request.getClass().getName();
        byte[] expectedRequestBytes = "test-action".getBytes(StandardCharsets.UTF_8);

        assertEquals(expectedAction, request.getAction());
        assertEquals(expectedRequestBytes, request.getRequestBytes());
    }

    static class TestRequest extends ActionRequest {

        private String data;

        public TestRequest(String data) {
            this.data = data;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(data);
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }
    }

    static class TestResponse extends ActionResponse {
        public TestResponse(StreamInput in) {}

        @Override
        public void writeTo(StreamOutput out) throws IOException {}
    }

    static class TestAction extends ActionType<TestResponse> {

        public static final String NAME = "helloworld/sample";
        public static final TestAction INSTANCE = new TestAction();

        private TestAction() {
            super(NAME, TestResponse::new);
        }

    }

}
