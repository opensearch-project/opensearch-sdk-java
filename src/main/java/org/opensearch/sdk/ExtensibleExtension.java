/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import java.util.List;

/**
 * An extension point for {@link Extension} implementations to be themselves extensible.
 *
 * This class provides a callback for extensible Extensions to be informed of other Extensions
 * which extend them.
 *
 * @opensearch.api
 */
public interface ExtensibleExtension {

    /**
     * Extension point for external Extensions to be extendable
     *
     * @opensearch.api
     */
    interface ExtensionLoader {
        /**
         * Load extensions of the type from all extending Extensions. The concrete extensions must have either a no-arg constructor
         * or a single-arg constructor accepting the specific Extension class.
         * @param extensionPointType the extension point type
         * @param <T> extension point type
         * @return all implementing extensions.
         */
        <T> List<T> loadExtensions(Class<T> extensionPointType);
    }

    /**
     * Allow this Extension to load extensions from other Extensions.
     *
     * This method is called once only, after initializing this Extension and all Extensions extending this Extension. It is called before
     * any other methods on this Extension instance are called.
     */
    default void loadExtensions(ExtensionLoader loader) {}
}
