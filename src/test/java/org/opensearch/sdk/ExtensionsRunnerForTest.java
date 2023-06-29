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

/**
 * An Extension Runner for testing using test settings.
 */
public class ExtensionsRunnerForTest extends ExtensionsRunner {

    public static final String NODE_NAME = "sample-extension";
    public static final String NODE_HOST = "127.0.0.1";
    public static final String NODE_PORT = "4532";

    /**
     * Instantiates a new Extensions Runner using test settings.
     *
     * @throws IOException if the runner failed to read settings or API.
     */
    public ExtensionsRunnerForTest() throws IOException {
        super(new BaseExtension(new ExtensionSettings(NODE_NAME, NODE_HOST, NODE_PORT, "127.0.0.1", "9200")) {
        });
    }

}
