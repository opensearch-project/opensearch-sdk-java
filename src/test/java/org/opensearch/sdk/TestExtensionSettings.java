package org.opensearch.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.test.OpenSearchTestCase;

import java.io.File;
import java.io.IOException;

public class TestExtensionSettings extends OpenSearchTestCase {

    private ExtensionSettings extensionSettings;

    @BeforeEach
    public void setUp() throws IOException {
        File file = new File(ExtensionSettings.EXTENSION_DESCRIPTOR);
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        extensionSettings = objectMapper.readValue(file, ExtensionSettings.class);
    }

    @Test
    public void testExtensionName() {
        assertEquals(extensionSettings.getExtensionName(), "extension");
    }

    @Test
    public void testHostAddress() {
        assertEquals(extensionSettings.getHostAddress(), "127.0.0.1");
    }

    @Test
    public void testHostPort() {
        assertEquals(extensionSettings.getHostPort(), "4532");
    }
}
