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

public class TestExtensionRestApi extends OpenSearchTestCase {

    private ExtensionRestApi extensionRestApi;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        File file = new File(ExtensionRestApi.class.getResource(ExtensionRestApi.EXTENSION_REST_API_DESCRIPTOR).getPath());
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        extensionRestApi = objectMapper.readValue(file, ExtensionRestApi.class);
    }

    @Test
    public void testExtensionApi() {
        List<String> apiList = extensionRestApi.getRestApi();
        List<String> expected = Arrays.asList("GET /api_1", "PUT /api_2", "POST /api_3");
        assertEquals(expected.size(), apiList.size());
        assertTrue(apiList.containsAll(expected));
        assertTrue(expected.containsAll(apiList));
    }

    @Test
    public void testReadFromYaml() throws IOException {
        List<String> apiList = ExtensionRestApi.readFromYaml().getRestApi();
        List<String> expected = Arrays.asList("GET /api_1", "PUT /api_2", "POST /api_3");
        assertEquals(expected.size(), apiList.size());
        assertTrue(apiList.containsAll(expected));
        assertTrue(expected.containsAll(apiList));
    }
}
