/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.test.OpenSearchTestCase;

import java.io.File;
import java.io.IOException;

public class TestExtensionSettings extends OpenSearchTestCase {
    private static final String EXTENSION_DESCRIPTOR_CLASSPATH = "/extension.yml";
    private static final String EXTENSION_DESCRIPTOR_FILEPATH = "src/test/resources" + EXTENSION_DESCRIPTOR_CLASSPATH;
    private ExtensionSettings extensionSettings;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        File file = new File(EXTENSION_DESCRIPTOR_FILEPATH);
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        extensionSettings = objectMapper.readValue(file, ExtensionSettings.class);
    }

    @Test
    public void testSettingsStrings() {
        assertEquals("sample-extension", extensionSettings.getExtensionName());
        assertEquals("127.0.0.1", extensionSettings.getHostAddress());
        assertEquals("4532", extensionSettings.getHostPort());
        assertEquals("127.0.0.1", extensionSettings.getOpensearchAddress());
        assertEquals("9200", extensionSettings.getOpensearchPort());
    }

    @Test
    public void testConstructorWithArgs() {
        ExtensionSettings settings = new ExtensionSettings("foo", "bar", "baz", "os", "port");
        assertEquals("foo", settings.getExtensionName());
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
        assertNull(ExtensionSettings.readSettingsFromYaml("this/path/does/not/exist"));
        assertNull(ExtensionSettings.readSettingsFromYaml(EXTENSION_DESCRIPTOR_CLASSPATH + "filedoesnotexist"));
    }
}
