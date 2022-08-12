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
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.TransportException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestSDKClient {
    private OpenSearchTransport transport = new FailingTransport();

    @Test
    public void testCreateIndexException() throws Exception {
        OpenSearchClient client = new OpenSearchClient(transport);

        //tag::builders
        assertThrows(TransportException.class, () -> client.indices().create(
                new CreateIndexRequest.Builder()
                        .index("my-index")
                        .aliases("foo",
                                new Alias.Builder().isWriteIndex(true).build()
                        )
                        .build()
        ));
        //end::builders
    }

}
