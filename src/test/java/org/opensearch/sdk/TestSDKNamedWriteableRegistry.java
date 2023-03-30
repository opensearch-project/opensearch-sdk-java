/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.common.io.stream.NamedWriteable;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.settings.Settings;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class TestSDKNamedWriteableRegistry extends OpenSearchTestCase {
    private TestSDKNamedWriteableRegistry.ExampleRunnerForTest runner;

    private static class DummyNamedWriteable implements NamedWriteable {
        DummyNamedWriteable(StreamInput in) {}

        @Override
        public String getWriteableName() {
            return "test";
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {}
    }

    private static class Example implements NamedWriteable {
        public static final String NAME = "Example";
        public static final NamedWriteableRegistry.Entry WRITEABLE_REGISTRY = new NamedWriteableRegistry.Entry(
            NamedWriteable.class,
            NAME,
            DummyNamedWriteable::new
        );

        private final String name;

        public Example(String name) {
            this.name = name;
        }

        @Override
        public String getWriteableName() {
            return name;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {}
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

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> registry.getReader(TestSDKNamedWriteableRegistry.Example.class, TestSDKNamedWriteableRegistry.Example.NAME)
        );
        assertEquals("Unknown NamedWriteable category [" + TestSDKNamedWriteableRegistry.Example.class.getName() + "]", ex.getMessage());
    }

    @Test
    public void testCustomNamedWriteableRegistry() throws IOException {
        // Update the runner before testing
        runner.updateNamedWriteableRegistry();
        NamedWriteableRegistry registry = runner.sdkNamedWriteableRegistry.getRegistry();

        Writeable.Reader<? extends NamedWriteable> reader = registry.getReader(NamedWriteable.class, Example.NAME);
        assertNotNull(reader.read(null));
    }
}
