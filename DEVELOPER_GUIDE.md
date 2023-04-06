
# OpenSearch SDK for Java Developer Guide

- [Introduction](#introduction)
- [Getting Started](#getting-started)
    - [Clone OpenSearch SDK for Java Repository](#clone-opensearch-sdk-for-java-repository)
        - [Run the Sample Extension](#run-the-sample-extension)
    - [Clone OpenSearch Repository](#clone-opensearch-repository)
        - [Enable Extensions Feature Flag](#enable-extensions-feature-flag)
        - [Create extensions.yml file](#create-extensions-yml-file)
        - [Run OpenSearch](#run-opensearch)
    - [Publish OpenSearch SDK for Java to Maven Local](#publish-opensearch-sdk-for-java-to-maven-local)
    - [Perform a REST Request on the Extension](#perform-a-rest-request-on-the-extension)
    - [Run Tests](#run-tests)
    - [Submitting Changes](#submitting-changes)

## Introduction

OpenSearch plugins have allowed the extensibility of various core features. However, the current plugin architecture is tightly coupled with OpenSearch. This creates barriers to innovation and carries the risk of fatally impacting clusters should the plugins fail. In order to ensure that extended functionality may run safely without impacting the system, our goal is to effectively isolate interactions with OpenSearch by modularizing the [extension points](https://opensearch.org/blog/technical-post/2021/12/plugins-intro/) to which they hook onto.

Read more about extensibility [here](https://github.com/opensearch-project/OpenSearch/issues/1422)

## Getting Started

Presently you need to start up the extension(s) first, and then start OpenSearch.

### Clone OpenSearch SDK for Java Repository

Fork [OpenSearch SDK for Java](https://github.com/opensearch-project/opensearch-sdk-java) and clone locally, e.g. `git clone https://github.com/[your username]/opensearch-sdk-java.git`.

#### Run the Sample Extension

Navigate to the directory that OpenSearch SDK for Java has been cloned to.

You can execute the sample Hello World extension using the `helloWorld` task:

```
./gradlew helloWorld
```

Bound addresses will then be logged to the terminal :

```bash
[main] INFO  transportservice.TransportService - publish_address {127.0.0.1:3333}, bound_addresses {[::1]:3333}, {127.0.0.1:3333}
[main] INFO  transportservice.TransportService - profile [test]: publish_address {127.0.0.1:5555}, bound_addresses {[::1]:5555}, {127.0.0.1:5555}
```

#### Running other extensions

If you are running an extension that uses the SDK, you may simply use `./gradlew run` on that extension.

#### Publish OpenSearch SDK for Java to Maven local

Until we publish this repo to maven central, publishing to maven local is the way for extensions (outside the sample packages) to import the artifacts:
```
./gradlew publishToMavenLocal
```

### Clone OpenSearch Repository

Fork [OpenSearch](https://github.com/opensearch-project/OpenSearch/), clone locally, e.g., `git clone https://github.com/[your username]/OpenSearch.git`.

#### Enable Extensions Feature Flag

##### Option 1
Add the experimental feature system property to `gradle/run.gradle` to enable extensions:

```
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
Add the experimental feature flag as a command line argument
- `./bin/opensearch -E opensearch.experimental.feature.extensions.enabled=true` when running from a local distribution
- `./gradlew run -Dopensearch.experimental.feature.extensions.enabled=true` when running using gradle in developer mode

## Create extensions.yml file

Every extension will require metadata stored in an extensions.yml file in order to be loaded successfully.  In order to make the SDK look like an extension within OpenSearch, there must be an entry for the SDK within `extensions.yml`.

To run OpenSearch from a compiled binary:
- Start a separate terminal and navigate to the directory that OpenSearch has been cloned to using `cd OpenSearch`.
- Run `./gradlew assemble` to create a local distribution.
- Navigate to the project root directory (i.e. `cd distribution/archives/linux-tar/build/install/opensearch-3.0.0-SNAPSHOT/`). Note: On Mac OS `linux-tar` should be replaced with `darwin-tar`.

- Check if extensions directory exists in OpenSearch using `ls`.
- If the directory does not exist, create it using `mkdir extensions`.
- Navigate to the extensions folder using `cd extensions`.
- Manually create a file titled `extensions.yml` within the extensions directory using an IDE or an in-line text editor.

- Return to the OpenSearch directory by using `cd ..`.
- Start OpenSearch using `./bin/opensearch`.

To run OpenSearch from gradle:
- Copy the `extensions.yml` file to the same directory as indicated above.
- Run `./gradlew run` to start OpenSearch. A log entry will indicate the location it is searching for `extensions.yml`.

A sample `extensions.yml` file is shown below. The `uniqueId` will be used in REST paths. The name must match the `extensionName` field in the corresponding `extension.yml`:

```
extensions:
  - name: hello-world
    uniqueId: opensearch-sdk-java-1
    hostAddress: '127.0.0.1'
    port: '4532'
    version: '1.0'
    opensearchVersion: '3.0.0'
    minimumCompatibleVersion: '3.0.0'
```

#### Run OpenSearch

During OpenSearch bootstrap, `ExtensionsManager` will then discover the extension listenening on a pre-defined port and execute the TCP handshake protocol to establish a data transfer connection. A request will be sent to OpenSearch SDK for Java and upon acknowledgment, the extension will respond with its name which will be logged onto terminal that OpenSearch is running on.

```
[2022-06-16T21:30:18,857][INFO ][o.o.t.TransportService   ] [runTask-0] publish_address {127.0.0.1:9300}, bound_addresses {[::1]:9300}, {127.0.0.1:9300}
[2022-06-16T21:30:18,978][INFO ][o.o.t.TransportService   ] [runTask-0] Action: internal:transport/handshake
[2022-06-16T21:30:18,989][INFO ][o.o.t.TransportService   ] [runTask-0] TransportService:sendRequest action=internal:discovery/extensions
[2022-06-16T21:30:18,989][INFO ][o.o.t.TransportService   ] [runTask-0] Action: internal:discovery/extensions
[2022-06-16T21:30:19,000][INFO ][o.o.e.ExtensionsManager] [runTask-0] received PluginResponse{examplepluginname}
```

OpenSearch SDK terminal will also log all requests and responses it receives from OpenSearch :

TCP HandShake Request :

```
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

Extension Name Request / Response :

```
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

It is important that the OpenSearch SDK for Java is already up and running on a separate process prior to starting OpenSearch, since extension discovery occurs only if the OpenSearch SDK for Java is already listening on a pre-defined port. Once discovery is complete and the data transfer connection between both nodes has been established, OpenSearch and the OpenSearch SDK for Java will now be able to communicate.

## Perform a REST Request on the Extension

The following request is configured to be handled by the sample `HelloWorldExtension` (note the matching uniqueId):
```
curl -X GET localhost:9200/_extensions/_opensearch-sdk-java-1/hello
```

## Run Tests

Run tests :
```
./gradlew clean build test
```
## Generate Artifact

In opensearch-sdk-java navigate to build/distributions. Look for tar ball in the form `opensearch-sdk-java-1.0.0-SNAPSHOT.tar`. If not found follow the below steps to create one:
```
./gradlew clean && ./gradlew build
```
Once the tar ball is generated navigate to `/src/test/resources/sample` and look for `extension-settings.yml`. Create one if not present
Look for tar ball in `/build/distributions`. To run the artifact i.e., tar ball, run the below command
```
tar -xvf opensearch-sdk-java-1.0.0-SNAPSHOT.tar
```

The artifact will include extension settings for the sample extension on the class path under the path `/sample/extension-settings.yml`. This path is used by the sample `HelloWorldExtension`.

```
  extensionName: hello-world
  hostAddress: 127.0.0.1
  hostPort: 4532
  opensearchAddress: 127.0.0.1
  opensearchPort: 9200
```
- Start the sample extension with `./bin/opensearch-sdk-java`

## Submitting Changes

See [CONTRIBUTING](CONTRIBUTING.md).
