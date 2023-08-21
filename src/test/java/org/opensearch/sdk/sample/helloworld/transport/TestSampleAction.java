/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.transport;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.core.common.io.stream.BytesStreamInput;
import org.opensearch.test.OpenSearchTestCase;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TestSampleAction extends OpenSearchTestCase {

    @Test
    public void testSampleAction() {
        SampleAction action = SampleAction.INSTANCE;
        assertEquals(SampleAction.NAME, action.name());
    }

    @Test
    public void testSampleRequest() throws IOException {
        String name = "test";
        SampleRequest request = new SampleRequest(name);
        assertEquals(name, request.getName());

        try (BytesStreamOutput out = new BytesStreamOutput()) {
            request.writeTo(out);
            out.flush();
            try (BytesStreamInput in = new BytesStreamInput(BytesReference.toBytes(out.bytes()))) {
                request = new SampleRequest(in);
                assertEquals(name, request.getName());
            }
        }
    }

    @Test
    public void testSampleResponse() throws IOException {
        String greeting = "test";
        SampleResponse response = new SampleResponse(greeting);
        assertEquals(greeting, response.getGreeting());

        try (BytesStreamOutput out = new BytesStreamOutput()) {
            response.writeTo(out);
            out.flush();
            try (BytesStreamInput in = new BytesStreamInput(BytesReference.toBytes(out.bytes()))) {
                response = new SampleResponse(in);
                assertEquals(greeting, response.getGreeting());
            }
        }
    }

    @Test
    public void testSampleTransportAction() throws Exception {
        String expectedName = "world";
        String expectedGreeting = "Hello, " + expectedName;

        SampleRequest request = new SampleRequest(expectedName);
        CompletableFuture<SampleResponse> responseFuture = new CompletableFuture<>();
        ActionListener<SampleResponse> listener = new ActionListener<SampleResponse>() {

            @Override
            public void onResponse(SampleResponse response) {
                responseFuture.complete(response);
            }

            @Override
            public void onFailure(Exception e) {
                responseFuture.completeExceptionally(e);
            }
        };

        // test successful response
        SampleTransportAction action = new SampleTransportAction(null, new ActionFilters(Collections.emptySet()), null);
        action.doExecute(null, request, listener);
        SampleResponse response = responseFuture.get(1, TimeUnit.SECONDS);
        assertEquals(expectedGreeting, response.getGreeting());
    }

    @Test
    public void testExceptionalSampleTransportAction() throws Exception {
        String expectedName = "";

        SampleRequest request = new SampleRequest(expectedName);
        CompletableFuture<SampleResponse> responseFuture = new CompletableFuture<>();
        ActionListener<SampleResponse> listener = new ActionListener<SampleResponse>() {

            @Override
            public void onResponse(SampleResponse response) {
                responseFuture.complete(response);
            }

            @Override
            public void onFailure(Exception e) {
                responseFuture.completeExceptionally(e);
            }
        };

        SampleTransportAction action = new SampleTransportAction(null, new ActionFilters(Collections.emptySet()), null);
        action.doExecute(null, request, listener);
        ExecutionException ex = assertThrows(ExecutionException.class, () -> responseFuture.get(1, TimeUnit.SECONDS));
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof IllegalArgumentException);
        assertEquals("The request name is blank.", cause.getMessage());
    }
}
