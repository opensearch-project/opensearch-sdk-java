/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.sdk.ssl.util;

import org.opensearch.OpenSearchException;

public class ExceptionUtils {
    
    public static Throwable getRootCause(final Throwable e) {
        
        if(e == null) {
            return null;
        }
        
        final Throwable cause = e.getCause();
        if(cause == null) {
            return e;
        }
        return getRootCause(cause);
    }
    
    public static Throwable findMsg(final Throwable e, String msg) {
        
        if(e == null) {
            return null;
        }
        
        if(e.getMessage() != null && e.getMessage().contains(msg)) {
            return e;
        }
        
        final Throwable cause = e.getCause();
        if(cause == null) {
            return null;
        }
        return findMsg(cause, msg);
    }

    public static OpenSearchException createBadHeaderException() {
        return new OpenSearchException("Illegal parameter in http or transport request found."+System.lineSeparator()
                + "This means that one node is trying to connect to another with "+System.lineSeparator()
                + "a non-node certificate (no OID or security.nodes_dn incorrect configured) or that someone "+System.lineSeparator()
                + "is spoofing requests. Check your TLS certificate setup as described here: "
                + "See https://opendistro.github.io/for-elasticsearch-docs/docs/troubleshoot/tls/");
    }

    public static OpenSearchException createTransportClientNoLongerSupportedException() {
        return new OpenSearchException("Transport client authentication no longer supported.");
    }
}
