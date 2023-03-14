/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.opensearch.index.IndexSettings;
import org.opensearch.index.codec.CodecServiceFactory;
import org.opensearch.index.engine.EngineFactory;
import org.opensearch.index.seqno.RetentionLeases;
import org.opensearch.index.translog.TranslogDeletionPolicy;
import org.opensearch.index.translog.TranslogDeletionPolicyFactory;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A Extension that provides alternative engine implementations.
 *
 */
public interface EngineExtension {

    /**
     * When an index is created this method is invoked for each engine Extension. Engine Extensions can inspect the index settings to determine
     * whether or not to provide an engine factory for the given index. A Extension that is not overriding the default engine should return
     * {@link Optional#empty()}. If multiple Extensions return an engine factory for a given index the index will not be created and an
     * {@link IllegalStateException} will be thrown during index creation.
     *
     * @param indexSettings the index settings to inspect
     * @return an optional engine factory
     */
    default Optional<EngineFactory> getEngineFactory(IndexSettings indexSettings) {
        return Optional.empty();
    }

    /**
     * EXPERT:
     * When an index is created this method is invoked for each engine Extension. Engine Extensions can inspect the index settings
     * to determine if a custom {@link CodecServiceFactory} should be provided for the given index. A Extension that is not overriding
     * the {@link CodecServiceFactory} through the Extension can ignore this method and the default Codec specified in the
     * {@link IndexSettings} will be used.
     *
     * @param indexSettings the index settings to inspect
     * @return an optional engine factory
     */
    default Optional<CodecServiceFactory> getCustomCodecServiceFactory(IndexSettings indexSettings) {
        return Optional.empty();
    }

    /**
     * When an index is created this method is invoked for each engine Extension. Engine Extensions that need to provide a
     * custom {@link TranslogDeletionPolicy} can override this method to return a function that takes the {@link IndexSettings}
     * and a {@link Supplier} for {@link RetentionLeases} and returns a custom {@link TranslogDeletionPolicy}.
     *
     * Only one of the installed Engine Extensions can override this otherwise {@link IllegalStateException} will be thrown.
     *
     * @return a function that returns an instance of {@link TranslogDeletionPolicy}
     */
    default Optional<TranslogDeletionPolicyFactory> getCustomTranslogDeletionPolicyFactory() {
        return Optional.empty();
    }
}
