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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.opensearch.common.ParseField;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.NamedXContentRegistryParseRequest;
import org.opensearch.common.xcontent.NamedXContentRegistryResponse;
import org.opensearch.extensions.ExtensionBooleanResponse;
import org.opensearch.extensions.OpenSearchRequest;
import org.opensearch.extensions.ExtensionsOrchestrator.OpenSearchRequestType;
import org.opensearch.test.OpenSearchTestCase;

public class TestNamedXContentRegistryAPI extends OpenSearchTestCase {

    private Object testObject;
    private ParseField testParseField;
    private List<NamedXContentRegistry.Entry> namedXContents;
    private NamedXContentRegistryAPI namedXContentRegistryAPI;

    @BeforeEach
    public void setUp() throws Exception {
        this.testObject = new Object();
        this.testParseField = new ParseField("testObject");
        this.namedXContents = Collections.singletonList(new NamedXContentRegistry.Entry(Object.class, testParseField, p -> testObject));
        this.namedXContentRegistryAPI = new NamedXContentRegistryAPI(namedXContents);
    }

    @Test
    public void testNamedXContentRegistryCreation() {
        assert (namedXContentRegistryAPI.getRegistry() instanceof NamedXContentRegistry);
    }

    @Test
    public void testNamedXContentRegistryRequest() throws UnknownHostException {
        OpenSearchRequest request = new OpenSearchRequest(OpenSearchRequestType.REQUEST_OPENSEARCH_NAMED_XCONTENT_REGISTRY);
        NamedXContentRegistryResponse response = namedXContentRegistryAPI.handleNamedXContentRegistryRequest(request);

        assertEquals(1, response.getRegistry().size());
        assertEquals(Object.class, response.getRegistry().get(testParseField));
    }

    @Test
    public void testNamedXContentParseRequest() throws UnknownHostException, IOException {

        Class categoryClass = Object.class;
        String context = "test context";
        NamedXContentRegistryParseRequest request = new NamedXContentRegistryParseRequest(categoryClass, context);
        ExtensionBooleanResponse response = namedXContentRegistryAPI.handleNamedXContentRegistryParseRequest(request);

        // verify that XcontentParser creation was successful
        assertEquals(response.getStatus(), true);
    }
}
