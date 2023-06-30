
# OpenSearch SDK for Java Developer Guide

[OpenSearch SDK for Java Developer Guide](#opensearch-sdk-for-java-developer-guide)
* [Getting started](#getting-started)
  * [Start the extension](#start-the-extension)
    * [Clone the OpenSearch SDK for Java repository](#clone-the-opensearch-sdk-for-java-repository)
    * [Run the sample extension](#run-the-sample-extension)
    * [Run the sample extension with security](#run-the-sample-extension-with-security)
  * [Start OpenSearch](#start-opensearch)
    * [Clone the OpenSearch repository](#clone-the-opensearch-repository)
    * [Enable the extensions feature flag](#enable-the-extensions-feature-flag)
      * [Option 1](#option-1)
      * [Option 2](#option-2)
      * [Option 3](#option-3)
    * [Run OpenSearch](#run-opensearch)
  * [Send a REST request to the extension](#send-a-rest-request-to-the-extension)
* [Developing your own extension](#developing-your-own-extension)
  * [Running a custom extension](#running-a-custom-extension)
  * [Publishing the OpenSearch SDK for Java repo to the Maven local repo](#publishing-the-opensearch-sdk-for-java-repo-to-the-maven-local-repo)
  * [Running tests](#running-tests)
  * [Launching and debugging from an IDE](#launching-and-debugging-from-an-ide)
  * [Generating an artifact](#generating-an-artifact)
  * [Submitting changes](#submitting-changes)

## Getting started

In general, running and using an extension can be broken down into the following steps:

1. Start the extension:
    - [Clone the OpenSearch SDK for Java repository](#clone-the-opensearch-sdk-for-java-repository).
    - Run your own extension or the sample Hello World extension:
      - [Run the sample extension](#run-the-sample-extension).
      - [Run the sample extension with security](#run-the-sample-extension-with-security).
1. Start OpenSearch:
    - [Clone the OpenSearch repository](#clone-the-opensearch-repository).
    - [Enable the extensions feature flag](#enable-the-extensions-feature-flag).
1. Use the extension:
    - [Send a REST request to the extension](#send-a-rest-request-to-the-extension).

Note: You need to first start the extension or extensions and then start OpenSearch.

This tutorial uses the sample Hello World extension included in the `opensearch-sdk-java` repository.

### Start the extension

To start the extension, you need to first clone the OpenSearch SDK for Java repository and then run the extension.

#### Clone the OpenSearch SDK for Java repository

Fork the [OpenSearch SDK for Java](https://github.com/opensearch-project/opensearch-sdk-java) repository and clone it locally using the following command:

```bash
git clone https://github.com/<your username>/opensearch-sdk-java.git
```

#### Run the sample extension

Navigate to the directory to which you cloned the OpenSearch SDK for Java repository.

You can run the sample Hello World extension using the `helloWorld` task:

```bash
./gradlew helloWorld
```

Bound addresses will then be logged to the terminal:

```bash
[main] INFO  transportservice.TransportService - publish_address {127.0.0.1:3333}, bound_addresses {[::1]:3333}, {127.0.0.1:3333}
[main] INFO  transportservice.TransportService - profile [test]: publish_address {127.0.0.1:5555}, bound_addresses {[::1]:5555}, {127.0.0.1:5555}
```

#### Run the sample extension with security

1. Uncomment the SSL settings from [resources/sample/helloworld-settings.yml](src/main/resources/sample/helloworld-settings.yml):
```
ssl.transport.enabled: true
ssl.transport.pemcert_filepath: certs/extension-01.pem
ssl.transport.pemkey_filepath: certs/extension-01-key.pem
ssl.transport.pemtrustedcas_filepath: certs/root-ca.pem
ssl.transport.enforce_hostname_verification: false
path.home: <path/to/extension>
```
2. Follow the instructions in [CERTIFICATE_GENERATION](Docs/CERTIFICATE_GENERATION.md) to generate the certificates.
3. Run the extension using `./gradlew run`.

### Start OpenSearch

Follow these steps to start OpenSearch:
- [Clone the OpenSearch repository](#clone-the-opensearch-repository).
- [Enable the extensions feature flag](#enable-the-extensions-feature-flag).
- [Run OpenSearch](#run-opensearch).

#### Clone the OpenSearch repository

Fork the [OpenSearch](https://github.com/opensearch-project/OpenSearch/) repository and clone it locally using the following command:

```bash
git clone https://github.com/<your username>/OpenSearch.git
```

#### Enable the extensions feature flag

Extensions are experimental in OpenSearch 2.8, so you must enable them either before or when you run OpenSearch. You can enable the feature flag using one of the following options.

##### Option 1

Add the experimental feature system property to `gradle/run.gradle`:

```bash
testClusters {
  runTask {
    testDistribution = 'archive'
    if (numZones > 1) numberOfZones = numZones
    if (numNodes > 1) numberOfNodes = numNodes
    systemProperty 'opensearch.experimental.feature.extensions.enabled', 'true'
  }
}
```

##### Option 2

Add the experimental feature flag as a command line argument:

- `./bin/opensearch -E opensearch.experimental.feature.extensions.enabled=true` when running from a local distribution
- `./gradlew run -Dopensearch.experimental.feature.extensions.enabled=true` when running using Gradle in developer mode

##### Option 3

Enable the experimental feature flag by setting it to `true` in `opensearch.yml`:
- `cd` to your local distribution build for OpenSearch.
- `cd` into the OpenSearch `config` folder and open `opensearch.yml` in your local editor.
- Search for `opensearch.experimental.feature.extensions.enabled`, uncomment it, and set it to `true`.
- Run OpenSearch using `./bin/opensearch` when running from a local distribution.


#### Run OpenSearch

You can run OpenSearch either from a compiled binary or from Gradle.

To **run OpenSearch from a compiled binary**, follow these steps:

- Start a separate terminal and navigate to the directory where OpenSearch has been cloned using `cd OpenSearch`.
- Run `./gradlew assemble` to create a local distribution.
- Start OpenSearch using `./bin/opensearch`. Ensure that extensions feature f
- Send the below sample REST API to initialize an extension
```bash
curl -XPOST "localhost:9200/_extensions/initialize" -H "Content-Type:application/json" --data '{
"name":"hello-world",
"uniqueId":"hello-world",
"hostAddress":"127.0.0.1",
"port":"4532",
"version":"1.0",
"opensearchVersion":"3.0.0",
"minimumCompatibleVersion":"3.0.0",
"dependencies":[{"uniqueId":"test1","version":"2.0.0"},{"uniqueId":"test2","version":"3.0.0"}] \
}'
```

To **run OpenSearch from Gradle**, follow these steps:
- Run `./gradlew run` to start OpenSearch.
- Send the below sample REST API to initialize an extension
```bash
curl -XPOST "localhost:9200/_extensions/initialize" -H "Content-Type:application/json" --data '{
"name":"hw",
"uniqueId":"hello-world",
"hostAddress":"127.0.0.1",
"port":"4532",
"version":"1.0",
"opensearchVersion":"3.0.0",
"minimumCompatibleVersion":"3.0.0",
"dependencies":[{"uniqueId":"test1","version":"2.0.0"},{"uniqueId":"test2","version":"3.0.0"}]
}'
```

Note: If Security plugin is initialized in OpenSearch, use admin credentials to send extension initialization request.

In response to the REST `/initialize` request, `ExtensionsManager` discovers the extension listening on a predefined port and executes the TCP handshake protocol to establish a data transfer connection. Then OpenSearch sends a request to the OpenSearch SDK for Java and, upon acknowledgment, the extension responds with its name. This name is logged in the terminal where OpenSearch is running:

```bash
[2022-06-16T21:30:18,857][INFO ][o.o.t.TransportService   ] [runTask-0] publish_address {127.0.0.1:9300}, bound_addresses {[::1]:9300}, {127.0.0.1:9300}
[2022-06-16T21:30:18,978][INFO ][o.o.t.TransportService   ] [runTask-0] Action: internal:transport/handshake
[2022-06-16T21:30:18,989][INFO ][o.o.t.TransportService   ] [runTask-0] TransportService:sendRequest action=internal:discovery/extensions
[2022-06-16T21:30:18,989][INFO ][o.o.t.TransportService   ] [runTask-0] Action: internal:discovery/extensions
[2022-06-16T21:30:19,000][INFO ][o.o.e.ExtensionsManager] [runTask-0] received PluginResponse{examplepluginname}
```

The OpenSearch SDK terminal also logs all requests and responses it receives from OpenSearch:

- TCP handshake request:

```bash
21:30:18.943 [opensearch[extension][transport_worker][T#7]] TRACE org.opensearch.latencytester.transportservice.netty4.OpenSearchLoggingHandler - [id: 0x37b22600, L:/127.0.0.1:4532 - R:/127.0.0.1:47766] READ: 55B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 45 53 00 00 00 31 00 00 00 00 00 00 00 01 08 08 |ES...1..........|
|00000010| 1e ab f3 00 00 00 1a 00 00 00 16 69 6e 74 65 72 |...........inter|
|00000020| 6e 61 6c 3a 74 63 70 2f 68 61 6e 64 73 68 61 6b |nal:tcp/handshak|
|00000030| 65 00 04 a3 8e b7 41                            |e.....A         |
+--------+-------------------------------------------------+----------------+
MESSAGE RECEIVED:E«󀀀internal:tcp/handshake£·A
```

- Extension name request/response:

```bash
21:30:18.992 [opensearch[extension][transport_worker][T#6]] TRACE org.opensearch.latencytester.transportservice.netty4.OpenSearchLoggingHandler - [id: 0xb2be651b, L:/127.0.0.1:4532 - R:/127.0.0.1:47782] READ: 204B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 45 53 00 00 00 c6 00 00 00 00 00 00 00 03 00 08 |ES..............|
|00000010| 2d c7 23 00 00 00 21 00 00 00 1d 69 6e 74 65 72 |-.#...!....inter|
|00000020| 6e 61 6c 3a 64 69 73 63 6f 76 65 72 79 2f 65 78 |nal:discovery/ex|
|00000030| 74 65 6e 73 69 6f 6e 73 00 00 0e 6e 6f 64 65 5f |tensions...node_|
|00000040| 65 78 74 65 6e 73 69 6f 6e 16 51 53 74 39 6f 4b |extension.QSt9oK|
|00000050| 58 46 54 53 57 71 67 58 34 62 6b 56 6a 47 2d 51 |XFTSWqgX4bkVjG-Q|
|00000060| 09 31 32 37 2e 30 2e 30 2e 31 09 31 32 37 2e 30 |.127.0.0.1.127.0|
|00000070| 2e 30 2e 31 04 7f 00 00 01 09 31 32 37 2e 30 2e |.0.1......127.0.|
|00000080| 30 2e 31 00 00 11 b4 00 04 0f 63 6c 75 73 74 65 |0.1.......cluste|
|00000090| 72 5f 6d 61 6e 61 67 65 72 01 6d 00 04 64 61 74 |r_manager.m..dat|
|000000a0| 61 01 64 01 06 69 6e 67 65 73 74 01 69 00 15 72 |a.d..ingest.i..r|
|000000b0| 65 6d 6f 74 65 5f 63 6c 75 73 74 65 72 5f 63 6c |emote_cluster_cl|
|000000c0| 69 65 6e 74 01 72 00 a3 8e b7 41 00             |ient.r....A.    |
+--------+-------------------------------------------------+----------------+
MESSAGE RECEIVED:ES-ǣ!internal:discovery/extensionsnode_extensionQSt9oKXFTSWqgX4bkVjG-Q 127.0.0.1       127.0.0.1  127.0.0.1´cluster_managermdatadingestiremote_cluster_clientr£·A
21:30:18.993 [opensearch[extension][transport_worker][T#6]] TRACE org.opensearch.transport.TransportLogger - Netty4TcpChannel{localAddress=/127.0.0.1:4532, remoteAddress=/127.0.0.1:47782} [length: 204, request id: 3, type: request, version: 3.0.0, action: internal:discovery/extensions] READ: 204B
21:30:18.993 [opensearch[extension][transport_worker][T#6]] TRACE org.opensearch.transport.TransportService.tracer - [3][internal:discovery/extensions] received request
21:30:18.996 [opensearch[extension][generic][T#1]] TRACE org.opensearch.tasks.TaskManager - register 2 [transport] [internal:discovery/extensions] []
21:30:18.997 [opensearch[extension][generic][T#1]] TRACE org.opensearch.tasks.TaskManager - unregister task for id: 2
21:30:18.997 [opensearch[extension][generic][T#1]] TRACE org.opensearch.transport.TransportLogger - Netty4TcpChannel{localAddress=/127.0.0.1:4532, remoteAddress=/127.0.0.1:47782} [length: 48, request id: 3, type: response, version: 3.0.0, header size: 2B] WRITE: 48B
21:30:18.998 [opensearch[extension][transport_worker][T#6]] TRACE org.opensearch.latencytester.transportservice.netty4.OpenSearchLoggingHandler - [id: 0xb2be651b, L:/127.0.0.1:4532 - R:/127.0.0.1:47782] WRITE: 48B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 45 53 00 00 00 2a 00 00 00 00 00 00 00 03 01 08 |ES...*..........|
|00000010| 2d c7 23 00 00 00 02 00 00 16 4e 61 6d 65 64 57 |-.#.......exampl|
|00000020| 72 69 74 65 61 62 6c 65 52 65 67 69 73 74 72 79 |epluginname     |
+--------+-------------------------------------------------+----------------+
21:30:18.999 [opensearch[extension][transport_worker][T#6]] TRACE org.opensearch.latencytester.transportservice.netty4.OpenSearchLoggingHandler - [id: 0xb2be651b, L:/127.0.0.1:4532 - R:/127.0.0.1:47782] FLUSH
21:30:18.999 [opensearch[extension][transport_worker][T#6]] TRACE org.opensearch.transport.TransportService.tracer - [3][internal:discovery/extensions] sent response
```

It is important to ensure that the OpenSearch SDK for Java is already running on a separate process.

### Send a REST request to the extension

The following request is configured to be handled by the sample `HelloWorldExtension` (note that its matching `uniqueId` is `opensearch-sdk-java-1`):
```bash
curl -X GET localhost:9200/_extensions/_opensearch-sdk-java-1/hello
```

## Developing your own extension

Before you write your own extension, read through the [design documentation](DESIGN.md) to learn about extension architecture and class hierarchy. Then follow [this guide](CREATE_YOUR_FIRST_EXTENSION.md) to develop your own extension. For an example, see the sample Hello World extension in the `org.opensearch.sdk.sample.helloworld` package.

Refer to the following sections for information about post-development tasks.

### Running a custom extension

To run an extension that uses the SDK, use `./gradlew run` on that extension.

### Publishing the OpenSearch SDK for Java repo to the Maven local repo

Until we publish this repo to the Maven Central Repository, publishing to the Maven local repository is how extensions (outside of sample packages) import the artifacts:

```bash
./gradlew publishToMavenLocal
```

### Running tests

Use the following command to run tests:

```
./gradlew clean build integTest
```

### Launching and debugging from an IDE

For information about launching and debugging from an IDE in OpenSearch, see [this document](https://github.com/opensearch-project/OpenSearch/blob/main/TESTING.md#launching-and-debugging-from-an-ide)

### Generating an artifact

In `opensearch-sdk-java`, navigate to `build/distributions`. Look for the tarball in the form `opensearch-sdk-java-1.0.0-SNAPSHOT.tar`. If there is no such tarball, use the following command to create one:
```bash
./gradlew clean && ./gradlew build
```
Once the tarball is generated, navigate to `/src/test/resources/sample` and look for `extension-settings.yml`. If the file is not present, create it.
The tarball is generated in `/build/distributions`. To run the artifact (the tarball), use the following command:
```bash
tar -xvf opensearch-sdk-java-1.0.0-SNAPSHOT.tar
```

The artifact will include extension settings for the sample Hello World extension on the class path under the path `/sample/extension-settings.yml`:

```yaml
  extensionName: hello-world
  hostAddress: 127.0.0.1
  hostPort: 4532
  opensearchAddress: 127.0.0.1
  opensearchPort: 9200
```

Start the sample extension with `./bin/opensearch-sdk-java`

### Submitting changes

To learn how to submit your changes, see [CONTRIBUTING](CONTRIBUTING.md).
