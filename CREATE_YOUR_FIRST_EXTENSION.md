# Creating a new extension

* [Initial setup](#initial-setup)
* [Obtain network address and port information](#obtain-network-address-and-port-information)
* [Implement the _`Extension`_ interface](#implement-the-extension-interface)
* [Implement other interfaces and extension points](#implement-other-interfaces-and-extension-points)
* [Use the OpenSearch Java client to implement functionality](#use-the-opensearch-java-client-to-implement-functionality)
  * [Defining a Document class](#defining-a-document-class)
  * [Creating (PUT) a document in an index](#creating-put-a-document-in-an-index)
  * [Reading (GET) a document in an index](#reading-get-a-document-in-an-index)
  * [Updating (POST) a document in an index](#updating-post-a-document-in-an-index)
  * [Deleting (DELETE) a document in an index](#deleting-delete-a-document-in-an-index)

*Note*: This document is evolving and is in draft state.

This document outlines how to create a new custom extension. For migration of existing plugins, see [PLUGIN_MIGRATION](PLUGIN_MIGRATION.md).

For this example, you will create a CRUD extension, demonstrating the create, read, update, and delete operations on an index.

## Initial setup

Create a new repository at a location of your choice.

In your dependency management, set up a dependency on the OpenSearch SDK for Java. Here is the required key information:
 - Group ID: `org.opensearch.sdk`
 - Artifact ID: `opensearch-sdk-java`
 - Version: `1.0.0-SNAPSHOT` (compatible with OpenSearch 2.x) or `2.0.0-SNAPSHOT` (compatible with OpenSearch 3.x)

At general availability, dependencies will be released to the Central Repository. To use SNAPSHOT versions, add these repositories:
 - OpenSearch SNAPSHOT repository: https://aws.oss.sonatype.org/content/repositories/snapshots/
 - Lucene snapshot repository: https://d1nvenhzbhpy0q.cloudfront.net/snapshots/lucene/

If you use Maven, the following POM entries will work:

```xml
<repositories>
  <repository>
    <id>opensearch.snapshots</id>
    <name>OpenSearch Snapshot Repository</name>
    <url>https://aws.oss.sonatype.org/content/repositories/snapshots/</url>
  </repository>
  <repository>
    <id>lucene.snapshots</id>
    <name>Lucene Snapshot Repository</name>
    <url>https://d1nvenhzbhpy0q.cloudfront.net/snapshots/lucene/</url>
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

For Gradle, specify dependencies as follows:

```groovy
repositories {
  mavenCentral()
  maven { url "https://aws.oss.sonatype.org/content/repositories/snapshots/" }
  maven { url "https://d1nvenhzbhpy0q.cloudfront.net/snapshots/lucene/"}
}

dependencies {
  implementation("org.opensearch.sdk:opensearch-sdk-java:2.0.0-SNAPSHOT")
}
```

## Obtain network address and port information

An extension requires host and port information for both the extension and OpenSearch.

You may either define these in code in an `ExtensionSettings` object, or in a YAML file. The following are equivalent:

- Java import and instantiation:

```java
import org.opensearch.sdk.ExtensionSettings;

new ExtensionSettings("crud", "127.0.0.1", "4532", "127.0.0.1", "9200")
```

- A `crud.yml` file:

```yml
extensionName: crud
hostAddress: 127.0.0.1
hostPort: 4532
opensearchAddress: 127.0.0.1
opensearchPort: 9200
```

## Implement the _`Extension`_ interface

Create a class that implements _`Extension`_. You may prefer to create a class that extends `BaseExtension`, which provides some helper methods.

Implementing the _`Extension`_ interface requires you to implement the `getExtensionSettings()` and `setExtensionsRunner()` methods. The `BaseExtension` class implements these and only requires that you call `super()` with either the `ExtensionSettings` object you created or a path to the YAML file (either absolute or classpath-based).

Implement a `main()` method that instantiates your object and passes an instance of itself to `ExtensionsRunner`. You will need to either handle or throw an `IOException` from this method.

The following Java code accomplishes the preceding steps:

```java
import java.io.IOException;

import org.opensearch.sdk.BaseExtension;
import org.opensearch.sdk.ExtensionSettings;
import org.opensearch.sdk.ExtensionsRunner;

public class CRUDExtension extends BaseExtension {

    public CRUDExtension() {
        // Optionally, pass a String path to a YAML file with these settings
        super(new ExtensionSettings("crud", "127.0.0.1", "4532", "127.0.0.1", "9200"));
    }

    public static void main(String[] args) throws IOException {
        ExtensionsRunner.run(new CRUDExtension());
    }
}
```

At this point, you have a working extension! Start it by executing the `main()` method, and then start your OpenSearch cluster.

But it doesn't _do_ anything yet. Here is where you can start defining your own functionality.

## Implement other interfaces and extension points

If you want to handle REST requests, implement the `ActionExtension` interface and override the `getExtensionRestHandlers()` method. Pass a list of classes that will handle those requests:

```java
import org.opensearch.sdk.api.ActionExtension;

public class CRUDExtension extends BaseExtension implements ActionExtension {

    // keep the constructor and main method from before and add the following code

    @Override
    public List<ExtensionRestHandler> getExtensionRestHandlers() {
        // you need to create this class next!
        return List.of(new CrudAction());
    }
}
```

These classes must implement _`ExtensionRestHandler`_, which is a functional interface that requires the implementation of the `handleRequest()` method with the signature `public ExtensionRestResponse handleRequest(RestRequest request)`.

The `BaseExtensionRestHandler` class provides many useful methods for exception handling in requests.

For the CRUD extension example, you'll implement one REST route for each option and delegate it to the appropriate handler function.

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

## Use the OpenSearch Java client to implement functionality

To use the OpenSearch REST API, you will need an instance of the OpenSearch Java client.

Refer to the OpenSearch Java client documentation for either [Apache HttpClient 5 Transport](https://opensearch.org/docs/latest/clients/java/#initializing-the-client-with-ssl-and-tls-enabled-using-apache-httpclient-5-transport) or [OpenSearch RestClient Transport](https://opensearch.org/docs/latest/clients/java/#initializing-the-client-with-ssl-and-tls-enabled-using-restclient-transport).

The remainder of this example assumes you have implemented one of the preceding options in your constructor:

```java
private final OpenSearchClient client;

public CrudAction() {
    final OpenSearchTransport transport = // implement per documentation
    this.client = new OpenSearchClient(transport);
}
```

### Defining a Document class

For your CRUD sample you will create a simple Java class with a single field:

```java
public static class CrudData {
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
```

### Creating (PUT) a document in an index

Now in the create handler function, create an index (if it doesn't exist):

```java
BooleanResponse exists = client.indices().exists(new ExistsRequest.Builder().index("crudsample").build());
if (!exists.value()) {
    client.indices().create(new CreateIndexRequest.Builder().index("crudsample").build());
}
```

Next, you add a document to it:

```java
CrudData crudData = new CrudData();
crudData.setValue("value");
IndexResponse response = client.index(new IndexRequest.Builder<CrudData>().index("crudsample").document(crudData).build());
```

The `BaseExtensionRestHandler` provides an `exceptionalRequest()` method to handle exceptions:

```java
return exceptionalRequest(request, e);
```

The user needs the ID of the created document (`response.id()`) for further handling. The `BaseExtensionRestHandler` provides a `createJsonResponse()` method for this:

```java
return createJsonResponse(request, RestStatus.OK, "_id", response.id());
```

Finally, you have the following code for the create handler method:

```java
Function<RestRequest, ExtensionRestResponse> createHandler = (request) -> {
    IndexResponse response;
    try {
        // Create index if it doesn't exist
        BooleanResponse exists = client.indices().exists(new ExistsRequest.Builder().index("crudsample").build());
        if (!exists.value()) {
            client.indices().create(new CreateIndexRequest.Builder().index("crudsample").build());
        }
        // Now add our document
        CrudData crudData = new CrudData();
        crudData.setValue("value");
        response = client.index(new IndexRequest.Builder<CrudData>().index("crudsample").document(crudData).build());
    } catch (OpenSearchException | IOException e) {
        return exceptionalRequest(request, e);
    }
    if (response.result() == Result.Created) {
        return createJsonResponse(request, RestStatus.OK, "_id", response.id());
    }
    return createJsonResponse(request, RestStatus.INTERNAL_SERVER_ERROR, "failed", response.result().toString());
};
```

### Reading (GET) a document in an index

You can now use the read handler function to get the document you just created, using its ID, which you will pass as a named parameter in the path. You can then get the document by ID.

```java
String id = request.param("id");
GetResponse<CrudData> response = client.get(new GetRequest.Builder().index("crudsample").id(id).build(), CrudData.class);
```

Adding exception handling, the following is the full handler method:

```java
Function<RestRequest, ExtensionRestResponse> readHandler = (request) -> {
    GetResponse<CrudData> response;
    // Parse ID from request
    String id = request.param("id");
    try {
        response = client.get(new GetRequest.Builder().index("crudsample").id(id).build(), CrudData.class);
    } catch (OpenSearchException | IOException e) {
        return exceptionalRequest(request, e);
    }
    if (response.found()) {
        return createJsonResponse(request, RestStatus.OK, "value", response.source().getValue());
    }
    return createJsonResponse(request, RestStatus.NOT_FOUND, "error", "not_found");
};
```

### Updating (POST) a document in an index

You will create a new document similar to the one you created in the create handler, parse the ID as you did in the read handler, and then update that document. With exception handling, the following is the update handler method:

```java
Function<RestRequest, ExtensionRestResponse> updateHandler = (request) -> {
    UpdateResponse<CrudData> response;
    // Parse ID from request
    String id = request.param("id");
    // Now create the new document to update with
    CrudData crudData = new CrudData();
    crudData.setValue("new value");
    try {
        response = client.update(
            new UpdateRequest.Builder<CrudData, CrudData>().index("crudsample").id(id).doc(crudData).build(),
            CrudData.class
        );
    } catch (OpenSearchException | IOException e) {
        return exceptionalRequest(request, e);
    }
    if (response.result() == Result.Updated) {
        return createEmptyJsonResponse(request, RestStatus.OK);
    }
    return createJsonResponse(request, RestStatus.INTERNAL_SERVER_ERROR, "failed", response.result().toString());
};
```

### Deleting (DELETE) a document in an index

You only need the ID to delete a document, so the delete handler method is implemented as follows:

```java
Function<RestRequest, ExtensionRestResponse> deleteHandler = (request) -> {
    DeleteResponse response;
    // Parse ID from request
    String id = request.param("id");
    try {
        response = client.delete(new DeleteRequest.Builder().index("crudsample").id(id).build());
    } catch (OpenSearchException | IOException e) {
        return exceptionalRequest(request, e);
    }
    if (response.result() == Result.Deleted) {
        return createEmptyJsonResponse(request, RestStatus.OK);
    }
    return createJsonResponse(request, RestStatus.INTERNAL_SERVER_ERROR, "failed", response.result().toString());
};
```
