/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.rest;

import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestStatus;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Collections;
import java.util.Set;

public class TestNamedRouteHandler extends OpenSearchTestCase {
    public void testUnnamedRouteHandler() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new NamedRouteHandler(
                RestRequest.Method.GET,
                "/foo/bar",
                req -> new ExtensionRestResponse(req, RestStatus.OK, "content"),
                "",
                Collections.emptySet()
            )
        );
    }

    public void testNamedRouteHandler() {
        NamedRouteHandler rh = new NamedRouteHandler(
            RestRequest.Method.GET,
            "/foo/bar",
            req -> new ExtensionRestResponse(req, RestStatus.OK, "content"),
            "foo",
            Set.of("cluster:admin/opensearch/ab/foo")
        );

        assertEquals("foo", rh.name());
    }
}
