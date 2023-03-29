# Plugin Migration

*Note*: This document is evolving and is in draft state.

A goal of the Extensions SDK is to ease developer migration of plugins to extensions.  Supporting this goal:
 - Plugin interfaces and their extension points are being migrated to equivalent Extension interfaces
 - Same-named methods and arguments are used whenever possible, with differences highlighted in this migration guide
 - Wrapper classes are provided when possible to retain existing code logic

Migration of the [Anomaly Detection Plugin](https://github.com/opensearch-project/anomaly-detection) to an Extension is in progress to both identify and refine migration challenges and provide examples to other plugin developers.

## Implement Extension Interfaces and Extension Points

 - Change the implementing class `FooPlugin` to `FooExtension` and either implement the `Extension` interface or extend `BaseExtension`.
   - Implement other corresponding Extension interfaces, for example `ActionPlugin` interface would be `ActionExtension`.
   - Change extension point implementation, if necessary, to conform to new types. For example, `RestHandler` classes become `ExtensionRestHandler` classes. Consider extending base implementations such as `BaseExtensionRestHandler` to provide additional convenience methods.
   - The `createComponents()` method no longer takes parameters. The parameters formerly sent in a constructor may be accessed using Guice `@Inject` annotation.
   - Extension developers who need to use a Rest Client should initialize it using `SDKClient` and return the appropriate client as an object in `createComponents()` to make it available for extension actions.
   - Annotations of `@Inject` in actions bound using `getActions()` should change the import from the OpenSearch internal package to `com.google.inject.Inject`

## Use Wrapper Classes

### Replace ClusterService with SDKClusterService

 - Calls to `clusterService.getClusterSettings().addSettingsUpdateConsumer()` with a single consumer do not require changes.  However, this method has an overload which takes a map parameter, and can do multiple consumer updates more efficiently.
 - Calls to `clusterService.state()` do not require changes.

### Replace Client with SDK Client, either OpenSearchClient (JavaClient) or SDKRestClient

The `SDKClient` provides two (eventually three) client options.

The [Java Client for OpenSearch](https://github.com/opensearch-project/opensearch-java) (`OpenSearchClient`) will be supported with both synchronous and asynchronous clients, and is actively developed along with other language clients and should be used whenever possible. These clients do have significant implementation differences compared to the existing `Client` interface implemented by plugins.

The `SDKRestClient` provides wrapper methods matching the `Client` API (but not implementing it), implemented internally with the (soon to be deprecated) `RestHighLevelClient`.  While this speeds migration efforts, it should be considered a temporary "bridge" with follow up migration efforts to the `OpenSearchClient` planned.
 - While the class names and method parameters are the same, the `Request` and `Response` classes are often in different packages. In most cases, other than changing `import` statements, no additional code changes are required. In a few cases, there are minor changes required to interface with the new response class API.

The `client.execute(action, request, responseListener)` method is implemented on the SDKClient.

Change the transport action inheritance from HandledTransportAction to directly inherit from `TransportAction`.

### Replace RestHandler with ExtensionRestHandler

Pass the `ExtensionsRunner` and `Extension` objects to the handler and access `createComponent` equivalents, such as:
```java
this.sdkNamedXContentRegistry = extensionsRunner.getNamedXContentRegistry();
```

When a `NamedXContentRegistry` object is required, get the current one from `this.sdkNamedXContentRegistry.getRegistry()`.

When initializing objects for `createComponents`, the `SDKNamedXContentRegistry` should be passed to the component constructors. In the objects that are instantiated for `createComponents`, whenever there is an `NamedXContentRegistry` object required, call `getRegistry()` from the `SDKNamedXContentRegistry` object passed from the constructor. For example :
```java
XContentParser parser = XContentType.JSON
                    .xContent()
                    .createParser(sdkNamedXContentRegistry.getRegistry(), LoggingDeprecationHandler.INSTANCE, value);
```

Other potential initialization values:
```java
this.environmentSettings = extensionsRunner.getEnvironmentSettings();
this.transportService = extensionsRunner.getExtensionTransportService();
this.restClient = anomalyDetectorExtension.getRestClient();
this.sdkClusterService = new SDKClusterService(extensionsRunner);
```

Many of these components are also available via Guice injection.

Optionally change the `routes()` to `routeHandlers()`.  Change `prepareRequest()` to `handleRequest()`.

### Replace BytesRestResponse with ExtensionRestResponse

 - Add the `request` as the first parameter, the remainder of the parameters should be the same.
