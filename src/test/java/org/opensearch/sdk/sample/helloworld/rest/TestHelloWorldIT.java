package org.opensearch.sdk.sample.helloworld.rest;

import org.junit.jupiter.api.Test;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.test.rest.OpenSearchRestTestCase;

import java.io.IOException;

public class TestHelloWorldIT extends OpenSearchRestTestCase {

    @Test
    public void testInitHelloWorld() throws IOException {
        Request request = new Request("PUT", "/_extensions/initialize");
        String entity = "{\"name\": \"hello-world\",\n" +
                "  \"uniqueId\": \"hello-world\",\n" +
                "  \"hostAddress\": \"127.0.0.1\",\n" +
                "  \"port\": \"4500\",\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"opensearchVersion\": \"2.9.0\",\n" +
                "  \"minimumCompatibleVersion\": \"2.9.0\",}";
        request.setJsonEntity(entity);
        Response response = client().performRequest(request);
        assertEquals("HTTP/1.1 200 OK", response.getStatusLine());
    }
}
