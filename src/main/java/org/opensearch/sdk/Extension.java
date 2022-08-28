/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * This interface defines methods which an extension must provide. Extensions
 * will instantiate a class implementing this interface and pass it to the
 * {@link ExtensionsRunner} constructor to initialize.
 */
public interface Extension {

    /**
     * Gets the {@link ExtensionSettings} of this extension.
     *
     * @return the extension settings.
     */
    ExtensionSettings getExtensionSettings();

    /**
     * Gets a list of {@link ExtensionAction} implementations this extension handles.
     *
     * @return a list of Actions this extension handles.
     */
    List<ExtensionAction> getExtensionActions();

    /**
     * Helper method to read extension settings from a YAML file.
     *
     * @param extensionSettingsPath
     *            The path (relative to the classpath) of the extension settings
     *            file.
     * @return A settings file encapsulating the extension host and port if the file
     *         exists, null otherwise.
     * @throws IOException
     *             if there is an error reading the file.
     */
    static ExtensionSettings readSettingsFromYaml(String extensionSettingsPath) throws IOException {
        URL resource = Extension.class.getResource(extensionSettingsPath);
        if (resource == null) {
            return null;
        }
        File file = new File(resource.getPath());
        if (!file.exists()) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(file, ExtensionSettings.class);
    }
}
