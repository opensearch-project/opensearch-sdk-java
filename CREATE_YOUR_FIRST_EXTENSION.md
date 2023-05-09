# Creating an Extension From Scratch

*Note*: This document is evolving and is in draft state.

This document outlines how to create a brand new extension. For migration of existing plugins, see [PLUGIN_MIGRATION](PLUGIN_MIGRATION.md).

For this example we will create a CRUD Extension demonstrating Create, Read, Update, and Delete operations on an index.

## Initial Setup

Create a new repository at a location of your choice.

In your dependency management, set up a dependency on OpenSearch SDK for Java.  Key information you need:
 - Group ID: org.opensearch.sdk
 - Artifact ID: opensearch-sdk-java
 - Version: 1.0.0-SNAPSHOT (compatible with OpenSearch 2.x) or 2.0.0-SNAPSHOT (compatible with OpenSearch 3.x)
 - GA repository: Not yet released
 - SNAPSHOT repository: https://aws.oss.sonatype.org/content/repositories/snapshots/

If you use Maven, the following POM entries will work.

```xml
<repositories>
  <repository>
    <id>opensearch.snapshots</id>
    <name>OpenSearch Snapshot Repository</name>
    <url>https://aws.oss.sonatype.org/content/repositories/snapshots/</url>
    <releases>
      <enabled>false</enabled>
    </releases>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>org.opensearch.sdk</groupId>
    <artifactId>opensearch-sdk-java</artifactId>
    <version>2.0.0-SNAPSHOT</version>
  </dependency>
</dependencies>
```

## Obtain network address and port information

An extension requires host and port information for both the Extension and OpenSearch.

You may either define these in code in an `ExtensionSettings` object, or in a YAML file.  The following are equivalent:

Java import and instantiation:

```java
import org.opensearch.sdk.ExtensionSettings;

new ExtensionSettings("crud", "127.0.0.1", "4532", "127.0.0.1", "9200")
```

A crud.yml file:

```yml
extensionName: crud
hostAddress: 127.0.0.1
hostPort: 4532
opensearchAddress: 127.0.0.1
opensearchPort: 9200
```

## Implement Extension Interface

Create a class that `implements Extension`. You may prefer to use `extends BaseExtension` which provides some helper methods.

The interface implementation would require you to implement `getExtensionSettings()` and `setExtensionsRunner()` methods. The `BaseExtension` class implements these and only requires that you call `super()` with either the `ExtensionSettings` object you created, or a path to the YAML file (either absolute or classpath-based).

Implement a `main()` method that instantiates your object and passes an instance of itself to `ExtensionsRunner`. You will need to either handle or throw an `IOException` from this method.

The following Java code accomplishes the above steps:

```java
import java.io.IOException;

import org.opensearch.sdk.BaseExtension;
import org.opensearch.sdk.ExtensionSettings;
import org.opensearch.sdk.ExtensionsRunner;

public class CRUDExtension extends BaseExtension {

    public CRUDExtension() {
        // Optionally pass a String path to a YAML file with these settings
        super(new ExtensionSettings("crud", "127.0.0.1", "4532", "127.0.0.1", "9200"));
    }

    public static void main(String[] args) throws IOException {
        ExtensionsRunner.run(new CRUDExtension());
    }
}
```

At this point, you have a working Extension!  Start it up by executing the `main()` method, and then start up your OpenSearch cluster.

But it doesn't _do_ anything yet.  Here is where you can start defining your own functionality.

## Implement Other Interfaces and Extension Points

If you want to handle REST Requests, implement the `ActionExtension` interface and override the `getExtensionRestHandlers()` method.  Pass a list of classes which will handle those requests.

```java
import org.opensearch.sdk.api.ActionExtension;

public class CRUDExtension extends BaseExtension implements ActionExtension {

    // keep constructor and main method from before and add the below

    @Override
    public List<ExtensionRestHandler> getExtensionRestHandlers() {
        // we need to create this class next!
        return List.of(new CrudAction());
    }
}
```

These classes must `implement ExtensionRestHandler`, which is a functional interface which requires the implementation of `public ExtensionRestResponse handleRequest(RestRequest request)`.

However the `BaseExtensionRestHandler` provides many useful methods for exception handling in requests including a `RouteHandler` class which eases logical separation of multiple `Route` choices.

For the CRUD extension example, we'll implement one REST Route for each option and delegate to the appropriate handler function, although each one could be in its own file.

```java
import java.util.List;
import java.util.function.Function;

import org.opensearch.extensions.rest.ExtensionRestResponse;
import org.opensearch.rest.RestRequest.Method;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestStatus;
import org.opensearch.sdk.rest.BaseExtensionRestHandler;

public class CrudAction extends BaseExtensionRestHandler {

    @Override
    protected List<RouteHandler> routeHandlers() {
        return List.of(
            new RouteHandler(Method.PUT, "/sample", createHandler),
            new RouteHandler(Method.GET, "/sample/{id}", readHandler),
            new RouteHandler(Method.POST, "/sample/{id}", updateHandler),
            new RouteHandler(Method.DELETE, "/sample/{id}", deleteHandler)
        );
    }

    Function<RestRequest, ExtensionRestResponse> createHandler = (request) -> {
        return new ExtensionRestResponse(request, RestStatus.OK, "To be implemented");
    };

    Function<RestRequest, ExtensionRestResponse> readHandler = (request) -> {
        return new ExtensionRestResponse(request, RestStatus.OK, "To be implemented");
    };

    Function<RestRequest, ExtensionRestResponse> updateHandler = (request) -> {
        return new ExtensionRestResponse(request, RestStatus.OK, "To be implemented");
    };

    Function<RestRequest, ExtensionRestResponse> deleteHandler = (request) -> {
        return new ExtensionRestResponse(request, RestStatus.OK, "To be implemented");
    };
}
```

## Use OpenSearch Clients to implement functionality

During initial creation of the extension, you either implemented `setExtensionsRunner()` or used the `BaseExtension` class which does so, giving you access to the `ExtensionsRunner` object running this extension. It has getters providing access to many objects you will need, one of which is the `SDKClient`.  That class allows initialization of the OpenSearch Java Client, which has both synchronous and asynchronous options.  For simplicity we'll use the synchronous client for this example.

First, we update `CRUDExtension` to send a copy of this `ExtensionsRunner` instance to our handler class:

```java
@Override
public List<ExtensionRestHandler> getExtensionRestHandlers() {
    return List.of(new CrudAction(extensionsRunner()));
}
```

And we update our handler class to create an instance field for the client and set the value in the constructor.

```java
private OpenSearchClient client;

public CrudAction(ExtensionsRunner extensionsRunner) {
    this.client = extensionsRunner.getSdkClient().initializeJavaClient();
}
```

### Creating (PUT) a document in an index

Now in our handler function, we create an index (if it doesn't exist):

```java
BooleanResponse exists = client.indices().exists(new ExistsRequest.Builder().index("crudsample").build());
if (!exists.value()) {
    client.indices().create(new CreateIndexRequest.Builder().index("crudsample").build());
}
```

And add a document to it.

```java
Document doc = new Document();
doc.add(new StringField("field", "value", Field.Store.YES));
IndexResponse response = client.index(new IndexRequest.Builder<Document>().index("crudsample").document(doc).build());
```

We need some exception handling.  The `BaseExtensionRestHandler` provides an `exceptionalRequest()` for this.

```java
return exceptionalRequest(request, e);
```

The user needs the id of the created document (`response.id()`) for further handling.  The `BaseExtensionRestHandler` provides a `createJsonResponse()` method for this.

```java
return createJsonResponse(request, RestStatus.OK, "_id", response.id());
```

Putting it all together:

```java
Function<RestRequest, ExtensionRestResponse> createHandler = (request) -> {
    IndexResponse response;
    try {
        BooleanResponse exists = client.indices().exists(new ExistsRequest.Builder().index("crudsample").build());
        if (!exists.value()) {
            client.indices().create(new CreateIndexRequest.Builder().index("crudsample").build());
        }
        Document doc = new Document();
        doc.add(new StringField("field", "value", Field.Store.YES));
        response = client.index(new IndexRequest.Builder<Document>().index("crudsample").document(doc).build());
    } catch (OpenSearchException | IOException e) {
        return exceptionalRequest(request, e);
    }
    return createJsonResponse(request, RestStatus.OK, "_id", response.id());
};
```

### Reading (GET) a document in an index

TBD

### Updating (POST) a document in an index

TBD

### Deleting (DELETE) a document in an index

TBD
