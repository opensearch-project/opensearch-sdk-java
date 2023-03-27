/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.sdk.ssl;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

public interface SslKeyStore {


    public SSLEngine createServerTransportSSLEngine() throws SSLException;

    public SSLEngine createClientTransportSSLEngine(String peerHost, int peerPort) throws SSLException;

    public String getTransportServerProviderName();
    public String getTransportClientProviderName();
    public String getSubjectAlternativeNames(X509Certificate cert);

    public void initTransportSSLConfig();
    public X509Certificate[] getTransportCerts();
}

