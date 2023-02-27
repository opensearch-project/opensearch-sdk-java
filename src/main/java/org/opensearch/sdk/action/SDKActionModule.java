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
import java.util.stream.Collectors;

import org.opensearch.action.ActionType;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.TransportAction;
import org.opensearch.common.NamedRegistry;
import org.opensearch.sdk.ActionExtension.ActionHandler;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import org.opensearch.sdk.ActionExtension;

import static java.util.Collections.unmodifiableMap;

/**
 * A module for injecting getActions classes into Guice.
 */
public class SDKActionModule extends AbstractModule {

    private final Map<String, ActionHandler<?, ?>> actions;
    private final ActionFilters actionFilters;

    /**
     * Instantiate this module
     *
     * @param extension An instance of {@link ActionExtension}.
     */
    public SDKActionModule(ActionExtension extension) {
        this.actions = setupActions(extension);
        this.actionFilters = setupActionFilters(extension);
        // TODO: consider moving Rest Handler registration here
    }

    public Map<String, ActionHandler<?, ?>> getActions() {
        return actions;
    }

    public ActionFilters getActionFilters() {
        return actionFilters;
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

    private static ActionFilters setupActionFilters(ActionExtension extension) {
        return new ActionFilters(extension.getActionFilters().stream().collect(Collectors.toSet()));
    }

    @Override
    protected void configure() {
        // Bind action filters
        bind(ActionFilters.class).toInstance(actionFilters);

        // bind ActionType -> transportAction Map used by Client
        @SuppressWarnings("rawtypes")
        MapBinder<ActionType, TransportAction> transportActionsBinder = MapBinder.newMapBinder(
            binder(),
            ActionType.class,
            TransportAction.class
        );
        for (ActionHandler<?, ?> action : actions.values()) {
            // bind the action as eager singleton, so the map binder one will reuse it
            bind(action.getTransportAction()).asEagerSingleton();
            transportActionsBinder.addBinding(action.getAction()).to(action.getTransportAction()).asEagerSingleton();
        }
    }
}
