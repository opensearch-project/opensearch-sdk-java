/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.transport;

import org.opensearch.action.ActionType;
import org.opensearch.sdk.sample.helloworld.transport.SampleResponse;

/**
 * A sample {@link ActionType} used as they key for the action map
 */
public class SampleAction extends ActionType<SampleResponse> {

    /**
     * The name to look up this action with
     */
    public static final String NAME = "helloworld/sample";
    /**
     * The singleton instance of this class
     */
    public static final SampleAction INSTANCE = new SampleAction();

    private SampleAction() {
        super(NAME, SampleResponse::new);
    }
}
