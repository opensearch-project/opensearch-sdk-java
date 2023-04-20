/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.opensearch.common.bytes.BytesArray;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.http.HttpRequest;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.test.OpenSearchTestCase;

import static java.util.Map.entry;

public class TestSDKRestRequest extends OpenSearchTestCase {
    @Test
    public void testSDKRestRequestMethods() throws IOException {
        RestRequest.Method expectedMethod = Method.GET;
        String expectedUri = "foobar?foo=bar&baz=42";
        String expectedPath = "foo";
        Map<String, String> expectedParams = Map.ofEntries(entry("foo", "bar"), entry("baz", "42"));
        Map<String, List<String>> expectedHeaders = Map.ofEntries(
            entry("Content-Type", Arrays.asList("application/json")),
            entry("foo", Arrays.asList("hello", "world"))
        );
        XContentType exptectedXContentType = XContentType.JSON;
        BytesReference expectedContent = new BytesArray("{\"foo\":\"bar\"}");

        RestRequest sdkRestRequest = createTestRestRequest(
            expectedMethod,
            expectedUri,
            expectedPath,
            expectedParams,
            expectedHeaders,
            exptectedXContentType,
            expectedContent,
            "",
            null
        );
        assertEquals(expectedMethod, sdkRestRequest.method());
        assertEquals(expectedUri, sdkRestRequest.uri());
        assertEquals(expectedPath, sdkRestRequest.path());
        assertEquals(expectedParams, sdkRestRequest.params());
        assertEquals(expectedHeaders, sdkRestRequest.getHeaders());
        assertEquals(exptectedXContentType, sdkRestRequest.getXContentType());
        assertEquals(expectedContent, sdkRestRequest.content());

        Map<String, String> source = sdkRestRequest.contentParser().mapStrings();
        assertEquals("bar", source.get("foo"));
    }

    public static RestRequest createTestRestRequest(
        final Method method,
        final String uri,
        final String path,
        final Map<String, String> params,
        final Map<String, List<String>> headers,
        final XContentType xContentType,
        final BytesReference content,
        final String principalIdentifier,
        final HttpRequest.HttpVersion httpVersion
    ) {
        // xContentType is not used. It will be parsed from headers
        ExtensionRestRequest request = new ExtensionRestRequest(
            method,
            uri,
            path,
            params,
            headers,
            xContentType,
            content,
            principalIdentifier,
            httpVersion
        );
        return new SDKRestRequest(null, request.params(), request.path(), request.headers(), new SDKHttpRequest(request), null);
    }
}
