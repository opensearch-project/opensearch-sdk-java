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
import org.opensearch.test.OpenSearchTestCase;
import org.junit.jupiter.api.Test;

public class TestExtensionRestHandler extends OpenSearchTestCase {
    private static class NoOpExtensionRestHandler implements ExtensionRestHandler {

        @Override
        public ExtensionRestResponse handleRequest(RestRequest request) {
            return null;
        }
    }

    @Test
    public void testHandlerDefaultRoutes() {
        NoOpExtensionRestHandler handler = new NoOpExtensionRestHandler();
        assertTrue(handler.routes().isEmpty());
    }
}
