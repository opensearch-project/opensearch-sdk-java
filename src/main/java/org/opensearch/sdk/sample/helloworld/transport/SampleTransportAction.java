/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.transport;

import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.TransportAction;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskManager;

/**
 * A sample {@link TransportAction} used as they value for the action map
 */
public class SampleTransportAction extends TransportAction<SampleRequest, SampleResponse> {

    /**
     * Instantiate this action
     *
     * @param actionName The action name
     * @param actionFilters Action filters
     * @param taskManager The task manager
     */
    protected SampleTransportAction(String actionName, ActionFilters actionFilters, TaskManager taskManager) {
        super(actionName, actionFilters, taskManager);
    }

    @Override
    protected void doExecute(Task task, SampleRequest request, ActionListener<SampleResponse> listener) {
        // Fail if name is empty
        if (request.getName().isBlank()) {
            listener.onFailure(new IllegalArgumentException("The request name is blank."));
        } else {
            listener.onResponse(new SampleResponse("Hello, " + request.getName()));
        }
    }
}
