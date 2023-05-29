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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.ActionType;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamInput;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.extensions.action.RemoteExtensionActionResponse;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.ExtensionsRunnerForTest;
import org.opensearch.sdk.SDKTransportService;
import org.opensearch.test.OpenSearchTestCase;

public class TestProxyActionRequest extends OpenSearchTestCase {
    private ExtensionsRunner extensionsRunner;
    private SDKTransportService sdkTransportService;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.extensionsRunner = new ExtensionsRunnerForTest();
        extensionsRunner.getSdkTransportService().setUniqueId("opensearch-sdk-1");
    }

    @Test
    public void testProxyActionRequest() throws Exception {
        extensionsRunner.startTransportService(sdkTransportService.getTransportService());
        
        TestRequest testRequest = new TestRequest("test-action");

        String expectedAction = TestAction.class.getName();
        String expectedRequestClass = testRequest.getClass().getName();
        byte[] expectedRequestBytes;
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            testRequest.writeTo(out);
            expectedRequestBytes = BytesReference.toBytes(out.bytes());
        }

        RemoteExtensionActionRequest request = new RemoteExtensionActionRequest(TestAction.INSTANCE, testRequest);
        assertEquals(expectedAction, request.getAction());
        assertEquals(expectedRequestClass, request.getRequestClass());
        assertArrayEquals(expectedRequestBytes, request.getRequestBytes());

        request = new RemoteExtensionActionRequest(expectedAction, expectedRequestClass, expectedRequestBytes);

        try (BytesStreamOutput out = new BytesStreamOutput()) {
            request.writeTo(out);
            out.flush();
            try (BytesStreamInput in = new BytesStreamInput(BytesReference.toBytes(out.bytes()))) {
                request = new RemoteExtensionActionRequest(in);

                assertEquals(expectedAction, request.getAction());
                assertEquals(expectedRequestClass, request.getRequestClass());
                assertArrayEquals(expectedRequestBytes, request.getRequestBytes());
            }
        }

        RemoteExtensionActionResponse response = sdkTransportService.sendRemoteExtensionActionRequest(request);
        assertTrue(response.isSuccess());
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

        public static final String NAME = "test";
        public static final TestAction INSTANCE = new TestAction();

        private TestAction() {
            super(NAME, TestResponse::new);
        }

    }

}
