/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * This class encapsulates the settings for an Extension.
 */
public class ExtensionSettings {

    private String extensionName;
    private String hostAddress;
    private String hostPort;
    private String opensearchAddress;
    private String opensearchPort;

    /**
     * Jackson requires a no-arg constructor.
     */
    @SuppressWarnings("unused")
    private ExtensionSettings() {
        super();
    }

    /**
     * Instantiate this class using the specified parameters.
     *
     * @param extensionName  The extension name. Provided to OpenSearch as a response to initialization query. Must match the defined extension name in OpenSearch.
     * @param hostAddress  The IP Address to bind this extension to.
     * @param hostPort  The port to bind this extension to.
     * @param opensearchAddress  The IP Address on which OpenSearch is running.
     * @param opensearchPort  The port on which OpenSearch is running.
     */
    public ExtensionSettings(String extensionName, String hostAddress, String hostPort, String opensearchAddress, String opensearchPort) {
        super();
        this.extensionName = extensionName;
        this.hostAddress = hostAddress;
        this.hostPort = hostPort;
        this.opensearchAddress = opensearchAddress;
        this.opensearchPort = opensearchPort;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public String getHostPort() {
        return hostPort;
    }

    public String getOpensearchAddress() {
        return opensearchAddress;
    }

    public String getOpensearchPort() {
        return opensearchPort;
    }

    @Override
    public String toString() {
        return "ExtensionSettings{extensionName="
            + extensionName
            + ", hostAddress="
            + hostAddress
            + ", hostPort="
            + hostPort
            + ", opensearchAddress="
            + opensearchAddress
            + ", opensearchPort="
            + opensearchPort
            + "}";
    }

    /**
     * Helper method to read extension settings from a YAML file.
     *
     * @param extensionSettingsPath The path (relative to the classpath) of the extension settings file.
     * @return A settings file encapsulating the extension host and port if the file exists, null otherwise.
     * @throws IOException if there is an error reading the file.
     */
    public static ExtensionSettings readSettingsFromYaml(String extensionSettingsPath) throws IOException {
        URL resource = Extension.class.getResource(extensionSettingsPath);
        if (resource == null) {
            return null;
        }
        File file = new File(resource.getPath());
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(file, ExtensionSettings.class);
    }
}
