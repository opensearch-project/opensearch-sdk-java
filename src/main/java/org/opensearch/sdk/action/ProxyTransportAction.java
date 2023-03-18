/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.action;

import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.TransportAction;
import org.opensearch.extensions.action.ExtensionActionResponse;
import org.opensearch.extensions.action.TransportActionRequestFromExtension;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskManager;

import com.google.inject.Inject;

/**
 * Sends a request to OpenSearch for a remote extension to execute an action.
 */
public class ProxyTransportAction extends TransportAction<TransportActionRequestFromExtension, ExtensionActionResponse> {

    /**
     * Instantiate this action
     *
     * @param actionName The action name
     * @param actionFilters Action filters
     * @param taskManager The task manager
     */
    @Inject
    protected ProxyTransportAction(String actionName, ActionFilters actionFilters, TaskManager taskManager) {
        super(actionName, actionFilters, taskManager);
    }

    @Override
    protected void doExecute(Task task, TransportActionRequestFromExtension request, ActionListener<ExtensionActionResponse> listener) {
        // TODO: Invoke ActionModule.sendProxyAction and handle the response
        if (request == null) {
            listener.onFailure(new IllegalArgumentException("The request is null."));
        } else {
            listener.onResponse(new ExtensionActionResponse(new byte[0]));
        }
    }
}
