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
import org.opensearch.action.ActionResponse;
import org.opensearch.action.ActionType;
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
     * action is the TransportAction intended to be invoked which is registered by an extension via {@link ExtensionTransportActionsHandler}.
     */
    private final String action;
    /**
     * requestClass is the ActionRequest class associated with the TransportAction
     */
    private final String requestClass;
    /**
     * requestBytes is the raw bytes being transported between extensions.
     */
    private final byte[] requestBytes;

    /**
     * ProxyActionRequest constructor with an ActionType and Request class. Requires a dependency on the remote extension code.
     *
     * @param instance An instance of {@link ActionType} registered with the remote extension's getActions registry
     * @param request A class extending {@link ActionRequest} associated with an action to be executed on another extension.
     */
    public ProxyActionRequest(ActionType<? extends ActionResponse> instance, ActionRequest request) {
        this.action = instance.getClass().getName();
        this.requestClass = request.getClass().getName();
        byte[] bytes = new byte[0];
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            request.writeTo(out);
            bytes = BytesReference.toBytes(out.bytes());
        } catch (IOException e) {
            // This Should Never Happen (TM)
            // Won't get an IOException locally
        }
        this.requestBytes = bytes;
    }

    /**
     * ProxyActionRequest constructor with class names and request bytes. Does not require a dependency on the remote extension code.
     *
     * @param action A string representing the fully qualified class name of the remote ActionType instance
     * @param requestClass A string representing the fully qualified class name of the remote ActionRequest class
     * @param requestBytes Bytes representing the serialized parameters to be used in the ActionRequest class StreamInput constructor
     */
    public ProxyActionRequest(String action, String requestClass, byte[] requestBytes) {
        this.action = action;
        this.requestClass = requestClass;
        this.requestBytes = requestBytes;
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
        this.requestClass = in.readString();
        this.requestBytes = in.readByteArray();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(action);
        out.writeString(requestClass);
        out.writeByteArray(requestBytes);
    }

    public String getAction() {
        return this.action;
    }

    public String getRequestClass() {
        return this.requestClass;
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
        return "ProxyActionRequest{action=" + action + ", requestClass=" + requestClass + ", requestBytes=" + requestBytes + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProxyActionRequest that = (ProxyActionRequest) obj;
        return Objects.equals(action, that.action)
            && Objects.equals(requestClass, that.requestClass)
            && Objects.equals(requestBytes, that.requestBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, requestClass, requestBytes);
    }
}
