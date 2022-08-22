package org.opensearch.sdk;

/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.jsonb.JsonbJsonpMapper;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.Endpoint;
import org.opensearch.client.transport.TransportException;
import org.opensearch.client.transport.TransportOptions;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A transport implementation that always fails. Used for simple doc sections where we just want to check compilation.
 */
public class FailingTransport implements OpenSearchTransport {

    private JsonpMapper mapper = new JsonbJsonpMapper();

    private TransportOptions options = new TransportOptions() {
        @Override
        public Collection<Map.Entry<String, String>> headers() {
            return Collections.emptyList();
        }

        @Override
        public Map<String, String> queryParameters() {
            return Collections.emptyMap();
        }

        @Override
        public Function<List<String>, Boolean> onWarnings() {
            return null;
        }

        @Override
        public Builder toBuilder() {
            return null;
        }
    };

    @Override
    public <RequestT, ResponseT, ErrorT> ResponseT performRequest(
        RequestT request,
        Endpoint<RequestT, ResponseT, ErrorT> endpoint,
        @Nullable TransportOptions options
    ) throws IOException {
        throw new TransportException("Not implemented");
    }

    @Override
    public <RequestT, ResponseT, ErrorT> CompletableFuture<ResponseT> performRequestAsync(
        RequestT request,
        Endpoint<RequestT, ResponseT, ErrorT> endpoint,
        @Nullable TransportOptions options
    ) {
        CompletableFuture<ResponseT> future = new CompletableFuture<>();
        future.completeExceptionally(new TransportException("Not implemented"));
        return future;
    }

    @Override
    public JsonpMapper jsonpMapper() {
        return mapper;
    }

    @Override
    public TransportOptions options() {
        return options;
    }

    @Override
    public void close() throws IOException {}
}
