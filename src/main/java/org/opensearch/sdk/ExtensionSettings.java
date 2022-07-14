/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.sdk;

/**
 * This class encapsulates the settings for an Extension.
 */
public class ExtensionSettings {

    private String extensionName;
    private String hostAddress;
    private String hostPort;

    /**
     * Placeholder field. Change the location to extension.yml file of the extension.
     */
    public static final String EXTENSION_DESCRIPTOR = "src/test/resources/extension.yml";

    /**
     * Jackson requires a default constructor.
     */
    private ExtensionSettings() {
        super();
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

    @Override
    public String toString() {
        return "\nnodename: " + extensionName + "\nhostaddress: " + hostAddress + "\nhostPort: " + hostPort + "\n";
    }

}
