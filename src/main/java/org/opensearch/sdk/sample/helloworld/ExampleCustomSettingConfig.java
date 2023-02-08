/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld;

import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Setting.RegexValidator;
import org.opensearch.common.settings.Setting.Property;

/**
 * {@link ExampleCustomSettingConfig} contains the custom settings value and their static declarations.
 */
public class ExampleCustomSettingConfig {
    private static final String FORBIDDEN_REGEX = "forbidden";
    private final String validated;

    /**
     * A string setting, if the string setting match the FORBIDDEN_REGEX string, the validation will be fail.
     */
    static final Setting<String> VALIDATED_SETTING = Setting.simpleString(
        "custom.validated",
        new RegexValidator(FORBIDDEN_REGEX),
        Property.NodeScope,
        Property.Dynamic
    );

    /**
     * Gets the value of the custom.validated String setting.
     *
     * @return the custom.validated value
     */
    public String getValidated() {
        return validated;
    }
}
