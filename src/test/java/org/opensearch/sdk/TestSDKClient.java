/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.junit.jupiter.api.Test;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.Alias;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.test.OpenSearchTestCase;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class TestSDKClient extends OpenSearchTestCase {
    SDKClient sdkClient = new SDKClient();

    @Test
    public void testCreateJavaClient() throws Exception {

        OpenSearchClient testClient = sdkClient.initializeJavaClient("localhost", 9200);
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

        sdkClient.doCloseJavaClient();
    }

    @Test
    public void testCreateHighLevelRestClient() throws Exception {
        RestHighLevelClient testClient = sdkClient.initializeRestClient("localhost", 9200);

        // Using the package name here as Java uses package name if the filename from different packages are same
        org.opensearch.client.indices.CreateIndexRequest createIndexRequest = new org.opensearch.client.indices.CreateIndexRequest(
            "my-index"
        );

        assertThrows(ConnectException.class, () -> testClient.indices().create(createIndexRequest, RequestOptions.DEFAULT));

        sdkClient.doCloseHighLevelClient();

    }

}
