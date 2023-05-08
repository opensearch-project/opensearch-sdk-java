package org.opensearch.sdk;

import org.opensearch.OpenSearchException;

public class ExtensionRouteHandlerFactory {
    private static ExtensionRouteHandlerFactory INSTANCE;

    private String extensionShortName;

    private ExtensionRouteHandlerFactory() { }

    public static ExtensionRouteHandlerFactory getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new ExtensionRouteHandlerFactory();
        }

        return INSTANCE;
    }

    public void init(String extensionShortName) {
        if (this.extensionShortName != null) {
            throw new OpenSearchException("ExtensionRouteHandlerFactory was previously initialized");
        }
        this.extensionShortName = extensionShortName;
    }

    public String generateRouteName(String handlerName) {
        return extensionShortName + ":" + handlerName;
    }
}
