/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.ssl;

/**
 * Return codes for SSLConnectionTestUtil.testConnection()
 */
public enum SSLConnectionTestResult {
    /**
     * OpenSearch Ping to the server failed.
     */
    OPENSEARCH_PING_FAILED,
    /**
     * Server does not support SSL.
     */
    SSL_NOT_AVAILABLE,
    /**
     * Server supports SSL.
     */
    SSL_AVAILABLE
}
