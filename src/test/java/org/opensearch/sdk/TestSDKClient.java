/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.Alias;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.test.OpenSearchTestCase;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class TestSDKClient extends OpenSearchTestCase {
    SDKClient sdkClient = new SDKClient();

    @Test
    public void testCreateClient() throws Exception {

        OpenSearchClient testClient = sdkClient.initializeClient("localhost", 9200);
        assertInstanceOf(OpenSearchClient.class, testClient);

        assertThrows(
            ConnectException.class,
            () -> testClient.indices()
                .create(
                    new CreateIndexRequest.Builder().index("my-index")
                        .aliases("foo", new Alias.Builder().isWriteIndex(true).build())
                        .build()
                )
        );

        sdkClient.doCloseRestClient();
    }

}
