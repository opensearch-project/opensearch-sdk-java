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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

/**
 * This class encapsulates the settings for an Extension.
 */
public class ExtensionSettings {

    private String extensionName;
    private String hostAddress;
    private String hostPort;
    private String opensearchAddress;
    private String opensearchPort;

    private String shortName;
    private Map<String, String> otherSettings;

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
     * @param shortName  The shortened name for the extension
     * @param hostAddress  The IP Address to bind this extension to.
     * @param hostPort  The port to bind this extension to.
     * @param opensearchAddress  The IP Address on which OpenSearch is running.
     * @param opensearchPort  The port on which OpenSearch is running.
     * @param otherSettings A generic map of any settings set in the config file that are not default setting keys
     */
    public ExtensionSettings(String extensionName, String shortName, String hostAddress, String hostPort, String opensearchAddress, String opensearchPort, Map<String, String> otherSettings) {
        super();
        this.extensionName = extensionName;
        this.shortName = shortName;
        this.hostAddress = hostAddress;
        this.hostPort = hostPort;
        this.opensearchAddress = opensearchAddress;
        this.opensearchPort = opensearchPort;
        this.otherSettings = otherSettings;
    }

    public String getExtensionName() {
        return extensionName;
    }
    public String getShortName() {
        return shortName;
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

    public Map<String, String> getOtherSettings() {
        return otherSettings;
    }

    @Override
    public String toString() {
        return "ExtensionSettings{extensionName="
            + extensionName
            + ", shortName="
            + shortName
            + ", hostAddress="
            + hostAddress
            + ", hostPort="
            + hostPort
            + ", opensearchAddress="
            + opensearchAddress
            + ", opensearchPort="
            + opensearchPort
            + ", otherSettings="
            + otherSettings
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
        Yaml yaml = new Yaml();
        URL resource = Extension.class.getResource(extensionSettingsPath);
        if (resource == null) {
            throw new IOException("extension.yml does not exist at path [" + extensionSettingsPath + "]");
        }
        try (InputStream inputStream = Files.newInputStream(Path.of(resource.toURI()))) {
            Map<String, Object> extensionMap = yaml.load(inputStream);
            if (extensionMap == null) {
                throw new IOException("extension.yml is empty");
            }
            Map<String, String> otherSettings = new HashMap<>();
            Set<String> defaultSettings = Set.of("extensionName", "shortName", "hostAddress", "hostPort", "opensearchAddress", "opensearchPort");
            for (String settingKey : extensionMap.keySet()) {
                if (!defaultSettings.contains(settingKey)) {
                    otherSettings.put(settingKey, extensionMap.get(settingKey).toString());
                }
            }
            return new ExtensionSettings(
                extensionMap.get("extensionName").toString(),
                extensionMap.get("shortName").toString(),
                extensionMap.get("hostAddress").toString(),
                extensionMap.get("hostPort").toString(),
                extensionMap.get("opensearchAddress").toString(),
                extensionMap.get("opensearchPort").toString(),
                otherSettings
            );
        } catch (URISyntaxException e) {
            throw new IOException("Error reading from extension.yml");
        }
    }
}
