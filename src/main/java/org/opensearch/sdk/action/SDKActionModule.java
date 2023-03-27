/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.action;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.opensearch.action.ActionType;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.TransportAction;
import org.opensearch.common.NamedRegistry;
import org.opensearch.sdk.ActionExtension.ActionHandler;
import org.opensearch.sdk.Extension;

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
     * @param extension The extension
     */
    public SDKActionModule(Extension extension) {
        this.actions = setupActions(extension);
        this.actionFilters = setupActionFilters(extension);
    }

    public Map<String, ActionHandler<?, ?>> getActions() {
        return actions;
    }

    public ActionFilters getActionFilters() {
        return actionFilters;
    }

    private static Map<String, ActionHandler<?, ?>> setupActions(Extension extension) {
        /**
         * Subclass of NamedRegistry permitting easier action registration
         */
        class ActionRegistry extends NamedRegistry<ActionHandler<?, ?>> {
            ActionRegistry() {
                super("action");
            }

            /**
             * Register an action handler pairing an ActionType and TransportAction
             *
             * @param handler The ActionHandler to register
             */
            public void register(ActionHandler<?, ?> handler) {
                register(handler.getAction().name(), handler);
            }
        }
        ActionRegistry actions = new ActionRegistry();

        // Register SDK actions
        actions.register(new ActionHandler<>(RemoteExtensionAction.INSTANCE, RemoteExtensionTransportAction.class));

        // Register actions from getActions extension point
        if (extension instanceof ActionExtension) {
            ((ActionExtension) extension).getActions().stream().forEach(actions::register);
        }
        return unmodifiableMap(actions.getRegistry());
    }

    private static ActionFilters setupActionFilters(Extension extension) {
        return new ActionFilters(
            extension instanceof ActionExtension
                ? ((ActionExtension) extension).getActionFilters().stream().collect(Collectors.toSet())
                : Collections.emptySet()
        );
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
