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
import java.util.Objects;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.extensions.action.ExtensionTransportActionsHandler;

/**
 * A request class to request an action be executed on another extension
 */
public class ProxyActionRequest extends ActionRequest {
    /**
     * action is the transport action intended to be invoked which is registered by an extension via {@link ExtensionTransportActionsHandler}.
     */
    private final String action;
    /**
     * requestBytes is the raw bytes being transported between extensions.
     */
    private final byte[] requestBytes;

    /**
     * ProxyActionRequest constructor.
     *
     * @param action is the transport action intended to be invoked which is registered by an extension via {@link ExtensionTransportActionsHandler}.
     * @param requestBytes is the raw bytes being transported between extensions.
     */
    public ProxyActionRequest(String action, byte[] requestBytes) {
        this.action = action;
        this.requestBytes = requestBytes;
    }

    /**
     * ProxyAcctionRequest constructor with a request class
     *
     * @param request A class extending {@link ActionRequest} associated with an action to be executed on another extension.
     */
    public ProxyActionRequest(ActionRequest request) {
        this.action = request.getClass().getName();
        byte[] bytes = new byte[0];
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            request.writeTo(out);
            out.flush();
            bytes = BytesReference.toBytes(out.bytes());
        } catch (IOException e) {
            // This Should Never Happen (TM)
            // Won't get an IOException locally
        }
        this.requestBytes = bytes;
    }

    /**
     * ProxyActionRequest constructor from {@link StreamInput}.
     *
     * @param in bytes stream input used to de-serialize the message.
     * @throws IOException when message de-serialization fails.
     */
    public ProxyActionRequest(StreamInput in) throws IOException {
        super(in);
        this.action = in.readString();
        this.requestBytes = in.readByteArray();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(action);
        out.writeByteArray(requestBytes);
    }

    public String getAction() {
        return this.action;
    }

    public byte[] getRequestBytes() {
        return this.requestBytes;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    @Override
    public String toString() {
        return "ProxyActionRequest{action=" + action + ", requestBytes=" + requestBytes + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProxyActionRequest that = (ProxyActionRequest) obj;
        return Objects.equals(action, that.action) && Objects.equals(requestBytes, that.requestBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, requestBytes);
    }
}
