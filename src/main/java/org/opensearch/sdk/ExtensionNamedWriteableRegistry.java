/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.io.stream.InputStreamStreamInput;
import org.opensearch.common.io.stream.NamedWriteable;
import org.opensearch.common.io.stream.NamedWriteableAwareStreamInput;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.NamedWriteableRegistryParseRequest;
import org.opensearch.common.io.stream.NamedWriteableRegistryResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.extensions.ExtensionBooleanResponse;
import org.opensearch.extensions.OpenSearchRequest;

/**
 * API used to handle named writeable registry requests from OpenSearch
 */
public class ExtensionNamedWriteableRegistry {
    private final Logger logger = LogManager.getLogger(ExtensionNamedWriteableRegistry.class);
    private List<NamedWriteableRegistry.Entry> namedWriteables;
    private final NamedWriteableRegistry namedWriteableRegistry;

    /**
     * Constructor for ExtensionNamedWriteableRegistry. Creates a NamedWriteableRegistry for this extension
     */
    public ExtensionNamedWriteableRegistry() {
        this.namedWriteables = getNamedWriteables();
        this.namedWriteableRegistry = new NamedWriteableRegistry(namedWriteables);
    }

    /**
     * Constructor for ExtensionNamedWriteableRegistry. Creates and populates a NamedWriteableRegistry with the given NamedWriteableRegistry entries for this extension
     *
     * @param extensionNamedWriteables List of NamedWriteableRegistry.Entry to be registered
     */
    public ExtensionNamedWriteableRegistry(List<NamedWriteableRegistry.Entry> extensionNamedWriteables) {
        this.namedWriteables = extensionNamedWriteables;
        this.namedWriteableRegistry = new NamedWriteableRegistry(namedWriteables);
    }

    /**
     * Getter for NamedWriteableRegistry
     *
     * @return The NamedWriteableRegistry of this API
     */
    public NamedWriteableRegistry getRegistry() {
        return this.namedWriteableRegistry;
    }

    /**
     * Current placeholder for extension point override getNamedWriteables(), will invoke extension point override here
     *
     * @return A list of NamedWriteableRegistry entries that the extension wants to register within OpenSearch
     */
    private List<NamedWriteableRegistry.Entry> getNamedWriteables() {
        List<NamedWriteableRegistry.Entry> namedWriteables = new ArrayList<>();
        return namedWriteables;
    }

    /**
     * Handles a request from OpenSearch for named writeable registry entries.
     *
     * @param request  The OpenSearch request to handle.
     * @return A response with a list of writeable names and fully qualified category class names to register within OpenSearch
     */
    @SuppressWarnings("unchecked")
    public NamedWriteableRegistryResponse handleNamedWriteableRegistryRequest(OpenSearchRequest request) {
        logger.info("Registering Named Writeable Registry Request recieved from OpenSearch.");
        // Iterate through Extensions's named writeables and add to extension entries
        Map<String, Class<? extends NamedWriteable>> extensionEntries = new HashMap<>();
        for (NamedWriteableRegistry.Entry entry : this.namedWriteables) {
            extensionEntries.put(entry.name, (Class<? extends NamedWriteable>) entry.categoryClass);
        }
        NamedWriteableRegistryResponse namedWriteableRegistryResponse = new NamedWriteableRegistryResponse(extensionEntries);
        return namedWriteableRegistryResponse;
    }

    /**
     * Handles a request from OpenSearch to parse a named writeable from a byte array generated from a {@link StreamInput} object.
     * Works as org.opensearch.common.io.stream.StreamInput#readNamedWriteable(Class)
     *
     * @param request  The request to handle.
     * @throws IOException if InputStream generated from the byte array is unsuccessfully closed
     * @return A response acknowledging the request to parse has executed successfully
     */
    public ExtensionBooleanResponse handleNamedWriteableRegistryParseRequest(NamedWriteableRegistryParseRequest request)
        throws IOException {

        logger.info("Registering Named Writeable Registry Parse request from OpenSearch");
        boolean status = false;

        // Extract data from request and procress fully qualified category class name into class instance
        @SuppressWarnings("unchecked")
        Class<? extends NamedWriteable> categoryClass = request.getCategoryClass();
        byte[] context = request.getContext();

        // Transform byte array context into an input stream
        try (InputStream inputStream = new ByteArrayInputStream(context, 0, context.length)) {

            // Convert input stream to stream input
            try (
                StreamInput streamInput = new NamedWriteableAwareStreamInput(
                    new InputStreamStreamInput(inputStream),
                    namedWriteableRegistry
                )
            ) {

                // Apply reader to stream input generated from the request context
                try {

                    // TODO : Determine how extensions utilize parsed object (https://github.com/opensearch-project/OpenSearch/issues/4067)
                    streamInput.readNamedWriteable(categoryClass);
                    status = true;
                } catch (UnsupportedOperationException e) {
                    logger.info("Failed to parse named writeable", e);
                }

            }
        }

        ExtensionBooleanResponse namedWriteableRegistryParseResponse = new ExtensionBooleanResponse(status);
        return namedWriteableRegistryParseResponse;
    }

}
