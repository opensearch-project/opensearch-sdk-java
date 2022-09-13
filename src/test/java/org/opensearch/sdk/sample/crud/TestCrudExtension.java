/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.crud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.sdk.Extension;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.sdk.ExtensionSettings;
import org.opensearch.test.OpenSearchTestCase;

import java.util.List;

public class TestCrudExtension extends OpenSearchTestCase {

    private Extension extension;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.extension = new CrudExtension();
    }

    @Test
    public void testExtensionSettings() {
        // This effectively tests the Extension interface helper method
        ExtensionSettings extensionSettings = extension.getExtensionSettings();
        ExtensionSettings expected = new ExtensionSettings("sample-crud", "127.0.0.1", "7331");
        assertEquals(expected.getExtensionName(), extensionSettings.getExtensionName());
        assertEquals(expected.getHostAddress(), extensionSettings.getHostAddress());
        assertEquals(expected.getHostPort(), extensionSettings.getHostPort());
    }

    @Test
    public void testExtensionRestHandlers() {
        List<ExtensionRestHandler> extensionRestHandlers = extension.getExtensionRestHandlers();
        assertEquals(2, extensionRestHandlers.size());
        List<Route> routes = extensionRestHandlers.get(0).routes();
        assertEquals(1, routes.size());
        assertEquals(Method.PUT, routes.get(0).getMethod());
        assertEquals("/detector", routes.get(0).getPath());
    }

}
