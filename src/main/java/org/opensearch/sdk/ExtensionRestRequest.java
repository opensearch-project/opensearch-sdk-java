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

public class ExtensionRestRequest extends TransportRequest {

    private Method method;
    private String uri;
    private PrincipalIdentifierToken principalIdentifierToken;

    public ExtensionRestRequest(Method method, String uri, PrincipalIdentifierToken requesterIdentifier) {
        this.method = method;
        this.uri = uri;
        this.principalIdentifierToken = requesterIdentifier;
    }

    // TODO Check if this constructor is more useful, If so what happens when request object is null
    protected ExtensionRestRequest(RestExecuteOnExtensionRequest request) {
        this.method = request.getMethod();
        this.uri = request.getUri();
        this.principalIdentifierToken = request.getRequestIssuerIdentity();
    }

    public ExtensionRestRequest(StreamInput in) throws IOException {
        super(in);
        method = in.readEnum(Method.class);
        uri = in.readString();
        principalIdentifierToken = in.readNamedWriteable(PrincipalIdentifierToken.class);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeEnum(method);
        out.writeString(uri);
        out.writeNamedWriteable(principalIdentifierToken);
    }

    public Method method() {
        return method;
    }

    public String uri() {
        return uri;
    }

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
