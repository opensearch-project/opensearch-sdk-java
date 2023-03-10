/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opensearch.index.mapper.Mapper;
import org.opensearch.index.mapper.MetadataFieldMapper;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class TestExtensionMapper {

    @Test
    public void testGetMappers() {
        MapperExtension mapperExtension = new MapperExtension() {
        };
        Map<String, Mapper.TypeParser> mappers = mapperExtension.getMappers();
        Assertions.assertNotNull(mappers);
        Assertions.assertTrue(mappers.isEmpty());
    }

    @Test
    public void testGetMetadataMappers() {
        MapperExtension mapperExtension = new MapperExtension() {
        };
        Map<String, MetadataFieldMapper.TypeParser> metadataMappers = mapperExtension.getMetadataMappers();
        Assertions.assertNotNull(metadataMappers);
        Assertions.assertTrue(metadataMappers.isEmpty());
    }

    @Test
    public void testGetFieldFilter() {
        MapperExtension extension = new MapperExtension() {
        };
        Function<String, Predicate<String>> fieldFilter = extension.getFieldFilter();
        String indexName = "myIndex";
        Predicate<String> predicate = fieldFilter.apply(indexName);
        Assertions.assertNotNull(predicate);
    }

    @Test
    public void testNoopFieldPredicate() {
        Predicate<String> noopFieldPredicate = MapperExtension.NOOP_FIELD_PREDICATE;
        Assertions.assertNotNull(noopFieldPredicate);
        Assertions.assertTrue(noopFieldPredicate.test("field"));
    }

    @Test
    public void testNoopFieldFilter() {
        Function<String, Predicate<String>> noopFieldFilter = MapperExtension.NOOP_FIELD_FILTER;
        Assertions.assertNotNull(noopFieldFilter);
        Predicate<String> predicate = noopFieldFilter.apply("indexName");
        Assertions.assertNotNull(predicate);
        Assertions.assertTrue(predicate.test("field"));
    }
}
