package org.opensearch.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.test.OpenSearchTestCase;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TestExtensionApi extends OpenSearchTestCase {

    private ExtensionApi extensionApi;

    @BeforeEach
    public void setUp() throws IOException {
        File file = new File(ExtensionApi.EXTENSION_API_DESCRIPTOR);
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        extensionApi = objectMapper.readValue(file, ExtensionApi.class);
    }

    @Test
    public void testExtensionApi() {
        List<String> apiList = extensionApi.getExtensionApi();
        List<String> expected = Arrays.asList("API1", "API2", "API3");
        assertEquals(expected.size(), apiList.size());
        assertTrue(apiList.containsAll(expected));
        assertTrue(expected.containsAll(apiList));
    }
}
