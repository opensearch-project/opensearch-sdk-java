/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.rest;

import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.http.HttpChannel;
import org.opensearch.http.HttpRequest;
import org.opensearch.rest.RestRequest;

import java.util.List;
import java.util.Map;

/**
 * This class helps to get instance of RestRequest
 */
public class SDKRestRequest extends RestRequest {
    /**
    * Instantiates this class with request's params
    *
    * @param xContentRegistry The request's content registry
    * @param params The request's params
    * @param path The request's path
    * @param headers The request's headers
    * @param httpRequest The request's httpRequest
    * @param httpChannel The request's http channel
    */
    public SDKRestRequest(
        NamedXContentRegistry xContentRegistry,
        Map<String, String> params,
        String path,
        Map<String, List<String>> headers,
        HttpRequest httpRequest,
        HttpChannel httpChannel
    ) {
        super(xContentRegistry, params, path, headers, httpRequest, httpChannel);
    }
}
