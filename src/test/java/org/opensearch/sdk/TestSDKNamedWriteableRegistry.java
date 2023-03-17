package org.opensearch.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.common.io.stream.NamedWriteable;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.xcontent.*;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class TestSDKNamedWriteableRegistry extends OpenSearchTestCase {
    private TestSDKNamedWriteableRegistry.ExampleRunnerForTest runner;

    private static class Example implements NamedWriteable {
        public static final String NAME = "Example";
        public static final NamedWriteableRegistry.Entry WRITEABLE_REGISTRY = new NamedWriteableRegistry.Entry(
                TestSDKNamedWriteableRegistry.Example.class,
                NAME,
                null
        );

        private final String name;

        public Example(String name) {
            this.name = name;
        }


        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestSDKNamedWriteableRegistry.Example that = (TestSDKNamedWriteableRegistry.Example) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String getWriteableName() {
            return null;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
        }
    }

    private static class ExampleRunnerForTest extends ExtensionsRunnerForTest {

        private List<NamedWriteableRegistry.Entry> testNamedWriteables = Collections.emptyList();
        private final SDKNamedWriteableRegistry sdkNamedWriteableRegistry = new SDKNamedWriteableRegistry(this);

        public ExampleRunnerForTest() throws IOException {
            super();
        }

        @Override
        public Settings getEnvironmentSettings() {
            return Settings.EMPTY;
        }

        @Override
        public List<NamedWriteableRegistry.Entry> getCustomNamedWriteables() {
            return this.testNamedWriteables;
        }

        @Override
        public void updateNamedWriteableRegistry() {
            this.testNamedWriteables = Collections.singletonList(Example.WRITEABLE_REGISTRY);
            this.sdkNamedWriteableRegistry.updateNamedWriteableRegistry(this);
        }
    }

    @Override
    @BeforeEach
    public void setUp() throws IOException {
        this.runner = new TestSDKNamedWriteableRegistry.ExampleRunnerForTest();
    }

    @Test
    public void testDefaultNamedWriteableRegistry() throws IOException {
        NamedWriteableRegistry registry = runner.sdkNamedWriteableRegistry.getRegistry();

        XContentParseException ex = assertThrows(
                XContentParseException.class,
                () -> registry.getReader(TestSDKNamedWriteableRegistry.Example.class, TestSDKNamedWriteableRegistry.Example.NAME)
        );
        assertEquals("unknown named object category [" + TestSDKNamedWriteableRegistry.Example.class.getName() + "]", ex.getMessage());
    }

}
