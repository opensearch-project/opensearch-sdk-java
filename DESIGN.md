# Extensions

*Note*: This document is evolving and is in draft state.

Plugin architecture enables extending core features of OpenSearch. There are various kinds of plugins which are supported.
But, the architecture has significant problems for OpenSearch customers. Importantly, plugins can fatally impact the cluster
i.e critical workloads like ingestion/search traffic would be impacted because of a non-critical plugin like s3-repository failed with an exception.

This problem is exponentially grows when we would like to run a 3rd Party plugin from the community.  
As OpenSearch and plugins run in the same process, it brings in security risk, dependency conflicts and reduces the velocity of releases.

Introducing extensions, a simple and easy way to extend features of OpenSearch. It would support all plugin features and enable them to run in a seperate process or on another node via OpenSearch SDK Java.

Meta Issue: [Steps to make OpenSearch extensible](https://github.com/opensearch-project/OpenSearch/issues/2447)  
Sandboxing: [Step towards modular architecture in OpenSearch](https://github.com/opensearch-project/OpenSearch/issues/1422)  
Security: [Security for extensions](SECURITY.md) 

## Plugins Architecture

![](Docs/plugins.png)

Plugins are installed via [`opensearch-plugin`](https://github.com/opensearch-project/OpenSearch/blob/main/distribution/tools/plugin-cli/src/main/java/org/opensearch/plugins/InstallPluginCommand.java) and are class loaded into OpenSearch.
Plugins run within OpenSearch as a single process. Plugins interface with OpenSearch via extension points which plug into the core modules of OpenSearch.
This [blog post](https://opensearch.org/blog/technical-post/2021/12/plugins-intro/) helps untangle how plugins work. 

Walking through an example, a Plugin would like to register a custom setting which could be toggled via Rest API by the user. 
The plugin uses compiles with OpenSearch x.y.z version and generates a `.zip`.   
This `.zip` file is installed via `opensearch-plugin` tool which unpacks the code and places it under `~/plugins/<plugin-name>`.
During the bootstrap of OpenSearch node, it class loads all the code under `~/plugins/` directory. `Node.java` makes a call to get all settings the plugins would like to register. These settings are used as `additionalSettings` and construct `SettingsModule` instance which tracks all settings.

## Extensions Architecture

![](Docs/Extensions.png)

Extensions are independent processes which are built using `opensearch-sdk-java`. They communicate with OpenSearch via [transport](https://github.com/opensearch-project/OpenSearch/tree/main/modules/transport-netty4) protocol which today is used to communicate between OpenSearch nodes.  

Extensions are designed to extend features via transport APIs which are exposed using extension points of OpenSearch.

### Discovery

Extensions are discovered and configured via `extensions.yml`, the same way we currently have `plugin-descriptor.properties` which is read by OpenSearch during the node bootstrap. `ExtensionsOrchestrator` reads through the config file at `~/extensions` and registers extensions within OpenSearch.

Here is an example extension configuration `extensions.yml`:

```
extensions:
  - name: sample-extension // extension name
    uniqueId: opensearch-sdk-1 // identifier for the extension
    hostName: 'sdk_host' // name of the host where extension is running
    hostAddress: '127.0.0.1' // host to reach
    port: '4532' // port to reach
    version: '1.0' // extension version
    description: Extension for the Opensearch SDK Repo // description of the extension
    opensearchVersion: '3.0.0' // OpenSearch compatibility
```

### Communication

Extensions will use a ServerSocket which binds them listen on a host address and port defined in their configuration file. Each type of incoming request will invoke code from an associated handler. 

OpenSearch will have its own configuration file, presently `extensions.yml`, matching these addresses and ports. On startup, the ExtensionsOrchestrator will use the node's TransportService to communicate its requests to each extension, with the first request initializing the extension and validating the host and port.

Immediately following initialization, each extension will establish a connection to OpenSearch on its own transport service, and send its REST API (a list of methods and URIs to which it will respond).  These will be registered with the RestController.

When OpenSearch receives a registered method and URI, it will send the request to the Extension. The extension will appropriately handle the request, using the API to determine which Action to execute.

### OpenSearch SDK for Java

Currently, plugins rely on extension points to communicate with OpenSearch. These are represented as Actions. To turn plugins into extensions, the Extension must assemble a list of all methods and URIs to communicate to OpenSearch, where they will be registered; upon receiving a matching request from a user these will be forwarded back to the Extension and the Extension will further need to handle these registered methods and URIs with an appropriate Action.

### Extension Walk Through

1. Extensions are started up and must be running before OpenSearch is started.  (In the future, there will be a facility to refresh the extension list during operation and handle network communication interruptions.)

2. OpenSearch is started. During its bootstrap, the `ExtensionsOrchestrator` is initialized, reading extension definitions from `extensions.yml`.

3. The Node bootstrapping OpenSearch sends its transport service and REST controller objects to the `ExtensionsOrchestrator` which initializes a `RestActionsRequestHandler` object.  This completes the `ExtensionsOrchestrator` initialization.

4. The `ExtensionsOrchestrator` iterates over its configured list of extensions, sending an initialization request to each one.

5. Each Extension responds to the initialization request and then sends its REST API, a list of methods and URIs.

6. The `RestActionsRequestHandler` registers these method/URI combinations in the `RestController` as the `routes()` that extension will handle.  This step relies on a globally unique combination of the Extension's `uniqueId` and the REST method and URI.  In theory multiple extensions may share the same `uniqueId` as long as their APIs do not overlap. Using reverse-DNS style names for the `uniqueId` is recommended for published extensions.

At a later time:

7. Users send REST requests to OpenSearch.

8. If the requests match the registered `routes()` of an extension, the `RestRequest` is forwarded to the Extension, and the user receives an ACCEPTED (202) response. 

9. Upon receipt of the `RestRequest`, the extension matches it to the appropriate Action and executes it.

## FAQ

- Will extensions replace plugins?  
  Plugins will continue to be supported and extensions are preferred as they will be easier to develop, deploy, and operate.
- How is the latency going to be for extensions?
  https://github.com/opensearch-project/OpenSearch/issues/3012#issuecomment-1122682444
