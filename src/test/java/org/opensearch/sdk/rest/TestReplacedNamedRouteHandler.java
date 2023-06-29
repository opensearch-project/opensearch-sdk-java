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

public class TestReplacedNamedRouteHandler extends OpenSearchTestCase {
    public void testUnnamedReplacedRouteHandler() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ReplacedNamedRouteHandler(
                RestRequest.Method.GET,
                "/foo/bar",
                RestRequest.Method.GET,
                "/deprecated/foo/bar",
                req -> new ExtensionRestResponse(req, RestStatus.OK, "content"),
                "",
                Collections.emptySet()
            )
        );
    }

    public void testReplacedNamedRouteHandler() {
        ReplacedNamedRouteHandler rh = new ReplacedNamedRouteHandler(
            RestRequest.Method.GET,
            "/foo/bar",
            RestRequest.Method.GET,
            "/deprecated/foo/bar",
            req -> new ExtensionRestResponse(req, RestStatus.OK, "content"),
            "foo",
            Set.of("cluster:admin/opensearch/ab/foo")
        );

        assertEquals("foo", rh.name());
    }
}
