package org.opensearch.sdk;

import org.opensearch.common.settings.Settings;
import org.opensearch.plugins.Plugin;
import org.opensearch.script.ScriptContext;
import org.opensearch.script.ScriptEngine;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * An additional extension point for {@link Extension}s that extends OpenSearch's scripting functionality.
 *
 * @opensearch.api
 */
public interface ScriptExtension {

    /**
     * Returns a {@link ScriptEngine} instance or <code>null</code> if this extension doesn't add a new script engine.
     * @param settings Node settings
     * @param contexts The contexts that {@link ScriptEngine#compile(String, String, ScriptContext, Map)} may be called with
     */
    default ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
        return null;
    }

    /**
     * Return script contexts this extension wants to allow using.
     */
    default List<ScriptContext<?>> getContexts() {
        return Collections.emptyList();
    }
}

