/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.extensions.rest.RestExecuteOnExtensionRequest;
import org.opensearch.identity.PrincipalIdentifierToken;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.transport.TransportRequest;

import java.io.IOException;
import java.util.Objects;

/**
 * A subclass of {@link TransportRequest} which contains request relevant information
 * to be utilised in ExtensionRestHandler implementation
 */
public class ExtensionRestRequest extends TransportRequest {
    private Method method;
    private String uri;
    /**
     * The owner of this request object
     */
    private PrincipalIdentifierToken principalIdentifierToken;

    /**
     * This object can be instantiated given method, uri and identifier
     * @param method of type {@link Method}
     * @param uri url string
     * @param principalIdentifier the owner of this request
     */
    public ExtensionRestRequest(Method method, String uri, PrincipalIdentifierToken principalIdentifier) {
        this.method = method;
        this.uri = uri;
        this.principalIdentifierToken = principalIdentifier;
    }

    /**
     * The object to be created from rest request object incoming from OpenSearch
     * @param request incoming object from OpenSearch
     * @throws IllegalArgumentException when request is null
     */
    protected ExtensionRestRequest(RestExecuteOnExtensionRequest request) throws IllegalArgumentException {
        if (request == null) throw new IllegalArgumentException("Request object can't be null");
        this.method = request.getMethod();
        this.uri = request.getUri();
        this.principalIdentifierToken = request.getRequestIssuerIdentity();
    }

    /**
     * Object generated from input stream
     * @param in Input stream
     * @throws IOException if there a error generating object from input stream
     */
    public ExtensionRestRequest(StreamInput in) throws IOException {
        super(in);
        method = in.readEnum(Method.class);
        uri = in.readString();
        principalIdentifierToken = in.readNamedWriteable(PrincipalIdentifierToken.class);
    }

    /**
     * Write this object to output stream
     * @param out the writeable output stream
     * @throws IOException if there is an error writing object to output stream
     */
    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeEnum(method);
        out.writeString(uri);
        out.writeNamedWriteable(principalIdentifierToken);
    }

    /**
     * @return This REST request {@link Method} type
     */
    public Method method() {
        return method;
    }

    /**
     * @return This REST request's uri
     */
    public String uri() {
        return uri;
    }

    /**
     * @return This REST request issuer's identity token
     */
    public PrincipalIdentifierToken getRequestIssuerIdentity() {
        return principalIdentifierToken;
    }

    @Override
    public String toString() {
        return "ExtensionRestRequest{method=" + method + ", uri=" + uri + ", requester = " + principalIdentifierToken.getToken() + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ExtensionRestRequest that = (ExtensionRestRequest) obj;
        return Objects.equals(method, that.method)
            && Objects.equals(uri, that.uri)
            && Objects.equals(principalIdentifierToken, that.principalIdentifierToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, uri, principalIdentifierToken);
    }
}
