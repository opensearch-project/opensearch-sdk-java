/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.ssl.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Class that reads a private key in .pem format
 */
public final class PemKeyReader {
    protected static final Logger log = LogManager.getLogger(PemKeyReader.class);

    /**
     *
     * @param file Path to certificate file
     * @return Returns an X509Certificate object
     * @throws Exception
     */
    public static X509Certificate loadCertificateFromFile(String file) throws Exception {
        if (file == null) {
            return null;
        }

        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        try (FileInputStream is = new FileInputStream(file)) {
            return (X509Certificate) fact.generateCertificate(is);
        }
    }

    private PemKeyReader() {}
}
