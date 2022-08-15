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
public class ExtensionRestApi {

    private List<String> restApi = new ArrayList<>();

    /**
     * Placeholder field. Change the location to extension_api.yml file of the extension.
     */
    public static final String EXTENSION_REST_API_DESCRIPTOR = "src/test/resources/extension_rest_api.yml";

    /**
     * Jackson requires a default constructor.
     */
    private ExtensionRestApi() {
        super();
    }

    public List<String> getRestApi() {
        return new ArrayList<>(restApi);
    }

    @Override
    public String toString() {
        return "ExtensionRestApi{restApi=" + restApi + "}";
    }
}
