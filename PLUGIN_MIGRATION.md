# Plugin Migration

*Note*: This document is evolving and is in draft state.

A goal of the Extensions SDK is to ease developer migration of plugins to extensions.  Supporting this goal:
 - Plugin interfaces and their extension points are being migrated to equivalent Extension interfaces
 - Same-named methods and arguments are used whenever possible, with differences highlighted in this migration guide
 - Wrapper classes are provided when possible to retain existing code logic

Migration of the [Anomaly Detection Plugin](https://github.com/opensearch-project/anomaly-detection) to an Extension is in progress to both identify and refine migration challenges and provide examples to other plugin developers.

## Implement Extension Interfaces and Extension Points

Change the implementing class `FooPlugin` to `FooExtension` and implement the `Extension` interface.
 - Implement other corresponding Extension interfaces, for example `ActionPlugin` interface would be `ActionExtension`.
 - Change extension point implementation, if necessary, to conform to new types. For example, `RestHandler` classes become `ExtensionRestHandler` classes.  Consider extending base classes such as `BaseExtensionRestHandler` to provide additional convenience methods.

## Use Wrapper Classes

### Replace ClusterService with SDKClusterService

 - `clusterService.getClusterSettings().addSettingsUpdateConsumer()` works as is, but has a method taking a map parameter can do multiple updates more efficiently.
 - `clusterService.state()` has no changes.

### Replace Client with SDKJavaClient or SDKRestClient

The `SDKClient` provides two client options.

The `OpenSearchClient` will be supported, and is actively developed along with other language clients and should be used whenever possible.  This client does have significant implementation differences compared to existing `Client` API.

The `SDKRestClient` provides wrapper methods matching the `Client` API (but not implementing it), implemented internally with the `RestHighLevelClient`.  While this speeds migration efforts, it should be considered a temporary "bridge" with followup migration efforts to the `SDKJavaClient` planned.
 - While the class names amd method parameters are the same, the `Request` and `Resopnse` classes are often in different packages.  In most cases, other than changing `import` statements, no additional code changes are required.  In a few changes, there are minor changes required to interface with the new response class API.

The `client.execute(action, request, responseListener)` method is not implemented. Instead:
 - Instantiate an instance of the corresponding transport action
 - Pass the `request` and `responseListener` to the action's `doExecute()` method.

Remove the transport action inheritance from HandledTransportAction. This may change to direct inheritance of `TransportAction` and implementation of `execute()`.

### Replace RestHandler with ExtensionRestHandler

Pass the `ExtensionsRunner` and `Extension` objects to the handler and access `createComponent` equivalents, such as:
```java
this.namedXContentRegistry = extensionsRunner.getNamedXContentRegistry().getRegistry();
this.environmentSettings = extensionsRunner.getEnvironmentSettings();
this.transportService = extensionsRunner.getExtensionTransportService();
this.restClient = anomalyDetectorExtension.getRestClient();
this.sdkClusterService = new SDKClusterService(extensionsRunner);
```

Optionally change the `routes()` to `routeHandlers()`.  Change `prepareRequest()` to `handleRequest()`.

### Replace RestRequest with ExtensionRestRequest

 - Change `request.contentParser()` to `request.contentParser(this.namedXContentRegistry)`
 - Change `request.getHttpRequest().method()` to `request.method()`

### Replace BytesRestResponse with ExtensionRestResponse

 - Add the `request` as the first parameter, the remainder of the parameters should be the same.
