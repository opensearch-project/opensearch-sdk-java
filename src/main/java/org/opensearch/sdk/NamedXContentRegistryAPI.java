/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.ParseField;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.NamedXContentRegistryParseRequest;
import org.opensearch.common.xcontent.NamedXContentRegistryResponse;
import org.opensearch.extensions.ExtensionBooleanResponse;
import org.opensearch.extensions.OpenSearchRequest;

/**
 * API used to handle named xcontent registry requests from OpenSearch
 */
public class NamedXContentRegistryAPI {
    private final Logger logger = LogManager.getLogger(NamedXContentRegistryAPI.class);
    private List<NamedXContentRegistry.Entry> namedXContent;
    private final NamedXContentRegistry namedXContentRegistry;

    /**
     * Constructor for NamedXContentRegistryAPI. Creates a NamedXContentRegistry for this extension
     */
    public NamedXContentRegistryAPI() {
        this.namedXContent = getNamedXContents();
        this.namedXContentRegistry = new NamedXContentRegistry(namedXContent);
    }

    /**
     * Constructor for NamedXContentRegistryAPI. Creates and populates a NamedXContentRegistry with the given entries for this extension
     *
     * @param extensionNamedXContent List of NamedXContentRegistry.Entry to be registered
     */
    public NamedXContentRegistryAPI(List<NamedXContentRegistry.Entry> extensionNamedXContent) {
        this.namedXContent = extensionNamedXContent;
        this.namedXContentRegistry = new NamedXContentRegistry(namedXContent);
    }

    /**
     * Getter for NamedXContentRegistry
     *
     * @return The NamedXContentRegistry of this API
     */
    public NamedXContentRegistry getRegistry() {
        return this.namedXContentRegistry;
    }

    /**
     * Current placeholder for extension point override getNamedXContent(), will invoke extension point override here
     *
     * @return A list of NamedXContentRegistry entries that the extension wants to register within OpenSearch
     */
    private List<NamedXContentRegistry.Entry> getNamedXContents() {
        List<NamedXContentRegistry.Entry> namedXContents = new ArrayList<>();
        return namedXContents;
    }

    /**
     * Handles a request from OpenSearch for named xcontent registry entries.
     *
     * @param request  The OpenSearch request to handle.
     * @return A response with a list of xcontent ParseFields and fully qualified category class names to register within OpenSearch
     */
    public NamedXContentRegistryResponse handleNamedXContentRegistryRequest(OpenSearchRequest request) {
        logger.info("Registering Named XContent Registry Request recieved from OpenSearch.");
        // Iterate through Extensions's named xcontent and add to extension entries
        Map<ParseField, Class> extensionEntries = new HashMap<>();
        for (NamedXContentRegistry.Entry entry : this.namedXContent) {
            extensionEntries.put(entry.name, entry.categoryClass);
        }
        NamedXContentRegistryResponse namedXContentRegistryResponse = new NamedXContentRegistryResponse(extensionEntries);
        return namedXContentRegistryResponse;
    }

    /**
     * Handles a request from OpenSearch to parse a named xcontent
     *
     * @param request  The request to handle.
     * @throws IOException if InputStream generated from the byte array is unsuccessfully closed
     * @return A response acknowledging the request to parse has executed successfully
     */
    public ExtensionBooleanResponse handleNamedXContentRegistryParseRequest(NamedXContentRegistryParseRequest request) throws IOException {
        logger.info("Registering Named Writeable Registry Parse request from OpenSearch");
        boolean status = false;

        Class cateogoryClass = request.getCategoryClass();
        String context = request.getContext();

        // TODO : invoke parse on context

        ExtensionBooleanResponse namedXContentRegistryParseResponse = new ExtensionBooleanResponse(status);
        return namedXContentRegistryParseResponse;
    }

}
