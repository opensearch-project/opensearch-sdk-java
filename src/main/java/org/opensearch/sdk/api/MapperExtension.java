/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.api;

import org.opensearch.index.mapper.Mapper;
import org.opensearch.index.mapper.MetadataFieldMapper;
import org.opensearch.sdk.Extension;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An extension point for {@link Extension} implementations to add custom mappers
 */
public interface MapperExtension {

    /**
     * Returns additional mapper implementations added by this extension.
     * <p>
     * The key of the returned {@link Map} is the unique name for the mapper which will be used
     * as the mapping {@code type}, and the value is a {@link Mapper.TypeParser} to parse the
     * mapper settings into a {@link Mapper}.
     */

    default Map<String, Mapper.TypeParser> getMappers() {
        return Collections.emptyMap();
    }

    /**
     * Returns additional metadata mapper implementations added by this extension.
     * <p>
     * The key of the returned {@link Map} is the unique name for the metadata mapper, which
     * is used in the mapping json to configure the metadata mapper, and the value is a
     * {@link MetadataFieldMapper.TypeParser} to parse the mapper settings into a
     * {@link MetadataFieldMapper}.
     */

    default Map<String, MetadataFieldMapper.TypeParser> getMetadataMappers() {
        return Collections.emptyMap();
    }

    /**
     * Returns a function that given an index name returns a predicate which fields must match in order to be returned by get mappings,
     * get index, get field mappings and field capabilities API. Useful to filter the fields that such API return. The predicate receives
     * the field name as input argument and should return true to show the field and false to hide it.
     */

    default Function<String, Predicate<String>> getFieldFilter() {
        return NOOP_FIELD_FILTER;
    }

    /**
     * The default field predicate applied, which doesn't filter anything. That means that by default get mappings, get index
     * get field mappings and field capabilities API will return every field that's present in the mappings.
     */

    Predicate<String> NOOP_FIELD_PREDICATE = field -> true;

    /**
     * The default field filter applied, which doesn't filter anything. That means that by default get mappings, get index
     * get field mappings and field capabilities API will return every field that's present in the mappings.
     */

    Function<String, Predicate<String>> NOOP_FIELD_FILTER = index -> NOOP_FIELD_PREDICATE;
}
