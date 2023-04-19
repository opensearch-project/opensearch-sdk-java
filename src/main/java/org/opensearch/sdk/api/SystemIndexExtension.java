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
import org.opensearch.indices.SystemIndexDescriptor;

import java.util.Collection;
import java.util.Collections;

/**
 * Extension for defining system indices. Extends {@link ActionExtension} because system indices must be accessed via APIs
 * added by the extension that owns the system index, rather than standard APIs.
 *
 *
 */
public interface SystemIndexExtension extends ActionExtension {

    /**
     * Returns a {@link Collection} of {@link SystemIndexDescriptor}s that describe this plugin's system indices, including
     * name, mapping, and settings.
     * @param settings The node's settings
     * @return Descriptions of the system indices managed by this plugin.
     */
    default Collection<SystemIndexDescriptor> getSystemIndexDescriptors(Settings settings) {
        return Collections.emptyList();
    }
}
