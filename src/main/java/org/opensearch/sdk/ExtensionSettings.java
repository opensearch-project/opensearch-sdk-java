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
    private String routeNamePrefix;
    private Map<String, String> securitySettings;

    /**
     * A set of keys for security settings related to SSL transport, keystore and truststore files, and hostname verification.
     * These settings are used in OpenSearch to secure network communication and ensure data privacy.
     */
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
     *
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
        this.securitySettings = Map.of();
    }

    /**
     * Instantiate this class using the specified parameters.
     *
     * @param extensionName  The extension name. Provided to OpenSearch as a response to initialization query. Must match the defined extension name in OpenSearch.
     * @param hostAddress  The IP Address to bind this extension to.
     * @param hostPort  The port to bind this extension to.
     * @param opensearchAddress  The IP Address on which OpenSearch is running.
     * @param opensearchPort  The port on which OpenSearch is running.
     * @param routeNamePrefix The prefix to be pre-pended to a NamedRoute being registered
     * @param securitySettings A generic map of any settings set in the config file that are not default setting keys
     */
    public ExtensionSettings(
        String extensionName,
        String hostAddress,
        String hostPort,
        String opensearchAddress,
        String opensearchPort,
        String routeNamePrefix,
        Map<String, String> securitySettings
    ) {
        this(extensionName, hostAddress, hostPort, opensearchAddress, opensearchPort);
        this.routeNamePrefix = routeNamePrefix;
        this.securitySettings = securitySettings;
    }

    /**
     * Returns the name of the extension.
     * @return A string representing the name of the extension.
     */
    public String getExtensionName() {
        return extensionName;
    }

    /**
     * Returns the host address associated with this object.
     * @return The host address as a string.
     */
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * Returns the host and port number of the server.
     * @return A string representation of the host and port number of the server.
     */
    public String getHostPort() {
        return hostPort;
    }

    /**
     * Sets the OpenSearch server address to use for connecting to OpenSearch.
     * @param opensearchAddress the URL or IP address of the OpenSearch server.
     */
    public void setOpensearchAddress(String opensearchAddress) {
        this.opensearchAddress = opensearchAddress;
    }

    /**
     * Returns the address of the OpenSearch instance being used by the application.
     * @return The address of the OpenSearch instance.
     */
    public String getOpensearchAddress() {
        return opensearchAddress;
    }

    /**
     * Sets the OpenSearch port number to be used for communication.
     * @param opensearchPort The port number to set.
     */
    public void setOpensearchPort(String opensearchPort) {
        this.opensearchPort = opensearchPort;
    }

    /**
     * Returns the OpenSearch port number.
     * @return The OpenSearch port number as a String.
     */
    public String getOpensearchPort() {
        return opensearchPort;
    }

    /**
     * Returns the route Prefix for all routes registered by this extension
     * @return A string representing the route prefix of this extension
     */
    public String getRoutePrefix() {
        return routeNamePrefix;
    }

    /**
     * Returns the security settings as a map of key-value pairs.
     * The keys represent the different security settings available, and the values represent the values set for each key.
     * @return A map of security settings and their values.
     */
    public Map<String, String> getSecuritySettings() {
        return securitySettings;
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

            // Making routeNamePrefix an optional setting
            String routeNamePrefix = null;
            if (extensionMap.containsKey("routeNamePrefix")) {
                routeNamePrefix = extensionMap.get("routeNamePrefix").toString();
            }
            return new ExtensionSettings(
                extensionMap.get("extensionName").toString(),
                extensionMap.get("hostAddress").toString(),
                extensionMap.get("hostPort").toString(),
                extensionMap.get("opensearchAddress").toString(),
                extensionMap.get("opensearchPort").toString(),
                routeNamePrefix,
                securitySettings
            );
        } catch (URISyntaxException e) {
            throw new IOException("Error reading from extension.yml");
        }
    }
}
