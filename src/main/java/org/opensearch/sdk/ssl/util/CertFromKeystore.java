/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.ssl.util;

import org.opensearch.OpenSearchException;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class that contains methods for reading in a certificate from a keystore
 */
public class CertFromKeystore {

    private final KeystoreProps keystoreProps;
    private final String serverKeystoreAlias;
    private final String clientKeystoreAlias;

    private PrivateKey serverKey;
    private X509Certificate[] serverCert;
    private final char[] serverKeyPassword;

    private PrivateKey clientKey;
    private X509Certificate[] clientCert;
    private final char[] clientKeyPassword;

    private X509Certificate[] loadedCerts;

    /**
     *
     * @param keystoreProps Keystore Props
     * @param keystoreAlias Keystore Alias
     * @param keyPassword Keystore Password
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     * @throws UnrecoverableKeyException
     */
    public CertFromKeystore(KeystoreProps keystoreProps, String keystoreAlias, String keyPassword) throws CertificateException,
        NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {
        this.keystoreProps = keystoreProps;
        final KeyStore ks = keystoreProps.loadKeystore();

        this.serverKeystoreAlias = keystoreAlias;
        this.serverKeyPassword = Utils.toCharArray(keyPassword);
        this.serverCert = SSLCertificateHelper.exportServerCertChain(ks, serverKeystoreAlias);
        this.serverKey = SSLCertificateHelper.exportDecryptedKey(ks, serverKeystoreAlias, this.serverKeyPassword);

        this.clientKeystoreAlias = keystoreAlias;
        this.clientKeyPassword = serverKeyPassword;
        this.clientCert = serverCert;
        this.clientKey = serverKey;

        this.loadedCerts = serverCert;

        validate();
    }

    /**
     *
     * @param keystoreProps Keystore Props
     * @param serverKeystoreAlias Server Keystore Alias
     * @param clientKeystoreAlias Client Keystore Alias
     * @param serverKeyPassword Server Key Password
     * @param clientKeyPassword Client Key Password
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     * @throws UnrecoverableKeyException
     */
    public CertFromKeystore(
        KeystoreProps keystoreProps,
        String serverKeystoreAlias,
        String clientKeystoreAlias,
        String serverKeyPassword,
        String clientKeyPassword
    ) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {
        this.keystoreProps = keystoreProps;
        final KeyStore ks = keystoreProps.loadKeystore();

        this.serverKeystoreAlias = serverKeystoreAlias;
        this.serverKeyPassword = Utils.toCharArray(serverKeyPassword);
        this.serverCert = SSLCertificateHelper.exportServerCertChain(ks, serverKeystoreAlias);
        this.serverKey = SSLCertificateHelper.exportDecryptedKey(ks, serverKeystoreAlias, this.serverKeyPassword);

        this.clientKeystoreAlias = clientKeystoreAlias;
        this.clientKeyPassword = Utils.toCharArray(clientKeyPassword);
        this.clientCert = SSLCertificateHelper.exportServerCertChain(ks, clientKeystoreAlias);
        this.clientKey = SSLCertificateHelper.exportDecryptedKey(ks, clientKeystoreAlias, this.clientKeyPassword);

        List<X509Certificate> allCerts = new ArrayList<>(serverCert.length + clientCert.length);
        Collections.addAll(allCerts, serverCert);
        Collections.addAll(allCerts, clientCert);
        this.loadedCerts = allCerts.toArray(new X509Certificate[allCerts.size()]);

        validate();
    }

    private void validate() {
        if (serverKey == null) {
            throw new OpenSearchException("No key found in " + keystoreProps.getFilePath() + " with alias " + serverKeystoreAlias);
        }

        if (serverCert == null || serverCert.length == 0) {
            throw new OpenSearchException("No certificates found in " + keystoreProps.getFilePath() + " with alias " + serverKeystoreAlias);
        }

        if (clientKey == null) {
            throw new OpenSearchException("No key found in " + keystoreProps.getFilePath() + " with alias " + clientKeystoreAlias);
        }

        if (clientCert == null || clientCert.length == 0) {
            throw new OpenSearchException("No certificates found in " + keystoreProps.getFilePath() + " with alias " + clientKeystoreAlias);
        }
    }

    public X509Certificate[] getCerts() {
        return loadedCerts;
    }

    public PrivateKey getServerKey() {
        return serverKey;
    }

    public X509Certificate[] getServerCert() {
        return serverCert;
    }

    public PrivateKey getClientKey() {
        return clientKey;
    }

    public X509Certificate[] getClientCert() {
        return clientCert;
    }
}
