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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionType;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.TransportAction;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.NamedRegistry;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.action.RegisterTransportActionsRequest;
import org.opensearch.sdk.ActionExtension.ActionHandler;
import org.opensearch.sdk.Extension;
import org.opensearch.sdk.ExtensionsRunner;
import org.opensearch.sdk.handlers.AcknowledgedResponseHandler;
import org.opensearch.transport.TransportService;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import org.opensearch.sdk.ActionExtension;

import static java.util.Collections.unmodifiableMap;

/**
 * A module for injecting getActions classes into Guice.
 */
public class SDKActionModule extends AbstractModule {
    private final Logger logger = LogManager.getLogger(SDKActionModule.class);

    private ExtensionsRunner extensionsRunner;
    private final Map<String, ActionHandler<?, ?>> actions;
    private final ActionFilters actionFilters;

    /**
     * Instantiate this module
     *
     * @param extensionsRunner The ExtensionsRunner instance
     * @param extension The extension
     */
    public SDKActionModule(ExtensionsRunner extensionsRunner, Extension extension) {
        this.extensionsRunner = extensionsRunner;
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
                super("sdkaction");
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
        actions.register(new ActionHandler<>(ProxyAction.INSTANCE, ProxyTransportAction.class));

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

        // Bind local actions

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

    /**
     * Requests that OpenSearch register the Transport Actions for this extension.
     */
    public void sendRegisterTransportActionsRequest() {
        logger.info("Sending Register Transport Actions request to OpenSearch");
        TransportService transportService = extensionsRunner.getExtensionTransportService();
        DiscoveryNode opensearchNode = extensionsRunner.getOpensearchNode();
        String uniqueId = extensionsRunner.getUniqueId();
        AcknowledgedResponseHandler registerTransportActionsResponseHandler = new AcknowledgedResponseHandler();
        try {
            transportService.sendRequest(
                opensearchNode,
                ExtensionsManager.REQUEST_EXTENSION_REGISTER_TRANSPORT_ACTIONS,
                new RegisterTransportActionsRequest(uniqueId, getActions().keySet()),
                registerTransportActionsResponseHandler
            );
        } catch (Exception e) {
            logger.info("Failed to send Register Transport Actions request to OpenSearch", e);
        }
    }
}
