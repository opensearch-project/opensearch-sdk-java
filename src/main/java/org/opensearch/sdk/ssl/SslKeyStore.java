/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.ssl;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

public interface SslKeyStore {

    public SSLEngine createServerTransportSSLEngine() throws SSLException;

    public SSLEngine createClientTransportSSLEngine(String peerHost, int peerPort) throws SSLException;

    public String getSubjectAlternativeNames(X509Certificate cert);

    public void initTransportSSLConfig();
}
