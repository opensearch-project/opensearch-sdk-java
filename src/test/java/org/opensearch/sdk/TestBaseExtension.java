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
import org.opensearch.test.OpenSearchTestCase;

import java.util.List;

public class TestBaseExtension extends OpenSearchTestCase {

    private static final String EXTENSION_DESCRIPTOR_CLASSPATH = "/bad-extension.yml";
    private static final String EXTENSION_DESCRIPTOR_FILEPATH = "src/test/resources" + EXTENSION_DESCRIPTOR_CLASSPATH;

    private class TestExtension extends BaseExtension {

        protected TestExtension(String path) {
            super(path);
        }

        @Override
        public List<ExtensionRestHandler> getExtensionRestHandlers() {
            return null;
        }
    }

    @Test
    public void testBaseExtensionWithNullPath() {
        // When a null path is passed, reading from YAML will fail.
        assertThrows(RuntimeException.class, () -> { TestExtension testExtension = new TestExtension(null); });
    }

    @Test
    public void testBaseExtensionWithBadConfig() {
        // When a bad extensions.yml config is passed, expect failing initialization.
        assertThrows(
            ExceptionInInitializerError.class,
            () -> { TestExtension testExtension = new TestExtension(EXTENSION_DESCRIPTOR_FILEPATH); }
        );
    }
}
