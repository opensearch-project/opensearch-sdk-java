package org.opensearch.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.test.OpenSearchTestCase;

public class TestExtensionSettings extends OpenSearchTestCase {

    private ExtensionSettings extensionSettings;

    @BeforeEach
    public void setUp() {
        extensionSettings = new ExtensionSettings("test-extension", "127.0.0.1", "1234");
    }

    @Test
    public void testExtensionName() {
        assertEquals(extensionSettings.getExtensionname(), "test-extension");
    }

    @Test
    public void testHostAddress() {
        assertEquals(extensionSettings.getHostaddress(), "127.0.0.1");
    }

    @Test
    public void testHostPort() {
        assertEquals(extensionSettings.getHostport(), "1234");
    }
}
