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
import java.util.Collections;
import java.util.List;

/**
 * An Extension Runner for testing using test settings.
 */
public class ExtensionsRunnerForTest extends ExtensionsRunner {

    /**
     * Instantiates a new Extensions Runner using test settings.
     *
     * @throws IOException if the runner failed to read settings or API.
     */
    public ExtensionsRunnerForTest() throws IOException {
        super(new BaseExtension(new ExtensionSettings("sample-extension", "127.0.0.1", "4532", "127.0.0.1", "9200")) {

            @Override
            public List<ExtensionRestHandler> getExtensionRestHandlers() {
                return Collections.emptyList();
            }

            @Override
            public String getJobType() {
                return "sample-job-type";
            }

            @Override
            public String getJobIndex() {
                return "sample-job-index";
            }
        });

    }

}
