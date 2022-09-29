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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.support.TransportAction;
import org.opensearch.common.settings.Setting;

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
     * Gets a list of {@link ExtensionRestHandler} implementations this extension handles.
     *
     * @return a list of REST handlers (REST actions) this extension handles.
     */
    List<ExtensionRestHandler> getExtensionRestHandlers();

    /**
     * Gets an optional list of custom {@link Setting} for the extension to register with OpenSearch.
     *
     * @return a list of custom settings this extension uses.
     */
    default List<Setting<?>> getSettings() {
        return Collections.emptyList();
    }

    /**
     * Gets an optional list of custom {@link TransportAction} for the extension to register with OpenSearch.
     *
     * @return a list of custom transport actions this extension uses.
     */
    default Map<String, Class<? extends TransportAction<ActionRequest, ActionResponse>>> getActions() {
        return Collections.emptyMap();
    }

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
