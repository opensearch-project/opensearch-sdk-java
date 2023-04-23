/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.sample.helloworld.util;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.OpenSearchStatusException;
import org.opensearch.action.ActionListener;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.index.IndexNotFoundException;
import org.opensearch.indices.InvalidIndexNameException;
import org.opensearch.rest.RestStatus;

import static org.opensearch.rest.RestStatus.BAD_REQUEST;
import static org.opensearch.rest.RestStatus.INTERNAL_SERVER_ERROR;

/**
 * Utility functions for REST handlers.
 */
public final class RestHandlerUtils {
    private static final Logger logger = LogManager.getLogger(RestHandlerUtils.class);

    public static final ToXContent.MapParams XCONTENT_WITH_TYPE = new ToXContent.MapParams(ImmutableMap.of("with_type", "true"));

    private RestHandlerUtils() {}

    /**
     * Wrap action listener to avoid return verbose error message and wrong 500 error to user.
     * Suggestion for exception handling in HW:
     * 1. If the error is caused by wrong input, throw IllegalArgumentException exception.
     * 2. For other errors, please use OpenSearchStatusException.
     *
     * TODO: tune this function for wrapped exception, return root exception error message
     *
     * @param actionListener action listener
     * @param generalErrorMessage general error message
     * @param <T> action listener response type
     * @return wrapped action listener
     */
    public static <T> ActionListener wrapRestActionListener(ActionListener<T> actionListener, String generalErrorMessage) {
        return ActionListener.<T>wrap(r -> { actionListener.onResponse(r); }, e -> {
            logger.error("Wrap exception before sending back to user", e);
            Throwable cause = Throwables.getRootCause(e);
            if (isProperExceptionToReturn(e)) {
                actionListener.onFailure(e);
            } else if (isProperExceptionToReturn(cause)) {
                actionListener.onFailure((Exception) cause);
            } else {
                RestStatus status = isBadRequest(e) ? BAD_REQUEST : INTERNAL_SERVER_ERROR;
                String errorMessage = generalErrorMessage;
                if (isBadRequest(e)) {
                    errorMessage = e.getMessage();
                } else if (cause != null && isBadRequest(cause)) {
                    errorMessage = cause.getMessage();
                }
                actionListener.onFailure(new OpenSearchStatusException(errorMessage, status));
            }
        });
    }

    public static boolean isBadRequest(Throwable e) {
        if (e == null) {
            return false;
        }
        return e instanceof IllegalArgumentException;
    }

    public static boolean isProperExceptionToReturn(Throwable e) {
        if (e == null) {
            return false;
        }
        return e instanceof OpenSearchStatusException || e instanceof IndexNotFoundException || e instanceof InvalidIndexNameException;
    }
}
