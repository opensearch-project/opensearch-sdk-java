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
import org.opensearch.core.action.ActionResponse;
import org.opensearch.action.ActionType;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.extensions.action.ExtensionTransportActionsHandler;

/**
 * A request class to request an action be executed on another extension
 */
public class RemoteExtensionActionRequest extends ActionRequest {
    /**
     * The Unicode UNIT SEPARATOR used to separate the Request class name and parameter bytes
     */
    public static final byte UNIT_SEPARATOR = (byte) '\u001F';
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
     * <p>
     * This array is the serialized bytes used to instantiate the {@link #requestClass} instance using its StreamInput constructor.
     */
    private final byte[] requestBytes;

    /**
     * RemoteExtensionActionRequest constructor with an ActionType and Request class. Requires a dependency on the remote extension code.
     *
     * @param instance An instance of {@link ActionType} registered with the remote extension's getActions registry
     * @param request A class extending {@link ActionRequest} associated with an action to be executed on another extension.
     */
    public RemoteExtensionActionRequest(ActionType<? extends ActionResponse> instance, ActionRequest request) {
        this.action = instance.getClass().getName();
        this.requestClass = request.getClass().getName();
        byte[] bytes = new byte[0];
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            request.writeTo(out);
            bytes = BytesReference.toBytes(out.bytes());
        } catch (IOException e) {
            throw new IllegalStateException("Writing an OutputStream to memory should never result in an IOException.");
        }
        this.requestBytes = bytes;
    }

    /**
     * RemoteExtensionActionRequest constructor with class names and request bytes. Does not require a dependency on the remote extension code.
     *
     * @param action A string representing the fully qualified class name of the remote ActionType instance
     * @param requestClass A string representing the fully qualified class name of the remote ActionRequest class
     * @param requestBytes Bytes representing the serialized parameters to be used in the ActionRequest class StreamInput constructor
     */
    public RemoteExtensionActionRequest(String action, String requestClass, byte[] requestBytes) {
        this.action = action;
        this.requestClass = requestClass;
        this.requestBytes = requestBytes;
    }

    /**
     * RemoteExtensionActionRequest constructor from {@link StreamInput}.
     *
     * @param in bytes stream input used to de-serialize the message.
     * @throws IOException when message de-serialization fails.
     */
    public RemoteExtensionActionRequest(StreamInput in) throws IOException {
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
        return "RemoteExtensionActionRequest{action=" + action + ", requestClass=" + requestClass + ", requestBytes=" + requestBytes + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RemoteExtensionActionRequest that = (RemoteExtensionActionRequest) obj;
        return Objects.equals(action, that.action)
            && Objects.equals(requestClass, that.requestClass)
            && Objects.equals(requestBytes, that.requestBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, requestClass, requestBytes);
    }
}
