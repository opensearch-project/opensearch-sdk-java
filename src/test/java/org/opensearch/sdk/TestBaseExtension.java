/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.opensearch.test.OpenSearchTestCase;

/*
 * Most of the code in BaseExtension is tested by HelloWorld extension tests.
 * This class tests code paths which are not tested.
 */
public class TestBaseExtension extends OpenSearchTestCase {

    private static final String UNPARSEABLE_EXTENSION_CONFIG = "/bad-extension.yml";
    private static final String EXTENSION_DESCRIPTOR_FILEPATH = "src/test/resources" + UNPARSEABLE_EXTENSION_CONFIG;

    public class TestExtension extends BaseExtension {

        public TestExtension(String path) {
            super(path);
        }
    }

    @Test
    public void testBaseExtensionWithNullPath() {
        // When a null path is passed, reading from YAML will fail.
        assertThrows(IllegalArgumentException.class, () -> { new TestExtension(null); });
    }

    @Test
    public void testBaseExtensionWithBadConfig() {
        // When a bad extensions.yml config is passed, expect failing initialization.
        assertThrows(IllegalArgumentException.class, () -> { new TestExtension(EXTENSION_DESCRIPTOR_FILEPATH); });
    }

    @Test
    public void testGetExtensionsRunner() throws IOException {
        ExtensionsRunnerForTest runner = new ExtensionsRunnerForTest();
        assertEquals(runner, ((BaseExtension) runner.getExtension()).extensionsRunner());
    }
}
