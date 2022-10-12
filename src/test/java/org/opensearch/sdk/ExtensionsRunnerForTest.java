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
        super(new BaseExtension() {
            @Override
            public ExtensionSettings getExtensionSettings() {
                return new ExtensionSettings("sample-extension", "127.0.0.1", "4532");
            }

            @Override
            public List<ExtensionRestHandler> getExtensionRestHandlers() {
                return Collections.emptyList();
            }
        });

    }

}
