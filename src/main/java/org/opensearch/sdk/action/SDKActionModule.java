/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.action;

import java.util.Map;

import org.opensearch.common.NamedRegistry;
import org.opensearch.sdk.ActionExtension.ActionHandler;
import org.opensearch.sdk.ActionExtension;

import static java.util.Collections.unmodifiableMap;

public class SDKActionModule {

    private final Map<String, ActionHandler<?, ?>> actions;

    public SDKActionModule(ActionExtension extension) {
        this.actions = setupActions(extension);
    }

    public Map<String, ActionHandler<?, ?>> getActions() {
        return actions;
    }

    private static Map<String, ActionHandler<?, ?>> setupActions(ActionExtension extension) {
        // Subclass NamedRegistry for easy registration
        class ActionRegistry extends NamedRegistry<ActionHandler<?, ?>> {
            ActionRegistry() {
                super("action");
            }

            public void register(ActionHandler<?, ?> handler) {
                register(handler.getAction().name(), handler);
            }
        }
        ActionRegistry actions = new ActionRegistry();
        // Register getActions in it
        extension.getActions().stream().forEach(actions::register);

        return unmodifiableMap(actions.getRegistry());
    }

}
