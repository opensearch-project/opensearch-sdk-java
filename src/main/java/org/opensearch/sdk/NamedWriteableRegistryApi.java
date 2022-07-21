/*
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

import org.opensearch.extensions.OpenSearchRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.io.stream.InputStreamStreamInput;
import org.opensearch.common.io.stream.NamedWriteable;
import org.opensearch.common.io.stream.NamedWriteableAwareStreamInput;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.NamedWriteableRegistryParseRequest;
import org.opensearch.extensions.BooleanResponse;
import org.opensearch.common.io.stream.NamedWriteableRegistryResponse;
import org.opensearch.common.io.stream.StreamInput;

/**
 * Api used to handle named writeable registry requests from OpenSearch
 */
public class NamedWriteableRegistryApi {
    private final Logger logger = LogManager.getLogger(ExtensionsRunner.class);
    private List<NamedWriteableRegistry.Entry> namedWriteables;
    private final NamedWriteableRegistry namedWriteableRegistry;

    public NamedWriteableRegistryApi() {
        this.namedWriteables = getNamedWriteables();
        this.namedWriteableRegistry = new NamedWriteableRegistry(namedWriteables);
    }

    /**
     * Current placeholder for extension point override getNamedWriteables(), will invoke extension point override here
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
    public NamedWriteableRegistryResponse handleNamedWriteableRegistryRequest(OpenSearchRequest request) {
        logger.info("Registering Named Writeable Registry Request recieved from OpenSearch.");
        // Iterate through Extensions's named writeables and add to extension entries
        Map<String, Class> extensionEntries = new HashMap<>();
        for (NamedWriteableRegistry.Entry entry : this.namedWriteables) {
            extensionEntries.put(entry.name, entry.categoryClass);
        }
        NamedWriteableRegistryResponse namedWriteableRegistryResponse = new NamedWriteableRegistryResponse(extensionEntries);
        return namedWriteableRegistryResponse;
    }

    /**
     * Handles a request from OpenSearch to parse a named writeable from a byte array generated from a {@link StreamInput} object.
     * Works as {@see org.opensearch.common.io.stream.StreamInput#readNamedWriteable(Class)}
     *
     * @param request  The request to handle.
     * @return A response acknowledging the request to parse has executed successfully
     */
    public <C extends NamedWriteable> BooleanResponse handleNamedWriteableRegistryParseRequest(NamedWriteableRegistryParseRequest request)
        throws IOException {

        logger.info("Registering Named Writeable Registry Parse request from OpenSearch");
        boolean status = false;

        // Extract data from request and procress fully qualified category class name into class instance
        Class<C> categoryClass = (Class<C>) request.getCategoryClass();
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
                    C c = streamInput.readNamedWriteable(categoryClass);
                    status = true;
                } catch (UnsupportedOperationException e) {
                    logger.info("Failed to parse named writeable", e);
                }

            }
        }

        BooleanResponse namedWriteableRegistryParseResponse = new BooleanResponse(status);
        return namedWriteableRegistryParseResponse;
    }

}
