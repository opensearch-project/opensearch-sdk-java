/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.opensearch.ingest.Processor;

import java.util.Collections;
import java.util.Map;

/**
 * An extension point for {@link Extension} implementations to add custom ingest processors
 */

public interface IngestExtension {

    /**
     * Returns additional ingest processor types added by this plugin.
     *
     * @param parameters instance of the Parameters class.
     * @return The key of the returned {@link Map} is the unique name for the processor which is
     * specified in pipeline configurations, and the value is a {@link Processor.Factory}
     * to create the processor from a given pipeline configuration.
     */
    default Map<String, Processor.Factory> getProcessors(Processor.Parameters parameters) {
        return Collections.emptyMap();
    }
}
