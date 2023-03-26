/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk;

import org.opensearch.action.ActionType;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.RequestValidators;
import org.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.opensearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.opensearch.action.support.ActionFilter;
import org.opensearch.action.support.TransportAction;
import org.opensearch.action.support.TransportActions;
import org.opensearch.common.Strings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.rest.RestHeaderDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * An additional extension point for {@link Extension}s that extends OpenSearch's scripting functionality. Implement it like this:
 * <pre>{@code
 *   {@literal @}Override
 *   public List<ActionHandler<?, ?>> getActions() {
 *       return Arrays.asList(new ActionHandler<>(ReindexAction.INSTANCE, TransportReindexAction.class),
 *               new ActionHandler<>(UpdateByQueryAction.INSTANCE, TransportUpdateByQueryAction.class),
 *               new ActionHandler<>(DeleteByQueryAction.INSTANCE, TransportDeleteByQueryAction.class),
 *               new ActionHandler<>(RethrottleAction.INSTANCE, TransportRethrottleAction.class));
 *   }
 * }</pre>
 */
public interface ActionExtension {
    /**
     * Actions added by this extension.
     */
    default List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return Collections.emptyList();
    }

    /**
     * Client actions added by this extension. This defaults to all of the {@linkplain ActionType} in
     * {@linkplain ActionExtension#getActions()}.
     */
    default List<ActionType<? extends ActionResponse>> getClientActions() {
        return getActions().stream().map(a -> a.action).collect(Collectors.toList());
    }

    /**
     * ActionType filters added by this extension.
     */
    default List<ActionFilter> getActionFilters() {
        return Collections.emptyList();
    }

    /**
     * Gets a list of {@link ExtensionRestHandler} implementations this extension handles.
     *
     * @return a list of REST handlers (REST actions) this extension handles.
     */
    default List<ExtensionRestHandler> getExtensionRestHandlers() {
        return Collections.emptyList();
    }

    /**
     * Returns headers which should be copied through rest requests on to internal requests.
     */
    default Collection<RestHeaderDefinition> getRestHeaders() {
        return Collections.emptyList();
    }

    /**
     * Returns headers which should be copied from internal requests into tasks.
     */
    default Collection<String> getTaskHeaders() {
        return Collections.emptyList();
    }

    /**
     * Returns a function used to wrap each rest request before handling the request.
     * The returned {@link UnaryOperator} is called for every incoming rest request and receives
     * the original rest handler as it's input. This allows adding arbitrary functionality around
     * rest request handlers to do for instance logging or authentication.
     * A simple example of how to only allow GET request is here:
     * <pre>
     * {@code
     *    UnaryOperator<ExtensionRestHandler> getRestHandlerWrapper(ThreadContext threadContext) {
     *      return originalHandler -> (RestHandler) (request) -> {
     *        if (request.method() != Method.GET) {
     *          throw new IllegalStateException("only GET requests are allowed");
     *        }
     *        originalHandler.handleRequest(request);
     *      };
     *    }
     * }
     * </pre>
     *
     * @param threadContext The Thread Context which can be used by the operator
     */
    default UnaryOperator<ExtensionRestHandler> getRestHandlerWrapper(ThreadContext threadContext) {
        return null;
    }

    /**
     * Class responsible for handing Transport Actions
     */
    final class ActionHandler<Request extends ActionRequest, Response extends ActionResponse> {
        private final ActionType<Response> action;
        private final Class<? extends TransportAction<Request, Response>> transportAction;
        private final Class<?>[] supportTransportActions;

        /**
         * Create a record of an action, the {@linkplain TransportAction} that handles it, and any supporting {@linkplain TransportActions}
         * that are needed by that {@linkplain TransportAction}.
         */
        public ActionHandler(
            ActionType<Response> action,
            Class<? extends TransportAction<Request, Response>> transportAction,
            Class<?>... supportTransportActions
        ) {
            this.action = action;
            this.transportAction = transportAction;
            this.supportTransportActions = supportTransportActions;
        }

        public ActionType<Response> getAction() {
            return action;
        }

        public Class<? extends TransportAction<Request, Response>> getTransportAction() {
            return transportAction;
        }

        public Class<?>[] getSupportTransportActions() {
            return supportTransportActions;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder().append(action.name()).append(" is handled by ").append(transportAction.getName());
            if (supportTransportActions.length > 0) {
                b.append('[').append(Strings.arrayToCommaDelimitedString(supportTransportActions)).append(']');
            }
            return b.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != ActionHandler.class) {
                return false;
            }
            ActionHandler<?, ?> other = (ActionHandler<?, ?>) obj;
            return Objects.equals(action, other.action)
                && Objects.equals(transportAction, other.transportAction)
                && Objects.deepEquals(supportTransportActions, other.supportTransportActions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(action, transportAction, supportTransportActions);
        }
    }

    /**
     * Returns a collection of validators that are used by {@link RequestValidators} to validate a
     * {@link org.opensearch.action.admin.indices.mapping.put.PutMappingRequest} before the executing it.
     */
    default Collection<RequestValidators.RequestValidator<PutMappingRequest>> mappingRequestValidators() {
        return Collections.emptyList();
    }

    /**
     * Returns a collection of validators that are used by {@link RequestValidators} to validate a
     * {@link org.opensearch.action.admin.indices.alias.IndicesAliasesRequest} before the executing it.
     */
    default Collection<RequestValidators.RequestValidator<IndicesAliasesRequest>> indicesAliasesRequestValidators() {
        return Collections.emptyList();
    }

}
