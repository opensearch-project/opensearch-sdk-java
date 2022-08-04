/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates the API of an Extension.
 */
public class ExtensionApi {

    private List<String> api = new ArrayList<>();

    /**
     * Placeholder field. Change the location to extension_api.yml file of the extension.
     */
    public static final String EXTENSION_API_DESCRIPTOR = "src/test/resources/extension_api.yml";

    /**
     * Jackson requires a default constructor.
     */
    private ExtensionApi() {
        super();
    }

    public List<String> getApi() {
        return new ArrayList<>(api);
    }

    @Override
    public String toString() {
        return "ExtensionApi{api=" + api + "}";
    }
}
