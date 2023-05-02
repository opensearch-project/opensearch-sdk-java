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

import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_CLIENT_PEMCERT_FILEPATH;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_CLIENT_PEMKEY_FILEPATH;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_CLIENT_PEMTRUSTEDCAS_FILEPATH;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_ENABLED;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_ENABLED_CIPHERS;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_ENABLED_PROTOCOLS;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION_RESOLVE_HOST_NAME;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_EXTENDED_KEY_USAGE_ENABLED;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_KEYSTORE_ALIAS;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_KEYSTORE_FILEPATH;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_KEYSTORE_TYPE;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_PEMCERT_FILEPATH;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_PEMKEY_FILEPATH;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_PEMTRUSTEDCAS_FILEPATH;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_SERVER_PEMCERT_FILEPATH;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_SERVER_PEMKEY_FILEPATH;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_SERVER_PEMTRUSTEDCAS_FILEPATH;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_TRUSTSTORE_ALIAS;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_TRUSTSTORE_FILEPATH;
import static org.opensearch.sdk.ssl.SSLConfigConstants.SSL_TRANSPORT_TRUSTSTORE_TYPE;

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
    public static final Set<String> SECURITY_SETTINGS_KEYS = Set.of(
        "path.home", // TODO Find the right place to put this setting
        SSL_TRANSPORT_CLIENT_PEMCERT_FILEPATH,
        SSL_TRANSPORT_CLIENT_PEMKEY_FILEPATH,
        SSL_TRANSPORT_CLIENT_PEMTRUSTEDCAS_FILEPATH,
        SSL_TRANSPORT_ENABLED,
        SSL_TRANSPORT_ENABLED_CIPHERS,
        SSL_TRANSPORT_ENABLED_PROTOCOLS,
        SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION,
        SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION_RESOLVE_HOST_NAME,
        SSL_TRANSPORT_EXTENDED_KEY_USAGE_ENABLED,
        SSL_TRANSPORT_KEYSTORE_ALIAS,
        SSL_TRANSPORT_KEYSTORE_FILEPATH,
        SSL_TRANSPORT_KEYSTORE_TYPE,
        SSL_TRANSPORT_PEMCERT_FILEPATH,
        SSL_TRANSPORT_PEMKEY_FILEPATH,
        SSL_TRANSPORT_PEMTRUSTEDCAS_FILEPATH,
        SSL_TRANSPORT_SERVER_PEMCERT_FILEPATH,
        SSL_TRANSPORT_SERVER_PEMKEY_FILEPATH,
        SSL_TRANSPORT_SERVER_PEMTRUSTEDCAS_FILEPATH,
        SSL_TRANSPORT_TRUSTSTORE_ALIAS,
        SSL_TRANSPORT_TRUSTSTORE_FILEPATH,
        SSL_TRANSPORT_TRUSTSTORE_TYPE
    );

    private Map<String, String> securitySettings;

    public static final Set<String> SECURITY_SETTINGS_KEYS = Set.of(
        "path.home", // TODO Find the right place to put this setting
        SSL_TRANSPORT_CLIENT_PEMCERT_FILEPATH,
        SSL_TRANSPORT_CLIENT_PEMKEY_FILEPATH,
        SSL_TRANSPORT_CLIENT_PEMTRUSTEDCAS_FILEPATH,
        SSL_TRANSPORT_ENABLED,
        SSL_TRANSPORT_ENABLED_CIPHERS,
        SSL_TRANSPORT_ENABLED_PROTOCOLS,
        SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION,
        SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION_RESOLVE_HOST_NAME,
        SSL_TRANSPORT_EXTENDED_KEY_USAGE_ENABLED,
        SSL_TRANSPORT_KEYSTORE_ALIAS,
        SSL_TRANSPORT_KEYSTORE_FILEPATH,
        SSL_TRANSPORT_KEYSTORE_TYPE,
        SSL_TRANSPORT_PEMCERT_FILEPATH,
        SSL_TRANSPORT_PEMKEY_FILEPATH,
        SSL_TRANSPORT_PEMTRUSTEDCAS_FILEPATH,
        SSL_TRANSPORT_SERVER_PEMCERT_FILEPATH,
        SSL_TRANSPORT_SERVER_PEMKEY_FILEPATH,
        SSL_TRANSPORT_SERVER_PEMTRUSTEDCAS_FILEPATH,
        SSL_TRANSPORT_TRUSTSTORE_ALIAS,
        SSL_TRANSPORT_TRUSTSTORE_FILEPATH,
        SSL_TRANSPORT_TRUSTSTORE_TYPE
    );

    private Map<String, String> securitySettings;

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
     */
    public ExtensionSettings(
        String extensionName,
        String shortName,
        String hostAddress,
        String hostPort,
        String opensearchAddress,
        String opensearchPort
    ) {
        super();
        this.extensionName = extensionName;
        this.shortName = shortName;
        this.hostAddress = hostAddress;
        this.hostPort = hostPort;
        this.opensearchAddress = opensearchAddress;
        this.opensearchPort = opensearchPort;
        this.securitySettings = Map.of();
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
     * @param securitySettings A generic map of any settings set in the config file that are not default setting keys
     */
    public ExtensionSettings(
        String extensionName,
        String shortName,
        String hostAddress,
        String hostPort,
        String opensearchAddress,
        String opensearchPort,
        Map<String, String> securitySettings
    ) {
        this(extensionName, shortName, hostAddress, hostPort, opensearchAddress, opensearchPort);
        this.securitySettings = securitySettings;
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

    public Map<String, String> getSecuritySettings() {
        return securitySettings;
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
            + ", securitySettings="
            + securitySettings
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
            Map<String, String> securitySettings = new HashMap<>();
            for (String settingKey : extensionMap.keySet()) {
                if (SECURITY_SETTINGS_KEYS.contains(settingKey)) {
                    securitySettings.put(settingKey, extensionMap.get(settingKey).toString());
                }
            }
            return new ExtensionSettings(
                extensionMap.get("extensionName").toString(),
                extensionMap.get("shortName").toString(),
                extensionMap.get("hostAddress").toString(),
                extensionMap.get("hostPort").toString(),
                extensionMap.get("opensearchAddress").toString(),
                extensionMap.get("opensearchPort").toString(),
                securitySettings
            );
        } catch (URISyntaxException e) {
            throw new IOException("Error reading from extension.yml");
        }
    }
}
