/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.api;

import org.opensearch.common.settings.Settings;
import org.opensearch.sdk.Extension;

/**
 * An extension point for {@link Extension}s that can be reloaded. There is no
 * clear definition about what reloading a extension actually means. When a extension
 * is reloaded it might rebuild any internal members. Extensions usually implement
 * this interface in order to reread the values of {@code SecureSetting}s and
 * then rebuild any dependent internal members.
 */
public interface ReloadableExtension {

    /**
     * Called to trigger the rebuilt of the extension's internal members. The reload
     * operation <b>is required to have been completed</b> when the method returns.
     * Strictly speaking, the <code>settings</code> argument should not be accessed
     * outside of this method's call stack, as any values stored in the node's
     * keystore (see {@code SecureSetting}) will not otherwise be retrievable. The
     * setting values do not follow dynamic updates, i.e. the values are identical
     * to the ones during the initial extension loading, barring the keystore file on
     * disk changes. Any failure during the operation should be signaled by raising
     * an exception, but the extension should otherwise continue to function
     * unperturbed.
     *
     * @param settings
     *            Settings used while reloading the extension. All values are
     *            retrievable, including the values stored in the node's keystore.
     *            The setting values are the initial ones, from when the node has be
     *            started, i.e. they don't follow dynamic updates.
     * @throws Exception
     *             if the operation failed. The extension should continue to operate as
     *             if the offending call didn't happen.
     */

    void reload(Settings settings) throws Exception;
}
