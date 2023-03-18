/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.Version;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.WriteableSetting;
import org.opensearch.common.unit.ByteSizeValue;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.extensions.AcknowledgedResponse;
import org.opensearch.extensions.UpdateSettingsRequest;

/**
 * Handles requests to update settings
 */
public class UpdateSettingsRequestHandler {

    private static final Logger logger = LogManager.getLogger(UpdateSettingsRequestHandler.class);

    private Map<Setting<?>, Consumer<?>> settingUpdateConsumers;

    /**
     * Instantiates a new Update Setting Request Handler
     */
    public UpdateSettingsRequestHandler() {
        this.settingUpdateConsumers = new HashMap<>();
    }

    /**
     * Registers the component {@link Setting} and the corresponding consumer to the settingsUpdateConsumer map.
     * This map is used only when handling {@link UpdateSettingsRequest}
     *
     * @param settingUpdateConsumers The settings and their corresponding update consumers to register
     */
    public void registerSettingUpdateConsumer(Map<Setting<?>, Consumer<?>> settingUpdateConsumers) {
        this.settingUpdateConsumers.putAll(settingUpdateConsumers);
    }

    /**
     * Handles a request to update a setting from OpenSearch.  Extensions must register their setting keys and consumers within the settingUpdateConsumer map
     *
     * @param updateSettingsRequest  The request to handle.
     * @return A response acknowledging the request.
     */
    @SuppressWarnings("unchecked")
    public AcknowledgedResponse handleUpdateSettingsRequest(UpdateSettingsRequest updateSettingsRequest) {

        logger.info("Registering UpdateSettingsRequest received from OpenSearch");

        boolean settingUpdateStatus = true;

        WriteableSetting.SettingType settingType = updateSettingsRequest.getSettingType();
        Setting<?> componentSetting = updateSettingsRequest.getComponentSetting();
        Object data = updateSettingsRequest.getData();

        // Setting updater in OpenSearch performs setting change validation, only need to cast the consumer to the corresponding type and
        // invoke the consumer
        try {
            switch (settingType) {
                case Boolean:
                    Consumer<Boolean> boolConsumer = (Consumer<Boolean>) this.settingUpdateConsumers.get(componentSetting);
                    boolConsumer.accept(Boolean.parseBoolean(data.toString()));
                case Integer:
                    Consumer<Integer> intConsumer = (Consumer<Integer>) this.settingUpdateConsumers.get(componentSetting);
                    intConsumer.accept(Integer.parseInt(data.toString()));
                case Long:
                    Consumer<Long> longConsumer = (Consumer<Long>) this.settingUpdateConsumers.get(componentSetting);
                    longConsumer.accept(Long.parseLong(data.toString()));
                case Float:
                    Consumer<Float> floatConsumer = (Consumer<Float>) this.settingUpdateConsumers.get(componentSetting);
                    floatConsumer.accept(Float.parseFloat(data.toString()));
                case Double:
                    Consumer<Double> doubleConsumer = (Consumer<Double>) this.settingUpdateConsumers.get(componentSetting);
                    doubleConsumer.accept(Double.parseDouble(data.toString()));
                case String:
                    Consumer<String> stringConsumer = (Consumer<String>) this.settingUpdateConsumers.get(componentSetting);
                    stringConsumer.accept(data.toString());
                case TimeValue:
                    Consumer<TimeValue> timeValueConsumer = (Consumer<TimeValue>) this.settingUpdateConsumers.get(componentSetting);
                    timeValueConsumer.accept(TimeValue.parseTimeValue(data.toString(), componentSetting.getKey()));
                case ByteSizeValue:
                    Consumer<ByteSizeValue> byteSizeValueConsumer = (Consumer<ByteSizeValue>) this.settingUpdateConsumers.get(
                        componentSetting
                    );
                    byteSizeValueConsumer.accept(ByteSizeValue.parseBytesSizeValue(data.toString(), componentSetting.getKey()));
                case Version:
                    Consumer<Version> versionConsumer = (Consumer<Version>) this.settingUpdateConsumers.get(componentSetting);
                    versionConsumer.accept((Version) data);
            }
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException("Setting Update Consumer type does not exist and is not handled here");
        } catch (Exception e) {
            logger.info(e.getMessage());
            settingUpdateStatus = false;
        }

        return new AcknowledgedResponse(settingUpdateStatus);
    }
}
