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
import org.opensearch.sdk.SDKTransportService;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskManager;

import com.google.inject.Inject;

/**
 * Sends a request to OpenSearch for a remote extension to execute an action.
 */
public class ProxyTransportAction extends TransportAction<ProxyActionRequest, ExtensionActionResponse> {

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
    protected ProxyTransportAction(
        String actionName,
        ActionFilters actionFilters,
        TaskManager taskManager,
        SDKTransportService sdkTransportService
    ) {
        super(actionName, actionFilters, taskManager);
        this.sdkTransportService = sdkTransportService;
    }

    @Override
    protected void doExecute(Task task, ProxyActionRequest request, ActionListener<ExtensionActionResponse> listener) {
        byte[] responseBytes = sdkTransportService.sendProxyActionRequest(request);
        if (responseBytes == null) {
            listener.onFailure(new RuntimeException("No response received from remote extension."));
        } else {
            listener.onResponse(new ExtensionActionResponse(responseBytes));
        }
    }
}
