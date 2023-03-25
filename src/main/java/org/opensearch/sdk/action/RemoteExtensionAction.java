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
import org.opensearch.extensions.action.RemoteExtensionActionResponse;

/**
 * The {@link ActionType} used as they key for the {@link RemoteExtensionTransportAction}.
 */
public class RemoteExtensionAction extends ActionType<RemoteExtensionActionResponse> {

    /**
     * The name to look up this action with
     */
    public static final String NAME = "internal:remote-extension-action";
    /**
     * The singleton instance of this class
     */
    public static final RemoteExtensionAction INSTANCE = new RemoteExtensionAction();

    private RemoteExtensionAction() {
        super(NAME, RemoteExtensionActionResponse::new);
    }
}
