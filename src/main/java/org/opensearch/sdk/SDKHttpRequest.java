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
    * Instantiates this class with a copy of request
    *
    * @param request The request
    */
    public SDKHttpRequest(ExtensionRestRequest request) {
        this.method = request.method();
        this.uri = request.uri();
        this.content = request.content();
        this.headers = request.headers();
        this.httpVersion = request.httpVersion();
    }

    /**
    * This method returns request method
    * @return A method of request
    */
    @Override
    public RestRequest.Method method() {
        return method;
    }

    /**
    * This method returns request uri
    * @return URI of request
    */
    @Override
    public String uri() {
        return uri;
    }

    /**
    * This method returns request content
    * @return content of request
    */
    @Override
    public BytesReference content() {
        return content;
    }

    /**
    * This method returns request headers
    * @return map containing headers
    */
    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
    * This method returns request cookies
    * @return list containing cookies
    */
    @Override
    public List<String> strictCookies() {
        return null;
    }

    /**
    * This method returns request HTTP protocol version
    * @return version of request HTTP protocol
    */
    @Override
    public HttpVersion protocolVersion() {
        return httpVersion;
    }

    /**
    * This method removes headers from request
    * @param s header
    */
    @Override
    public HttpRequest removeHeader(String s) {
        return null;
    }

    /**
    * This method creates response
    * @param restStatus response status
    * @param bytesReference content
    */
    @Override
    public HttpResponse createResponse(RestStatus restStatus, BytesReference bytesReference) {
        return null;
    }

    /**
    * This method returns inbound exception
    * @return thrown exception
    */
    @Override
    public Exception getInboundException() {
        return null;
    }

    /**
    * Release any resources associated with this request.
    */
    @Override
    public void release() {

    }

    /**
    * If this instances uses any pooled resources, creates a copy of this instance that does not use any pooled resources and releases
    * any resources associated with this instance. If the instance does not use any shared resources, returns itself.
    * @return a safe unpooled http request
    */
    @Override
    public HttpRequest releaseAndCopy() {
        return null;
    }
}
