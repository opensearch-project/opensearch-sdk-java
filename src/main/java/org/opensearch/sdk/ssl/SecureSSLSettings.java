/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.ssl;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.common.settings.SecureSetting;
import org.opensearch.common.settings.SecureString;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;

import static org.opensearch.sdk.ssl.SSLConfigConstants.DEFAULT_STORE_PASSWORD;

/**
 * Container for secured settings (passwords for certs, keystores) and the now deprecated original settings
 */
public final class SecureSSLSettings {
    private static final Logger LOG = LogManager.getLogger(SecureSSLSettings.class);

    private static final String SECURE_SUFFIX = "_secure";
    private static final String PREFIX = "ssl";
    private static final String TRANSPORT_PREFIX = PREFIX + ".transport";

    /**
     * SSLSetting
     */
    public enum SSLSetting {
        // transport settings
        SSL_TRANSPORT_PEMKEY_PASSWORD(TRANSPORT_PREFIX + ".pemkey_password"),
        SSL_TRANSPORT_SERVER_PEMKEY_PASSWORD(TRANSPORT_PREFIX + ".server.pemkey_password"),
        SSL_TRANSPORT_CLIENT_PEMKEY_PASSWORD(TRANSPORT_PREFIX + ".client.pemkey_password"),
        SSL_TRANSPORT_KEYSTORE_PASSWORD(TRANSPORT_PREFIX + ".keystore_password"),
        SSL_TRANSPORT_KEYSTORE_KEYPASSWORD(TRANSPORT_PREFIX + ".keystore_keypassword"),
        SSL_TRANSPORT_SERVER_KEYSTORE_KEYPASSWORD(TRANSPORT_PREFIX + ".server.keystore_keypassword"),
        SSL_TRANSPORT_CLIENT_KEYSTORE_KEYPASSWORD(TRANSPORT_PREFIX + ".client.keystore_keypassword"),
        SSL_TRANSPORT_TRUSTSTORE_PASSWORD(TRANSPORT_PREFIX + ".truststore_password", DEFAULT_STORE_PASSWORD);

        /**
         *
         * @param insecurePropertyName insecure property name
         */
        SSLSetting(String insecurePropertyName) {
            this(insecurePropertyName, null);
        }

        /**
         *
         * @param insecurePropertyName insecure property name
         * @param defaultValue default value
         */
        SSLSetting(String insecurePropertyName, String defaultValue) {
            this.insecurePropertyName = insecurePropertyName;
            this.propertyName = String.format("%s%s", this.insecurePropertyName, SECURE_SUFFIX);
            this.defaultValue = defaultValue;
        }

        public final String insecurePropertyName;

        public final String propertyName;

        public final String defaultValue;

        /**
         *
         * @return Returns the setting with a secure string
         */
        public Setting<SecureString> asSetting() {
            return SecureSetting.secureString(this.propertyName, new InsecureFallbackStringSetting(this.insecurePropertyName));
        }

        /**
         *
         * @return Returns the setting with an insecure string
         */
        public Setting<SecureString> asInsecureSetting() {
            return new InsecureFallbackStringSetting(this.insecurePropertyName);
        }

        /**
         *
         * @param settings Gets this setting from all settings
         * @return The value of the setting
         */
        public String getSetting(Settings settings) {
            return this.getSetting(settings, this.defaultValue);
        }

        /**
         *
         * @param settings Gets this setting from all settings
         * @param defaultValue default value if the setting does not exist
         * @return The value of the setting
         */
        public String getSetting(Settings settings, String defaultValue) {
            return Optional.of(this.asSetting().get(settings))
                .filter(ss -> ss.length() > 0)
                .map(SecureString::toString)
                .orElse(defaultValue);
        }
    }

    private SecureSSLSettings() {}

    /**
     * Alternative to InsecureStringSetting, which doesn't raise an exception if allow_insecure_settings is false, but
     * instead log.WARNs the violation. This is to appease a potential cyclic dependency between commons-utils
     */
    private static class InsecureFallbackStringSetting extends Setting<SecureString> {
        private final String name;

        /**
         *
         * @param name name of the insecure setting
         */
        private InsecureFallbackStringSetting(String name) {
            super(name, "", s -> new SecureString(s.toCharArray()), Property.Deprecated, Property.Filtered, Property.NodeScope);
            this.name = name;
        }

        /**
         *
         * @param settings all settings
         * @return returns a secure string of the setting
         */
        public SecureString get(Settings settings) {
            if (this.exists(settings)) {
                LOG.warn(
                    "Setting [{}] has a secure counterpart [{}{}] which should be used instead - allowing for legacy SSL setups",
                    this.name,
                    this.name,
                    SECURE_SUFFIX
                );
            }

            return super.get(settings);
        }
    }
}
