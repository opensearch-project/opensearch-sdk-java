/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.action;

import org.opensearch.action.ActionType;
import org.opensearch.extensions.action.ExtensionActionResponse;

/**
 * The {@link ActionType} used as they key for the {@link ProxyTransportAction}.
 */
public class ProxyAction extends ActionType<ExtensionActionResponse> {

    /**
     * The name to look up this action with
     */
    public static final String NAME = "internal/proxyaction";
    /**
     * The singleton instance of this class
     */
    public static final ProxyAction INSTANCE = new ProxyAction();

    private ProxyAction() {
        super(NAME, ExtensionActionResponse::new);
    }
}
