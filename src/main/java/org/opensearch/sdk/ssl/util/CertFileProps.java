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

    /**
     * Returns the path to the certificate file in the .pem format.
     *
     * @return the path to the certificate file
     */
    public String getPemCertFilePath() {
        return pemCertFilePath;
    }

    /**
     * Returns the path to the private key file in the .pem format.
     *
     * @return the path to the private key file
     */
    public String getPemKeyFilePath() {
        return pemKeyFilePath;
    }

    /**
     * Returns the path to the trusted CA file.
     *
     * @return the path to the trusted CA file
     */
    public String getTrustedCasFilePath() {
        return trustedCasFilePath;
    }

    /**
     * Returns the password for the private key file.
     *
     * @return the password for the private key file
     */
    public String getPemKeyPassword() {
        return pemKeyPassword;
    }
}
