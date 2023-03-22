/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.opensearch.common.breaker.CircuitBreaker;
import org.opensearch.common.settings.Settings;
import org.opensearch.indices.breaker.BreakerSettings;
import org.opensearch.indices.breaker.CircuitBreakerService;

/**
 * An extension point for {@link Extension} implementations to add custom circuit breakers
 *
 *
 */
public interface CircuitBreakerExtension {

    /**
     * Each of the factory functions are passed to the configured {@link CircuitBreakerService}.
     *
     * The service then constructs a {@link CircuitBreaker} given the resulting {@link BreakerSettings}.
     *
     * Custom circuit breakers settings can be found in {@link BreakerSettings}.
     * See:
     *  - limit (example: `breaker.foo.limit`) {@link BreakerSettings#CIRCUIT_BREAKER_LIMIT_SETTING}
     *  - overhead (example: `breaker.foo.overhead`) {@link BreakerSettings#CIRCUIT_BREAKER_OVERHEAD_SETTING}
     *  - type (example: `breaker.foo.type`) {@link BreakerSettings#CIRCUIT_BREAKER_TYPE}
     *
     * The `limit` and `overhead` settings will be dynamically updated in the circuit breaker service iff a {@link BreakerSettings}
     * object with the same name is provided at node startup.
     */
    BreakerSettings getCircuitBreaker(Settings settings);

    /**
     * The passed {@link CircuitBreaker} object is the same one that was constructed by the {@link BreakerSettings}
     * provided by {@link CircuitBreakerExtension#getCircuitBreaker(Settings)}.
     *
     * This reference should never change throughout the lifetime of the node.
     *
     * @param circuitBreaker The constructed {@link CircuitBreaker} object from the {@link BreakerSettings}
     *                       provided by {@link CircuitBreakerExtension#getCircuitBreaker(Settings)}
     */
    void setCircuitBreaker(CircuitBreaker circuitBreaker);
}
