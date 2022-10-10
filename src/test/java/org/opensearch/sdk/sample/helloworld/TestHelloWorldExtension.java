/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.helloworld;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.rest.RestHandler.Route;
import org.opensearch.sdk.Extension;
import org.opensearch.sdk.ExtensionRestHandler;
import org.opensearch.sdk.ExtensionSettings;
import org.opensearch.test.OpenSearchTestCase;

public class TestHelloWorldExtension extends OpenSearchTestCase {

    private Extension extension;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.extension = new HelloWorldExtension();
    }

    @Test
    public void testExtensionSettings() {
        // This effectively tests the Extension interface helper method
        ExtensionSettings extensionSettings = extension.getExtensionSettings();
        ExtensionSettings expected = new ExtensionSettings("hello-world", "127.0.0.1", "4532");
        assertEquals(expected.getExtensionName(), extensionSettings.getExtensionName());
        assertEquals(expected.getHostAddress(), extensionSettings.getHostAddress());
        assertEquals(expected.getHostPort(), extensionSettings.getHostPort());
    }

    @Test
    public void testExtensionRestHandlers() {
        List<ExtensionRestHandler> extensionRestHandlers = extension.getExtensionRestHandlers();
        assertEquals(1, extensionRestHandlers.size());
        List<Route> routes = extensionRestHandlers.get(0).routes();
        assertEquals(4, routes.size());
    }

}
