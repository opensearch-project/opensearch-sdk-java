/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.opensearch.common.bytes.BytesReference;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.http.HttpRequest;
import org.opensearch.http.HttpResponse;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestStatus;

import java.util.List;
import java.util.Map;

public class SDKHttpRequest implements HttpRequest {
    private final RestRequest.Method method;
    private final BytesReference content;

    public SDKHttpRequest(ExtensionRestRequest request) {
        this.method = request.method();
        this.content = request.content();
    }

    @Override
    public RestRequest.Method method() {
        return method;
    }

    @Override
    public String uri() {
        return null;
    }

    @Override
    public BytesReference content() {
        return content;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return null;
    }

    @Override
    public List<String> strictCookies() {
        return null;
    }

    @Override
    public HttpVersion protocolVersion() {
        return null;
    }

    @Override
    public HttpRequest removeHeader(String s) {
        return null;
    }

    @Override
    public HttpResponse createResponse(RestStatus restStatus, BytesReference bytesReference) {
        return null;
    }

    @Override
    public Exception getInboundException() {
        return null;
    }

    @Override
    public void release() {

    }

    @Override
    public HttpRequest releaseAndCopy() {
        return null;
    }
}
