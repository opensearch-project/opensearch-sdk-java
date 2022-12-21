/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.junit.jupiter.api.Test;
import org.opensearch.extensions.rest.ExtensionRestRequest;
import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Collections;

public class TestExtensionRestHandler extends OpenSearchTestCase {
    private class NoOpExtensionRestHandler implements ExtensionRestHandler {

        @Override
        public ExtensionRestResponse handleRequest(ExtensionRestRequest request) {
            return null;
        }
    }

    @Test
    public void testHandlerDefaultRoutes() {
        NoOpExtensionRestHandler handler = new NoOpExtensionRestHandler();
        assertEquals(handler.routes(), Collections.emptyList());
    }
}
