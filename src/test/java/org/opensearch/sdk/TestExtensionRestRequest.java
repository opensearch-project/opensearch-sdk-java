package org.opensearch.sdk;

import org.junit.jupiter.api.Test;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamInput;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.io.stream.NamedWriteableAwareStreamInput;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.identity.ExtensionTokenProcessor;
import org.opensearch.identity.PrincipalIdentifierToken;
import org.opensearch.rest.RestRequest;
import org.opensearch.test.OpenSearchTestCase;

import java.security.Principal;

public class TestExtensionRestRequest extends OpenSearchTestCase {

    @Test
    public void testExtensionRestRequest() throws Exception {
        RestRequest.Method expectedMethod = RestRequest.Method.GET;
        String expectedUri = "/test/uri";
        String extensionUniqueId1 = "ext_1";
        Principal userPrincipal = () -> "user1";
        ExtensionTokenProcessor extensionTokenProcessor = new ExtensionTokenProcessor(extensionUniqueId1);
        PrincipalIdentifierToken expectedRequestIssuerIdentity = extensionTokenProcessor.generateToken(userPrincipal);
        NamedWriteableRegistry registry = new NamedWriteableRegistry(
            org.opensearch.common.collect.List.of(
                new NamedWriteableRegistry.Entry(
                    PrincipalIdentifierToken.class,
                    PrincipalIdentifierToken.NAME,
                    PrincipalIdentifierToken::new
                )
            )
        );

        ExtensionRestRequest request = new ExtensionRestRequest(expectedMethod, expectedUri, expectedRequestIssuerIdentity);

        assertEquals(expectedMethod, request.method());
        assertEquals(expectedUri, request.uri());
        assertEquals(expectedRequestIssuerIdentity, request.getRequestIssuerIdentity());

        try (BytesStreamOutput out = new BytesStreamOutput()) {
            request.writeTo(out);
            out.flush();
            try (BytesStreamInput in = new BytesStreamInput(BytesReference.toBytes(out.bytes()))) {
                try (NamedWriteableAwareStreamInput nameWritableAwareIn = new NamedWriteableAwareStreamInput(in, registry)) {
                    request = new ExtensionRestRequest(nameWritableAwareIn);
                }

                assertEquals(expectedMethod, request.method());
                assertEquals(expectedUri, request.uri());
                assertEquals(expectedRequestIssuerIdentity, request.getRequestIssuerIdentity());
            }
        }
    }

}
