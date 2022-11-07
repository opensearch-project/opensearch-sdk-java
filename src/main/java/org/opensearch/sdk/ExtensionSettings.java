/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

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
     * Jackson requires a default constructor.
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
}
