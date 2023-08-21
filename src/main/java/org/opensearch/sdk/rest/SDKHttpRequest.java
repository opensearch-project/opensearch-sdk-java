/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.rest;

import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.http.HttpRequest;
import org.opensearch.http.HttpResponse;
import org.opensearch.rest.RestRequest;

import java.util.List;
import java.util.Map;

/**
 * This class helps to get instance of HttpRequest
 */
public class SDKHttpRequest implements HttpRequest {
    private final RestRequest.Method method;
    private final String uri;
    private final BytesReference content;
    private final Map<String, List<String>> headers;
    private final HttpVersion httpVersion;

    /**
    * Instantiates this class with a copy of {@link ExtensionRestRequest}
    *
    * @param request The request
    */
    public SDKHttpRequest(ExtensionRestRequest request) {
        this.method = request.method();
        this.uri = request.uri();
        this.content = request.content();
        this.headers = request.headers();
        this.httpVersion = request.protocolVersion();
    }

    @Override
    public RestRequest.Method method() {
        return method;
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public BytesReference content() {
        return content;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
    * Not implemented. Does nothing.
    * @return null
    */
    @Override
    public List<String> strictCookies() {
        return null;
    }

    @Override
    public HttpVersion protocolVersion() {
        return httpVersion;
    }

    /**
    * Not implemented. Does nothing.
    * @return null
    */
    @Override
    public HttpRequest removeHeader(String s) {
        return null;
    }

    /**
    * Not implemented. Does nothing.
    * @param restStatus response status
    * @param bytesReference content
    * @return null
    */
    @Override
    public HttpResponse createResponse(RestStatus restStatus, BytesReference bytesReference) {
        return null;
    }

    /**
    * Not implemented. Does nothing.
    * @return null
    */
    @Override
    public Exception getInboundException() {
        return null;
    }

    /**
    * Not implemented. Does nothing.
    */
    @Override
    public void release() {

    }

    /**
    * Not implemented. Does nothing.
    * @return null
    */
    @Override
    public HttpRequest releaseAndCopy() {
        return null;
    }
}
