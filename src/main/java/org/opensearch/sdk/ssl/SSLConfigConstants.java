/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.ssl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.opensearch.common.settings.Settings;

public final class SSLConfigConstants {
    public static final String SSL_TRANSPORT_ENABLED = "ssl.transport.enabled";
    // TODO Replace this with true when security changes are complete
    public static final boolean SSL_TRANSPORT_ENABLED_DEFAULT = false;
    public static final String SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION = "ssl.transport.enforce_hostname_verification";
    public static final String SSL_TRANSPORT_ENFORCE_HOSTNAME_VERIFICATION_RESOLVE_HOST_NAME = "ssl.transport.resolve_hostname";

    public static final String SSL_TRANSPORT_KEYSTORE_ALIAS = "ssl.transport.keystore_alias";
    public static final String SSL_TRANSPORT_SERVER_KEYSTORE_ALIAS = "ssl.transport.server.keystore_alias";
    public static final String SSL_TRANSPORT_CLIENT_KEYSTORE_ALIAS = "ssl.transport.client.keystore_alias";

    public static final String SSL_TRANSPORT_KEYSTORE_FILEPATH = "ssl.transport.keystore_filepath";
    public static final String SSL_TRANSPORT_PEMKEY_FILEPATH = "ssl.transport.pemkey_filepath";
    public static final String SSL_TRANSPORT_PEMCERT_FILEPATH = "ssl.transport.pemcert_filepath";

    public static final String SSL_TRANSPORT_PEMTRUSTEDCAS_FILEPATH = "ssl.transport.pemtrustedcas_filepath";
    public static final String SSL_TRANSPORT_EXTENDED_KEY_USAGE_ENABLED = "ssl.transport.extended_key_usage_enabled";
    public static final boolean SSL_TRANSPORT_EXTENDED_KEY_USAGE_ENABLED_DEFAULT = false;
    public static final String SSL_TRANSPORT_SERVER_PEMKEY_FILEPATH = "ssl.transport.server.pemkey_filepath";
    public static final String SSL_TRANSPORT_SERVER_PEMCERT_FILEPATH = "ssl.transport.server.pemcert_filepath";
    public static final String SSL_TRANSPORT_SERVER_PEMTRUSTEDCAS_FILEPATH = "ssl.transport.server.pemtrustedcas_filepath";
    public static final String SSL_TRANSPORT_CLIENT_PEMKEY_FILEPATH = "ssl.transport.client.pemkey_filepath";
    public static final String SSL_TRANSPORT_CLIENT_PEMCERT_FILEPATH = "ssl.transport.client.pemcert_filepath";
    public static final String SSL_TRANSPORT_CLIENT_PEMTRUSTEDCAS_FILEPATH = "ssl.transport.client.pemtrustedcas_filepath";

    public static final String SSL_TRANSPORT_KEYSTORE_TYPE = "ssl.transport.keystore_type";

    public static final String SSL_TRANSPORT_TRUSTSTORE_ALIAS = "ssl.transport.truststore_alias";
    public static final String SSL_TRANSPORT_SERVER_TRUSTSTORE_ALIAS = "ssl.transport.server.truststore_alias";
    public static final String SSL_TRANSPORT_CLIENT_TRUSTSTORE_ALIAS = "ssl.transport.client.truststore_alias";

    public static final String SSL_TRANSPORT_TRUSTSTORE_FILEPATH = "ssl.transport.truststore_filepath";
    public static final String SSL_TRANSPORT_TRUSTSTORE_TYPE = "ssl.transport.truststore_type";
    public static final String SSL_TRANSPORT_ENABLED_CIPHERS = "ssl.transport.enabled_ciphers";
    public static final String SSL_TRANSPORT_ENABLED_PROTOCOLS = "ssl.transport.enabled_protocols";
    public static final String DEFAULT_STORE_PASSWORD = "changeit"; // #16

    private static final String[] _SECURE_SSL_PROTOCOLS = { "TLSv1.3", "TLSv1.2", "TLSv1.1" };

    public static final String[] getSecureSSLProtocols(Settings settings) {
        List<String> configuredProtocols = null;

        if (settings != null) {
            configuredProtocols = settings.getAsList(SSL_TRANSPORT_ENABLED_PROTOCOLS, Collections.emptyList());
        }

        if (configuredProtocols != null && configuredProtocols.size() > 0) {
            return configuredProtocols.toArray(new String[0]);
        }

        return _SECURE_SSL_PROTOCOLS.clone();
    }

    // @formatter:off
    private static final String[] _SECURE_SSL_CIPHERS = {
        // TLS_<key exchange and authentication algorithms>_WITH_<bulk cipher and message authentication algorithms>

        // Example (including unsafe ones)
        // Protocol: TLS, SSL
        // Key Exchange RSA, Diffie-Hellman, ECDH, SRP, PSK
        // Authentication RSA, DSA, ECDSA
        // Bulk Ciphers RC4, 3DES, AES
        // Message Authentication HMAC-SHA256, HMAC-SHA1, HMAC-MD5

        // thats what chrome 48 supports (https://cc.dcsec.uni-hannover.de/)
        // (c0,2b)ECDHE-ECDSA-AES128-GCM-SHA256128 BitKey exchange: ECDH, encryption: AES, MAC: SHA256.
        // (c0,2f)ECDHE-RSA-AES128-GCM-SHA256128 BitKey exchange: ECDH, encryption: AES, MAC: SHA256.
        // (00,9e)DHE-RSA-AES128-GCM-SHA256128 BitKey exchange: DH, encryption: AES, MAC: SHA256.
        // (cc,14)ECDHE-ECDSA-CHACHA20-POLY1305-SHA256128 BitKey exchange: ECDH, encryption: ChaCha20 Poly1305, MAC: SHA256.
        // (cc,13)ECDHE-RSA-CHACHA20-POLY1305-SHA256128 BitKey exchange: ECDH, encryption: ChaCha20 Poly1305, MAC: SHA256.
        // (c0,0a)ECDHE-ECDSA-AES256-SHA256 BitKey exchange: ECDH, encryption: AES, MAC: SHA1.
        // (c0,14)ECDHE-RSA-AES256-SHA256 BitKey exchange: ECDH, encryption: AES, MAC: SHA1.
        // (00,39)DHE-RSA-AES256-SHA256 BitKey exchange: DH, encryption: AES, MAC: SHA1.
        // (c0,09)ECDHE-ECDSA-AES128-SHA128 BitKey exchange: ECDH, encryption: AES, MAC: SHA1.
        // (c0,13)ECDHE-RSA-AES128-SHA128 BitKey exchange: ECDH, encryption: AES, MAC: SHA1.
        // (00,33)DHE-RSA-AES128-SHA128 BitKey exchange: DH, encryption: AES, MAC: SHA1.
        // (00,9c)RSA-AES128-GCM-SHA256128 BitKey exchange: RSA, encryption: AES, MAC: SHA256.
        // (00,35)RSA-AES256-SHA256 BitKey exchange: RSA, encryption: AES, MAC: SHA1.
        // (00,2f)RSA-AES128-SHA128 BitKey exchange: RSA, encryption: AES, MAC: SHA1.
        // (00,0a)RSA-3DES-EDE-SHA168 BitKey exchange: RSA, encryption: 3DES, MAC: SHA1.

        // thats what firefox 42 supports (https://cc.dcsec.uni-hannover.de/)
        // (c0,2b) ECDHE-ECDSA-AES128-GCM-SHA256
        // (c0,2f) ECDHE-RSA-AES128-GCM-SHA256
        // (c0,0a) ECDHE-ECDSA-AES256-SHA
        // (c0,09) ECDHE-ECDSA-AES128-SHA
        // (c0,13) ECDHE-RSA-AES128-SHA
        // (c0,14) ECDHE-RSA-AES256-SHA
        // (00,33) DHE-RSA-AES128-SHA
        // (00,39) DHE-RSA-AES256-SHA
        // (00,2f) RSA-AES128-SHA
        // (00,35) RSA-AES256-SHA
        // (00,0a) RSA-3DES-EDE-SHA

        // Mozilla modern browsers
        // https://wiki.mozilla.org/Security/Server_Side_TLS
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",

        // TLS 1.3
        "TLS_AES_128_GCM_SHA256",
        "TLS_AES_256_GCM_SHA384",
        "TLS_CHACHA20_POLY1305_SHA256", // Open SSL >= 1.1.1 and Java >= 12

        // TLS 1.2 CHACHA20 POLY1305 supported by Java >= 12 and
        // OpenSSL >= 1.1.0
        "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
        "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
        "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",

        // IBM
        "SSL_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "SSL_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "SSL_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "SSL_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "SSL_DHE_RSA_WITH_AES_128_GCM_SHA256",
        "SSL_DHE_DSS_WITH_AES_128_GCM_SHA256",
        "SSL_DHE_DSS_WITH_AES_256_GCM_SHA384",
        "SSL_DHE_RSA_WITH_AES_256_GCM_SHA384",
        "SSL_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
        "SSL_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "SSL_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "SSL_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "SSL_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
        "SSL_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        "SSL_ECDHE_RSA_WITH_AES_256_CBC_SHA",
        "SSL_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
        "SSL_DHE_RSA_WITH_AES_128_CBC_SHA256",
        "SSL_DHE_RSA_WITH_AES_128_CBC_SHA",
        "SSL_DHE_DSS_WITH_AES_128_CBC_SHA256",
        "SSL_DHE_RSA_WITH_AES_256_CBC_SHA256",
        "SSL_DHE_DSS_WITH_AES_256_CBC_SHA",
        "SSL_DHE_RSA_WITH_AES_256_CBC_SHA"

        // some others
        // "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        // "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
        // "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        // "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
        // "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
        // "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        // "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
        // "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        // "TLS_RSA_WITH_AES_128_CBC_SHA256",
        // "TLS_RSA_WITH_AES_128_GCM_SHA256",
        // "TLS_RSA_WITH_AES_128_CBC_SHA",
        // "TLS_RSA_WITH_AES_256_CBC_SHA",
    };
    // @formatter:on

    public static final List<String> getSecureSSLCiphers(Settings settings) {

        List<String> configuredCiphers = null;

        if (settings != null) {
            configuredCiphers = settings.getAsList(SSL_TRANSPORT_ENABLED_CIPHERS, Collections.emptyList());
        }

        if (configuredCiphers != null && configuredCiphers.size() > 0) {
            return configuredCiphers;
        }

        return Collections.unmodifiableList(Arrays.asList(_SECURE_SSL_CIPHERS));
    }

    private SSLConfigConstants() {

    }

}
