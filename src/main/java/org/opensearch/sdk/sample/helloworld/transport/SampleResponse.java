/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.transport;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * A sample response class to demonstrate extension actions
 */
public class SampleResponse extends ActionResponse {

    private final String greeting;

    /**
     * Instantiate this response
     *
     * @param greeting The greeting to return
     */
    public SampleResponse(String greeting) {
        this.greeting = greeting;
    }

    /**
     * Instantiate this response from a byte stream
     *
     * @param in the byte stream
     * @throws IOException on failure reading the stream
     */
    public SampleResponse(StreamInput in) throws IOException {
        this.greeting = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(greeting);
    }

    public String getGreeting() {
        return greeting;
    }
}
