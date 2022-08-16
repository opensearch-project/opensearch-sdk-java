/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * This class encapsulates the API of an Extension.
 */
public class ExtensionRestApi {

    private List<String> restApi = new ArrayList<>();

    /**
     * Placeholder field. Eventually will read this from spec file
     */
    public static final String EXTENSION_REST_API_DESCRIPTOR = "/extension_rest_api.yml";

    /**
     * Jackson requires a default constructor.
     */
    private ExtensionRestApi() {
        super();
    }

    /**
     * Gets the REST API Strings.
     *
     * @return a copy of the list containing the REST API Strings
     */
    public List<String> getRestApi() {
        return new ArrayList<>(restApi);
    }

    @Override
    public String toString() {
        return "ExtensionRestApi{restApi=" + restApi + "}";
    }

    /**
     * Instantiates an instance of this class by reading from a YAML file.
     *
     * @return An instance of this class.
     * @throws IOException if there is an error reading the file.
     */
    static ExtensionRestApi readFromYaml() throws IOException {
        File file = new File(ExtensionRestApi.class.getResource(EXTENSION_REST_API_DESCRIPTOR).getPath());
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(file, ExtensionRestApi.class);
    }
}
