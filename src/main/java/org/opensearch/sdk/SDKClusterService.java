/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.opensearch.cluster.ClusterState;

/**
 * This class simulates methods normally called from OpenSearch ClusterService class.
 */
public class SDKClusterService {

    private final ExtensionsRunner extensionsRunner;

    /**
     * Create an instance of this object.
     *
     * @param extensionsRunner An {@link ExtensionsRunner} instance.
     */
    public SDKClusterService(ExtensionsRunner extensionsRunner) {
        this.extensionsRunner = extensionsRunner;
    }

    /**
     * Send a request to OpenSearch to retrieve the cluster state
     *
     * @return the cluster state of OpenSearch
     */
    public ClusterState state() {
        return extensionsRunner.sendClusterStateRequest(extensionsRunner.getExtensionTransportService());
    }
}
