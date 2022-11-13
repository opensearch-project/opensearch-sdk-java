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

public class TestExtensionSettings extends OpenSearchTestCase {
    private static final String EXTENSION_DESCRIPTOR = "src/test/resources/extension.yml";
    private ExtensionSettings extensionSettings;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        File file = new File(EXTENSION_DESCRIPTOR);
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        extensionSettings = objectMapper.readValue(file, ExtensionSettings.class);
    }

    @Test
    public void testExtensionName() {
        assertEquals("sample-extension", extensionSettings.getExtensionName());
    }

    @Test
    public void testHostAddress() {
        assertEquals("127.0.0.1", extensionSettings.getHostAddress());
    }

    @Test
    public void testHostPort() {
        assertEquals("4532", extensionSettings.getHostPort());
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
}
