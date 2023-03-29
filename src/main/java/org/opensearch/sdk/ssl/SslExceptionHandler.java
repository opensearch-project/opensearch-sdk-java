/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.ssl;

import org.opensearch.rest.RestRequest;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportRequest;

public interface SslExceptionHandler {

    default void logError(Throwable t, RestRequest request, int type) {
        //no-op
    }

    default void logError(Throwable t, boolean isRest) {
        //no-op
    }

    default void logError(Throwable t, final TransportRequest request, String action, Task task, int type) {
        //no-op
    }
}

