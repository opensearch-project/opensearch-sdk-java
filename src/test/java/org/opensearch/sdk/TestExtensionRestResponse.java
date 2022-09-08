package org.opensearch.sdk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.test.OpenSearchTestCase;

import static org.opensearch.rest.BytesRestResponse.TEXT_CONTENT_TYPE;
import static org.opensearch.rest.RestStatus.ACCEPTED;
import static org.opensearch.rest.RestStatus.OK;
import static org.opensearch.sdk.ExtensionRestResponse.CONSUMED_PARAMS_KEY;

public class TestExtensionRestResponse extends OpenSearchTestCase {

    private static final String OCTET_CONTENT_TYPE = "application/octet-stream";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    private String testText;
    private byte[] testBytes;
    private List<String> testConsumedParams;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        testText = "plain text";
        testBytes = new byte[] { 1, 2 };
        testConsumedParams = List.of("foo", "bar");
    }

    @Test
    public void testConstructorWithBuilder() throws IOException {
        XContentBuilder builder = XContentBuilder.builder(XContentType.JSON.xContent());
        builder.startObject();
        builder.field("status", ACCEPTED);
        builder.endObject();
        ExtensionRestResponse response = new ExtensionRestResponse(OK, builder, testConsumedParams);

        assertEquals(OK, response.status());
        assertEquals(JSON_CONTENT_TYPE, response.contentType());
        assertEquals("{\"status\":\"ACCEPTED\"}", response.content().utf8ToString());
        List<String> consumedParams = response.getHeaders().get(CONSUMED_PARAMS_KEY);
        for (String param : consumedParams) {
            assertTrue(testConsumedParams.contains(param));
        }
    }

    @Test
    public void testConstructorWithPlainText() {
        ExtensionRestResponse response = new ExtensionRestResponse(OK, testText, testConsumedParams);

        assertEquals(OK, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        assertEquals(testText, response.content().utf8ToString());
        List<String> consumedParams = response.getHeaders().get(CONSUMED_PARAMS_KEY);
        for (String param : consumedParams) {
            assertTrue(testConsumedParams.contains(param));
        }
    }

    @Test
    public void testConstructorWithText() {
        ExtensionRestResponse response = new ExtensionRestResponse(OK, TEXT_CONTENT_TYPE, testText, testConsumedParams);

        assertEquals(OK, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        assertEquals(testText, response.content().utf8ToString());

        List<String> consumedParams = response.getHeaders().get(CONSUMED_PARAMS_KEY);
        for (String param : consumedParams) {
            assertTrue(testConsumedParams.contains(param));
        }
    }

    @Test
    public void testConstructorWithByteArray() {
        ExtensionRestResponse response = new ExtensionRestResponse(OK, OCTET_CONTENT_TYPE, testBytes, testConsumedParams);

        assertEquals(OK, response.status());
        assertEquals(OCTET_CONTENT_TYPE, response.contentType());
        assertArrayEquals(testBytes, BytesReference.toBytes(response.content()));
        List<String> consumedParams = response.getHeaders().get(CONSUMED_PARAMS_KEY);
        for (String param : consumedParams) {
            assertTrue(testConsumedParams.contains(param));
        }
    }

    @Test
    public void testConstructorWithBytesReference() {
        ExtensionRestResponse response = new ExtensionRestResponse(
            OK,
            OCTET_CONTENT_TYPE,
            BytesReference.fromByteBuffer(ByteBuffer.wrap(testBytes, 0, 2)),
            testConsumedParams
        );

        assertEquals(OK, response.status());
        assertEquals(OCTET_CONTENT_TYPE, response.contentType());
        assertArrayEquals(testBytes, BytesReference.toBytes(response.content()));
        List<String> consumedParams = response.getHeaders().get(CONSUMED_PARAMS_KEY);
        for (String param : consumedParams) {
            assertTrue(testConsumedParams.contains(param));
        }
    }
}
