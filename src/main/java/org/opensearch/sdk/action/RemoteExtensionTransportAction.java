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
import org.opensearch.extensions.action.RemoteExtensionActionResponse;
import org.opensearch.sdk.SDKTransportService;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskManager;

import com.google.inject.Inject;

/**
 * Sends a request to OpenSearch for a remote extension to execute an action.
 */
public class RemoteExtensionTransportAction extends TransportAction<RemoteExtensionActionRequest, RemoteExtensionActionResponse> {

    private SDKTransportService sdkTransportService;

    /**
     * Instantiate this action
     *
     * @param actionName The action name
     * @param actionFilters Action filters
     * @param taskManager The task manager
     * @param sdkTransportService The SDK transport service
     */
    @Inject
    protected RemoteExtensionTransportAction(
        String actionName,
        ActionFilters actionFilters,
        TaskManager taskManager,
        SDKTransportService sdkTransportService
    ) {
        super(actionName, actionFilters, taskManager);
        this.sdkTransportService = sdkTransportService;
    }

    @Override
    protected void doExecute(Task task, RemoteExtensionActionRequest request, ActionListener<RemoteExtensionActionResponse> listener) {
        RemoteExtensionActionResponse response = sdkTransportService.sendRemoteExtensionActionRequest(request);
        if (response.getResponseBytes().length > 0) {
            listener.onResponse(response);
        } else {
            listener.onFailure(new RuntimeException("No response received from remote extension."));
        }
    }
}
