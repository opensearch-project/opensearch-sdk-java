/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import java.util.List;

import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestStatus;

/**
 * A subclass of {@link BytesRestResponse} which processes the consumed parameters into a custom header.
 */
public class ExtensionRestResponse extends BytesRestResponse {

    /**
     * Key passed in {@link BytesRestResponse} headers to identify parameters consumed by the handler. For internal use.
     */
    static final String CONSUMED_PARAMS_KEY = "extension.consumed.parameters";

    /**
     * Creates a new response based on {@link XContentBuilder}.
     *
     * @param status  The REST status.
     * @param builder  The builder for the response.
     * @param consumedParams  Parameters consumed by the handler.
     */
    public ExtensionRestResponse(RestStatus status, XContentBuilder builder, List<String> consumedParams) {
        super(status, builder);
        addConsumedParamHeader(consumedParams);
    }

    /**
     * Creates a new plain text response.
     *
     * @param status  The REST status.
     * @param content  A plain text response string.
     * @param consumedParams  Parameters consumed by the handler.
     */
    public ExtensionRestResponse(RestStatus status, String content, List<String> consumedParams) {
        super(status, content);
        addConsumedParamHeader(consumedParams);
    }

    /**
     * Creates a new plain text response.
     *
     * @param status  The REST status.
     * @param contentType  The content type of the response string.
     * @param content  A response string.
     * @param consumedParams  Parameters consumed by the handler.
     */
    public ExtensionRestResponse(RestStatus status, String contentType, String content, List<String> consumedParams) {
        super(status, contentType, content);
        addConsumedParamHeader(consumedParams);
    }

    /**
     * Creates a binary response.
     *
     * @param status  The REST status.
     * @param contentType  The content type of the response bytes.
     * @param content  Response bytes.
     * @param consumedParams  Parameters consumed by the handler.
     */
    public ExtensionRestResponse(RestStatus status, String contentType, byte[] content, List<String> consumedParams) {
        super(status, contentType, content);
        addConsumedParamHeader(consumedParams);
    }

    /**
     * Creates a binary response.
     *
     * @param status  The REST status.
     * @param contentType  The content type of the response bytes.
     * @param content  Response bytes.
     * @param consumedParams  Parameters consumed by the handler.
     */
    public ExtensionRestResponse(RestStatus status, String contentType, BytesReference content, List<String> consumedParams) {
        super(status, contentType, content);
        addConsumedParamHeader(consumedParams);
    }

    private void addConsumedParamHeader(List<String> consumedParams) {
        consumedParams.stream().forEach(p -> addHeader(CONSUMED_PARAMS_KEY, p));
    }
}
