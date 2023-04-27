/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.sdk.ssl.util;

/**
 * File with help methods for handling exceptions related to SSL
 */
public class ExceptionUtils {

    /**
     *
     * @param e Throwable
     * @param msg Message to find within message text
     * @return Returns the throwable if it contains the message text
     */
    public static Throwable findMsg(final Throwable e, String msg) {

        if (e == null) {
            return null;
        }

        if (e.getMessage() != null && e.getMessage().contains(msg)) {
            return e;
        }

        final Throwable cause = e.getCause();
        if (cause == null) {
            return null;
        }
        return findMsg(cause, msg);
    }
}
