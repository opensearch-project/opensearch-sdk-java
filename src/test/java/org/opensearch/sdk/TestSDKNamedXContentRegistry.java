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

import org.opensearch.core.ParseField;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.xcontent.NamedXContentRegistry.Entry;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParseException;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.test.OpenSearchTestCase;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestSDKNamedXContentRegistry extends OpenSearchTestCase {
    private ExampleRunnerForTest runner;

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

    private static class ExampleRunnerForTest extends ExtensionsRunnerForTest {

        private List<Entry> testNamedXContent = Collections.emptyList();
        private final SDKNamedXContentRegistry sdkNamedXContentRegistry = new SDKNamedXContentRegistry(this);

        public ExampleRunnerForTest() throws IOException {
            super();
        }

        @Override
        public Settings getEnvironmentSettings() {
            return Settings.EMPTY;
        }

        @Override
        public List<NamedXContentRegistry.Entry> getCustomNamedXContent() {
            return this.testNamedXContent;
        }

        @Override
        public void updateNamedXContentRegistry() {
            this.testNamedXContent = Collections.singletonList(Example.XCONTENT_REGISTRY);
            this.sdkNamedXContentRegistry.updateNamedXContentRegistry(this);
        }
    }

    @Override
    @BeforeEach
    public void setUp() throws IOException {
        this.runner = new ExampleRunnerForTest();
    }

    @Test
    public void tesDefaultNamedXContentRegistryParse() throws IOException {
        // Tests parsing the default namedXContent with nothing from extension
        BytesReference bytes = BytesReference.bytes(
            JsonXContent.contentBuilder().startObject().field(Example.NAME_FIELD, Example.NAME).endObject()
        );

        NamedXContentRegistry registry = runner.sdkNamedXContentRegistry.getRegistry();
        XContentParser parser = XContentType.JSON.xContent()
            .createParser(registry, LoggingDeprecationHandler.INSTANCE, bytes.streamInput());
        ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.nextToken(), parser);

        XContentParseException ex = assertThrows(
            XContentParseException.class,
            () -> registry.parseNamedObject(Example.class, Example.NAME, parser, null)
        );
        assertEquals("unknown named object category [" + Example.class.getName() + "]", ex.getMessage());
    }

    @Test
    public void testCustomNamedXContentRegistryParse() throws IOException {
        // Tests parsing the custom namedXContent from extension
        BytesReference bytes = BytesReference.bytes(
            JsonXContent.contentBuilder().startObject().field(Example.NAME_FIELD, Example.NAME).endObject()
        );

        // Update the runner before testing
        runner.updateNamedXContentRegistry();
        NamedXContentRegistry registry = runner.sdkNamedXContentRegistry.getRegistry();
        XContentParser parser = XContentType.JSON.xContent()
            .createParser(registry, LoggingDeprecationHandler.INSTANCE, bytes.streamInput());
        ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.nextToken(), parser);

        Example example = registry.parseNamedObject(Example.class, Example.NAME, parser, null);
        assertEquals(Example.NAME, example.getName());
    }

    @Test
    public void testEmptyRegistry() {
        assertEquals(NamedXContentRegistry.EMPTY, SDKNamedXContentRegistry.EMPTY.getRegistry());
    }
}
