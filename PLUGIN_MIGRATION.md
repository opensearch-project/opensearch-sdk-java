# Plugin Migration

*Note*: This document is evolving and is in draft state.

The goal of the Extensions SDK is to ease developer migration of plugins to extensions. We'll be supporting this goal by doing the following:
 - Migrating plugin interfaces and their extension points to equivalent extension interfaces.
 - Using same-named methods and arguments whenever possible, with differences highlighted in this migration guide.
 - Providing wrapper classes when possible to retain existing code logic.

Migration of the [Anomaly Detection plugin](https://github.com/opensearch-project/anomaly-detection) to an extension is in progress. We'll use it to identify and refine migration challenges and provide examples to other plugin developers.

Use the notes in the following sections to help migrate a plugin to an extension.

## Implement extension interfaces and extension points

 - Change the implementing class `FooPlugin` to `FooExtension` and either implement the _`Extension`_ interface or extend `BaseExtension`.
 - Implement other corresponding extension interfaces, for example, the `ActionPlugin` interface becomes `ActionExtension`.
 - Change extension point implementation, if necessary, to conform to new types. For example, `RestHandler` classes become `ExtensionRestHandler` classes. Consider extending base implementations, such as `BaseExtensionRestHandler`, to provide additional convenience methods.
 - The `createComponents()` method no longer takes parameters. The parameters formerly sent in a constructor may be accessed using Guice `@Inject` annotation.
 - Extension developers who need to use a REST client should initialize it using `SDKClient` and return the appropriate client as an object in `createComponents()` to make it available for extension actions.
 - Annotations of `@Inject` in actions bound using `getActions()` should change the import from the OpenSearch internal package to `com.google.inject.Inject`.

## Use wrapper classes

Refer to the following notes for using wrapper classes.

### Replace `ClusterService` with `SDKClusterService`

 - Calls to `clusterService.getClusterSettings().addSettingsUpdateConsumer()` with a single consumer do not require changes. However, this method has an overloaded version that takes a map parameter and can perform multiple consumer updates more efficiently.
 - Calls to `clusterService.state()` do not require changes.

### Replace `Client` with `SDKClient`: either `OpenSearchClient` (`JavaClient`) or `SDKRestClient`

The `SDKClient` provides two (eventually three) client options.

The [Java client for OpenSearch](https://github.com/opensearch-project/opensearch-java) (`OpenSearchClient`) will be supported with both synchronous and asynchronous clients, is actively developed along with other language clients, and should be used whenever possible. These clients do have significant implementation differences compared to the existing `Client` interface implemented by plugins.

## Change plugin and OpenSearch `TransportAction` implementations

The `SDKRestClient` provides wrapper methods matching the `Client` API (but not implementing it), implemented internally with the (soon to be deprecated) `RestHighLevelClient`. While this expedites migration efforts, it should be considered a temporary "bridge," with follow-up migration efforts to the `OpenSearchClient` planned.
 - While the class names and method parameters are the same, the `Request` and `Response` classes are often in different packages. In most cases, other than changing `import` statements, no additional code changes are required. In a few cases, there are minor changes required to interface with the new response class API.

The `client.execute(action, request, responseListener)` method is implemented in the `SDKClient`.

For TransportActions internal to the plugin (registered with `getActions()`), change the transport action inheritance from `HandledTransportAction` to directly inherit from `TransportAction`.

OpenSearch TransportActions are not accessible to extensions and will need to be replaced with functionality from either a client (OpenSearch client for Java or the `SDKRestClient`) or some other functionality directly provided by the Extensions SDK. The following are a few examples of the types of changes needed:
 - Some transport actions on OpenSearch, such as the `GetFieldMappingsAction`, are exposed via the REST API and should be called using those clients.
 - Some information available from OpenSearch services, such as the state on `ClusterService`, stats on the `IndexingPressure` object, and others, are designed for local access and would transfer far more data than needed if implemented directly. Calls to these services should be replaced by REST API calls to endpoints, which filter to just the information required. For example, cluster state associated with indexes should use one of the Index API endpoints. Indexing Pressure can be retrieved by Node API endpoints.

### Replace `RestHandler` with `ExtensionRestHandler`

Pass the `ExtensionsRunner` and `Extension` objects to the handler and access `createComponent` equivalents, such as:
```java
this.sdkNamedXContentRegistry = extensionsRunner.getNamedXContentRegistry();
```

When a `NamedXContentRegistry` object is required, get the current one from `this.sdkNamedXContentRegistry.getRegistry()`.

When initializing objects for `createComponents`, the `SDKNamedXContentRegistry` should be passed to the component constructors. In the objects that are instantiated for `createComponents`, whenever there is a `NamedXContentRegistry` object required, call `getRegistry()` from the `SDKNamedXContentRegistry` object passed from the constructor, for example:
```java
XContentParser parser = XContentType.JSON
                    .xContent()
                    .createParser(sdkNamedXContentRegistry.getRegistry(), LoggingDeprecationHandler.INSTANCE, value);
```

Other potential initialization values are:
```java
this.environmentSettings = extensionsRunner.getEnvironmentSettings();
this.transportService = extensionsRunner.getSdkTransportService().getTransportService();
this.restClient = anomalyDetectorExtension.getRestClient();
this.sdkClusterService = new SDKClusterService(extensionsRunner);
```

Many of these components are also available via Guice injection.

### Replace `Route` with `NamedRoute`
Change `routes()` to be NamedRoutes. Here is a sample of an existing route converted to a named route:
Before:
```
public List<Route> routes() {
    return ImmutableList.of(
            new Route(GET, "/uri")
        );
}
```
With new scheme:
```
private Function<RestRequest, RestResponse> uriHandler = () -> {};
public List<NamedRoute> routes() {
    return ImmutableList.of(
            new NamedRoute.Builder().method(GET).path("/uri").uniqueName("extension:uri").handler(uriHandler).build()
        );
}
```

You can optionally also add `actionNames()` to this route. These should correspond to any current actions defined as permissions in roles.
Ensure that these name-to-route mappings are easily accessible to the cluster admins to allow granting access to these APIs.

Change `prepareRequest()` to `handleRequest()`.

### Replace `BytesRestResponse` with `ExtensionRestResponse`

 - Add the `request` as the first parameter. The remaining parameters should be the same.

### Replace Return Type for SDKRestClient

While most SDKRestClient client return types match existing classes, some changes may be necessary to conform to the new method signatures. Examples include:
- `InternalAggregation/Min/Max` should be replaced with the corresponding `Parsed` class. For example:
  1. `ParsedStringTerms` from `StringTerms` to fetch the aggregation for a specific index.
  2. `ParsedMax` from `InternalMax` to fetch max agg result parsed between nodes.
- Replace `ObjectObjectCursor<String, List<AliasMetadata>> entry` with `Entry<String, Set<AliasMetadata>> entry`
