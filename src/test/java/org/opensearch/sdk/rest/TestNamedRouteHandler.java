/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.rest;

import org.opensearch.core.rest.RestStatus;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.NamedRoute;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Collections;

import static org.opensearch.rest.RestRequest.Method.GET;

public class TestNamedRouteHandler extends OpenSearchTestCase {
    public void testUnnamedRouteHandler() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new NamedRoute.Builder().method(GET)
                .path("/foo/bar")
                .handler(req -> new ExtensionRestResponse(req, RestStatus.OK, "content"))
                .uniqueName("")
                .legacyActionNames(Collections.emptySet())
                .build()
        );
    }

    public void testNamedRouteHandler() {
        NamedRoute nr = new NamedRoute.Builder().method(GET)
            .path("/foo/bar")
            .handler(req -> new ExtensionRestResponse(req, RestStatus.OK, "content"))
            .uniqueName("")
            .legacyActionNames(Collections.emptySet())
            .build();

        assertEquals("foo", nr.name());
        assertNotNull(nr.handler());
    }
}
