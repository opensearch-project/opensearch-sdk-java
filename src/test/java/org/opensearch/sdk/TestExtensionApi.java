package org.opensearch.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.test.OpenSearchTestCase;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class TestExtensionApi extends OpenSearchTestCase {

    private ExtensionApi extensionApi;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        File file = new File(ExtensionApi.EXTENSION_API_DESCRIPTOR);
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        extensionApi = objectMapper.readValue(file, ExtensionApi.class);
    }

    @Test
    public void testExtensionApi() {
        List<String> apiList = extensionApi.getApi();
        List<String> expected = Arrays.asList("GET /api_1", "PUT /api_2", "POST /api_3");
        assertEquals(expected.size(), apiList.size());
        assertTrue(apiList.containsAll(expected));
        assertTrue(expected.containsAll(apiList));
    }
}
