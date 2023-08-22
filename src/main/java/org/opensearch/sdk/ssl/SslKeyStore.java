/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.ssl;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import java.security.cert.X509Certificate;

/**
 * Interface for an SslKeyStore
 */
public interface SslKeyStore {

    /**
     *
     * @return SSLEngine
     * @throws SSLException
     */
    public SSLEngine createServerTransportSSLEngine() throws SSLException;

    /**
     *
     * @param peerHost Peer Hostname
     * @param peerPort Peer Port
     * @return SSLEngine
     * @throws SSLException
     */
    public SSLEngine createClientTransportSSLEngine(String peerHost, int peerPort) throws SSLException;

    /**
     *
     * @param cert Public Certificate
     * @return Returns the san from the certificate
     */
    public String getSubjectAlternativeNames(X509Certificate cert);

    /**
     * Initialize SSL config
     */
    public void initTransportSSLConfig();
}
