/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

public class TestExtensionSettings extends OpenSearchTestCase {
    private static final String EXTENSION_DESCRIPTOR_CLASSPATH = "/extension.yml";
    private ExtensionSettings extensionSettings;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        extensionSettings = ExtensionSettings.readSettingsFromYaml(EXTENSION_DESCRIPTOR_CLASSPATH);
    }

    @Test
    public void testSettingsStrings() {
        assertEquals("sample-extension", extensionSettings.getExtensionName());
        assertEquals("127.0.0.1", extensionSettings.getHostAddress());
        assertEquals("4532", extensionSettings.getHostPort());
        assertEquals("127.0.0.1", extensionSettings.getOpensearchAddress());
        assertEquals("9200", extensionSettings.getOpensearchPort());

        extensionSettings.setOpensearchAddress("localhost");
        assertEquals("localhost", extensionSettings.getOpensearchAddress());
        extensionSettings.setOpensearchPort("9300");
        assertEquals("9300", extensionSettings.getOpensearchPort());
    }

    @Test
    public void testConstructorWithArgs() {
        ExtensionSettings settings = new ExtensionSettings("foo", "abbr", "bar", "baz", "os", "port");
        assertEquals("foo", settings.getExtensionName());
        assertEquals("abbr", settings.getShortExtensionName());
        assertEquals("bar", settings.getHostAddress());
        assertEquals("baz", settings.getHostPort());
        assertEquals("os", settings.getOpensearchAddress());
        assertEquals("port", settings.getOpensearchPort());
    }

    @Test
    public void testReadSettingsFromYaml() throws IOException {
        ExtensionSettings settings = ExtensionSettings.readSettingsFromYaml(EXTENSION_DESCRIPTOR_CLASSPATH);
        assertNotNull(settings);
        assertEquals(extensionSettings.getExtensionName(), settings.getExtensionName());
        assertEquals(extensionSettings.getHostAddress(), settings.getHostAddress());
        assertEquals(extensionSettings.getHostPort(), settings.getHostPort());
        assertEquals(extensionSettings.getOpensearchAddress(), settings.getOpensearchAddress());
        assertEquals(extensionSettings.getOpensearchPort(), settings.getOpensearchPort());
        expectThrows(IOException.class, () -> ExtensionSettings.readSettingsFromYaml("this/path/does/not/exist"));
        expectThrows(IOException.class, () -> ExtensionSettings.readSettingsFromYaml(EXTENSION_DESCRIPTOR_CLASSPATH + "filedoesnotexist"));
    }
}
