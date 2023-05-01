/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.ssl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * File that contains properties for a keystore
 */
public class KeystoreProps {
    private final String filePath;
    private final String type;
    private final char[] password;

    /**
     *
     * @param filePath Filepath to the keystore
     * @param type The type of keystore
     * @param password The password to the keystore
     */
    public KeystoreProps(String filePath, String type, String password) {
        this.filePath = filePath;
        this.type = type;
        this.password = Utils.toCharArray(password);
    }

    public String getFilePath() {
        return filePath;
    }

    public String getType() {
        return type;
    }

    public char[] getPassword() {
        return password;
    }

    /**
     *
     * @return Returns the keystore give the keystore props
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public KeyStore loadKeystore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final KeyStore ts = KeyStore.getInstance(type);
        ts.load(new FileInputStream(new File(filePath)), password);
        return ts;
    }
}
