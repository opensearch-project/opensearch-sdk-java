/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import static org.opensearch.common.xcontent.XContentParserUtils.ensureExpectedToken;

import org.opensearch.cluster.ClusterModule;
import org.opensearch.common.ParseField;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.network.NetworkModule;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.ToXContentObject;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.indices.IndicesModule;
import org.opensearch.search.SearchModule;
import org.opensearch.test.OpenSearchTestCase;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestSDKNamedXContentRegistry extends OpenSearchTestCase {
    private SDKNamedXContentRegistry extensionNamedXContentRegistry;

    private static class Example implements ToXContentObject {
        public static final String NAME = "Example";
        public static final NamedXContentRegistry.Entry XCONTENT_REGISTRY = new NamedXContentRegistry.Entry(
            Example.class,
            new ParseField(NAME),
            it -> parse(it)
        );
        public static final String NAME_FIELD = "name";

        private final String name;

        public Example(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static Example parse(XContentParser parser) throws IOException {
            String name = null;

            ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.currentToken(), parser);
            while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
                String fieldName = parser.currentName();
                parser.nextToken();

                switch (fieldName) {
                    case NAME_FIELD:
                        name = parser.text();
                        break;
                    default:
                        parser.skipChildren();
                        break;
                }
            }
            return new Example(name);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            XContentBuilder xContentBuilder = builder.startObject().field(NAME_FIELD, name);
            return xContentBuilder.endObject();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Example that = (Example) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    @Override
    @BeforeEach
    public void setUp() {
        List<NamedXContentRegistry.Entry> namedXContents = Collections.singletonList(Example.XCONTENT_REGISTRY);
        this.extensionNamedXContentRegistry = new SDKNamedXContentRegistry(Settings.EMPTY, namedXContents);
    }

    @Test
    public void testNamedXContentRegistryParse() throws IOException {
        // Tests parsing the custom namedXContent
        BytesReference bytes = BytesReference.bytes(
            JsonXContent.contentBuilder().startObject().field(Example.NAME_FIELD, Example.NAME).endObject()
        );

        NamedXContentRegistry registry = this.extensionNamedXContentRegistry.getRegistry();
        XContentParser parser = XContentType.JSON.xContent()
            .createParser(registry, LoggingDeprecationHandler.INSTANCE, bytes.streamInput());
        ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.nextToken(), parser);

        Example example = registry.parseNamedObject(Example.class, Example.NAME, parser, null);
        assertEquals(Example.NAME, example.getName());
    }

    @Test
    public void testNamedXContentRegistryExceptions() {
        // Tests that the registry includes module contents and generates conflicts when adding
        assertThrows(IllegalArgumentException.class, () -> new SDKNamedXContentRegistry(Settings.EMPTY, NetworkModule.getNamedXContents()));
        assertThrows(IllegalArgumentException.class, () -> new SDKNamedXContentRegistry(Settings.EMPTY, IndicesModule.getNamedXContents()));
        assertThrows(
            IllegalArgumentException.class,
            () -> new SDKNamedXContentRegistry(
                Settings.EMPTY,
                new SearchModule(Settings.EMPTY, Collections.emptyList()).getNamedXContents()
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new SDKNamedXContentRegistry(Settings.EMPTY, ClusterModule.getNamedXWriteables())
        );
    }
}
