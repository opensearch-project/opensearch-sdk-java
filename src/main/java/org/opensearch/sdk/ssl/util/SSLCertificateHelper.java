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
import org.opensearch.core.common.Strings;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Helper class with helpful methods related to SSL Certificates
 */
public class SSLCertificateHelper {

    private static final Logger log = LogManager.getLogger(SSLCertificateHelper.class);
    private static boolean stripRootFromChain = true; // TODO check

    /**
     *
     * @param ks Keystore
     * @param alias Keystore Alias
     * @return Returns an array of root certificates
     * @throws KeyStoreException
     */
    public static X509Certificate[] exportRootCertificates(final KeyStore ks, final String alias) throws KeyStoreException {
        logKeyStore(ks);

        final List<X509Certificate> trustedCerts = new ArrayList<X509Certificate>();

        if (Strings.isNullOrEmpty(alias)) {

            if (log.isDebugEnabled()) {
                log.debug("No alias given, will trust all of the certificates in the store");
            }

            final List<String> aliases = toList(ks.aliases());

            for (final String _alias : aliases) {

                if (ks.isCertificateEntry(_alias)) {
                    final X509Certificate cert = (X509Certificate) ks.getCertificate(_alias);
                    if (cert != null) {
                        trustedCerts.add(cert);
                    } else {
                        log.error("Alias {} does not exist", _alias);
                    }
                }
            }
        } else {
            if (ks.isCertificateEntry(alias)) {
                final X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                if (cert != null) {
                    trustedCerts.add(cert);
                } else {
                    log.error("Alias {} does not exist", alias);
                }
            } else {
                log.error("Alias {} does not contain a certificate entry", alias);
            }
        }

        return trustedCerts.toArray(new X509Certificate[0]);
    }

    /**
     *
     * @param ks Keystore
     * @param alias Keystore Alias
     * @return Returns the chain of certificates
     * @throws KeyStoreException
     */
    public static X509Certificate[] exportServerCertChain(final KeyStore ks, String alias) throws KeyStoreException {
        logKeyStore(ks);
        final List<String> aliases = toList(ks.aliases());

        if (Strings.isNullOrEmpty(alias)) {
            if (aliases.isEmpty()) {
                log.error("Keystore does not contain any aliases");
            } else {
                alias = aliases.get(0);
                log.info("No alias given, use the first one: {}", alias);
            }
        }

        final Certificate[] certs = ks.getCertificateChain(alias);
        if (certs != null && certs.length > 0) {
            X509Certificate[] x509Certs = Arrays.copyOf(certs, certs.length, X509Certificate[].class);

            final X509Certificate lastCertificate = x509Certs[x509Certs.length - 1];

            if (lastCertificate.getBasicConstraints() > -1
                && lastCertificate.getSubjectX500Principal().equals(lastCertificate.getIssuerX500Principal())) {
                log.warn("Certificate chain for alias {} contains a root certificate", alias);

                if (stripRootFromChain) {
                    x509Certs = Arrays.copyOf(certs, certs.length - 1, X509Certificate[].class);
                }
            }

            return x509Certs;
        } else {
            log.error("Alias {} does not exist or contain a certificate chain", alias);
        }

        return new X509Certificate[0];
    }

    /**
     *
     * @param ks Keystore
     * @param alias Keystore Alias
     * @param keyPassword Keystore Password
     * @return Returns the private key from the keystore
     * @throws KeyStoreException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     */
    public static PrivateKey exportDecryptedKey(final KeyStore ks, final String alias, final char[] keyPassword) throws KeyStoreException,
        UnrecoverableKeyException, NoSuchAlgorithmException {
        logKeyStore(ks);
        final List<String> aliases = toList(ks.aliases());

        String evaluatedAlias = alias;

        if (alias == null && aliases.size() > 0) {
            evaluatedAlias = aliases.get(0);
        }

        if (evaluatedAlias == null) {
            throw new KeyStoreException("null alias, current aliases: " + aliases);
        }

        final Key key = ks.getKey(evaluatedAlias, (keyPassword == null || keyPassword.length == 0) ? null : keyPassword);

        if (key == null) {
            throw new KeyStoreException("no key alias named " + evaluatedAlias);
        }

        if (key instanceof PrivateKey) {
            return (PrivateKey) key;
        }

        return null;
    }

    /**
     *
     * @param ks Keystore
     */
    private static void logKeyStore(final KeyStore ks) {
        try {
            final List<String> aliases = toList(ks.aliases());
            if (log.isDebugEnabled()) {
                log.debug("Keystore has {} entries/aliases", ks.size());
                for (String _alias : aliases) {
                    log.debug(
                        "Alias {}: is a certificate entry? {}/is a key entry? {}",
                        _alias,
                        ks.isCertificateEntry(_alias),
                        ks.isKeyEntry(_alias)
                    );
                    Certificate[] certs = ks.getCertificateChain(_alias);

                    if (certs != null) {
                        log.debug("Alias {}: chain len {}", _alias, certs.length);
                        for (int i = 0; i < certs.length; i++) {
                            X509Certificate certificate = (X509Certificate) certs[i];
                            log.debug(
                                "cert {} of type {} -> {}",
                                certificate.getSubjectX500Principal(),
                                certificate.getBasicConstraints(),
                                certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal())
                            );
                        }
                    }

                    X509Certificate cert = (X509Certificate) ks.getCertificate(_alias);

                    if (cert != null) {
                        log.debug(
                            "Alias {}: single cert {} of type {} -> {}",
                            _alias,
                            cert.getSubjectX500Principal(),
                            cert.getBasicConstraints(),
                            cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal())
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error logging keystore due to " + e, e);
        }
    }

    private static List<String> toList(final Enumeration<String> enumeration) {
        final List<String> aliases = new ArrayList<>();

        while (enumeration.hasMoreElements()) {
            aliases.add(enumeration.nextElement());
        }

        return Collections.unmodifiableList(aliases);
    }
}
