/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.ssl.util;

/**
 * File that contains properties of a certificate file
 */
public class CertFileProps {
    private final String pemCertFilePath;
    private final String pemKeyFilePath;
    private final String trustedCasFilePath;
    private final String pemKeyPassword;

    /**
     *
     * @param pemCertFilePath Path to the certificate file in the .pem format
     * @param pemKeyFilePath Path to the private key file in the .pem format
     * @param trustedCasFilePath Path to the trusted CA file
     * @param pemKeyPassword Password for the private key file
     */
    public CertFileProps(String pemCertFilePath, String pemKeyFilePath, String trustedCasFilePath, String pemKeyPassword) {
        this.pemCertFilePath = pemCertFilePath;
        this.pemKeyFilePath = pemKeyFilePath;
        this.trustedCasFilePath = trustedCasFilePath;
        this.pemKeyPassword = pemKeyPassword;
    }

    public String getPemCertFilePath() {
        return pemCertFilePath;
    }

    public String getPemKeyFilePath() {
        return pemKeyFilePath;
    }

    public String getTrustedCasFilePath() {
        return trustedCasFilePath;
    }

    public String getPemKeyPassword() {
        return pemKeyPassword;
    }
}
