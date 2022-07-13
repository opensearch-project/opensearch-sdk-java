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

    private String extensionname;
    private String hostaddress;
    private String hostport;

    /**
     * Placeholder field. Change the location to extension.yml file of the extension.
     */
    public static final String EXTENSION_DESCRIPTOR = "src/test/resources/extension.yml";

    public ExtensionSettings(String extensionname, String hostaddress, String hostport) {
        this.extensionname = extensionname;
        this.hostaddress = hostaddress;
        this.hostport = hostport;
    }

    public ExtensionSettings() {}

    public String getExtensionname() {
        return extensionname;
    }

    public String getHostaddress() {
        return hostaddress;
    }

    public String getHostport() {
        return hostport;
    }

    @Override
    public String toString() {
        return "\nnodename: " + extensionname + "\nhostaddress: " + hostaddress + "\nhostPort: " + hostport + "\n";
    }

}
