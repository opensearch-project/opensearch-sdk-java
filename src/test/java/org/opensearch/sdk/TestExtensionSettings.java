package org.opensearch.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.test.OpenSearchTestCase;

public class TestExtensionSettings extends OpenSearchTestCase {

    private ExtensionSettings extensionSettings;

    @BeforeEach
    public void setUp() {
        extensionSettings = new ExtensionSettings();
    }

    @Test
    public void testExtensionName() {
        extensionSettings.setExtensionname("test-extension");
        assertEquals(extensionSettings.getExtensionname(), "test-extension");
    }

    @Test
    public void testHostAddress() {
        extensionSettings.setHostaddress("127.0.0.1");
        assertEquals(extensionSettings.getHostaddress(), "127.0.0.1");
    }

    @Test
    public void testHostPort() {
        extensionSettings.setHostport("1234");
        assertEquals(extensionSettings.getHostport(), "1234");
    }
}
