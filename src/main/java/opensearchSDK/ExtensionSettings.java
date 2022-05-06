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

package opensearchSDK;

public class ExtensionSettings {

    private String extensionname;
    private String hostaddress;
    private String hostport;
    // Change the location to extension.yml file of the extension
    public static final String EXTENSION_DESCRIPTOR = "src/test/resources/extension.yml";

    public String getExtensionname() {
        return extensionname;
    }

    public void setExtensionname(String extensionname) {
        this.extensionname = extensionname;
    }

    public String getHostaddress() {
        return hostaddress;
    }

    public void getHostaddress(String hostaddress) {
        this.hostaddress = hostaddress;
    }

    public String getHostport() {
        return hostport;
    }

    public void setHostport(String hostport) {
        this.hostport = hostport;
    }

    @Override
    public String toString() {
        return "\nnodename: " + extensionname + "\nhostaddress: " + hostaddress + "\nhostPort: " + hostport + "\n";
    }

}
