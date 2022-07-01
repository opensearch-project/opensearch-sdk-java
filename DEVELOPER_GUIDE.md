
# OpenSearch SDK Developer Guide
- [Introduction](#introduction)
- [Getting Started](#getting-started)
	- [Git Clone OpenSearch-SDK Repo](#git-clone-OpenSearch-SDK-repo)
	- [Git Clone OpenSearch Repo](#git-clone-opensearch-repo)
	- [Publish OpenSearch Feature/Extensions branch to Maven local](#publish-opensearch-feature/extensions-branch-to-maven-local)
	- [Run OpenSearch-SDK](#run-opensearch-sdk)
	- [Run OpenSearch](#run-opensearch)
	- [Run Tests](#run-tests)
    - [Submitting Changes](#submitting-changes)

## Introduction
Opensearch plugins have allowed the extension and ehancements of various core features however, current plugin architecture carries the risk of fatally impacting clusters should they fail. In order to ensure that plugins may run safely without impacting the system, our goal is to effectively isolate plugin interactions with OpenSearch by modularizing the [extension points](https://opensearch.org/blog/technical-post/2021/12/plugins-intro/) to which they hook onto. 

Read more about extensibility [here](https://github.com/opensearch-project/OpenSearch/issues/1422)

## Getting Started

### Git Clone OpenSearch SDK Repo
Fork [OpenSearch SDK](https://github.com/opensearch-project/opensearch-sdk) and clone locally, e.g. `git clone https://github.com/[your username]/opensearch-sdk.git`.

### Git Clone OpenSearch Repo
Fork [OpenSearch](https://github.com/opensearch-project/OpenSearch) and clone locally, e.g. `git clone https://github.com/[your username]/OpenSearch.git`.

## Publish OpenSearch feature/extensions Branch to Maven local
The work done to support the extensions framework is located on the `feature/extensions` branch of the OpenSearch project. It is necessary to publish the dependencies of this branch to your local maven repository prior to running the OpenSearch SDK on a seperate process. 

- First navigate to the directory that OpenSearch has been cloned to
- Checkout the correct branch, e.g. `git checkout feature/extensions`.
- Run `./gradlew publishToMavenLocal`. 
- Run `./gradlew check` to make sure the build is successful.

It is necessary to publish dependencies to a local maven repository until this branch is merged to `main`, at which point all dependencies will be published to Maven central.

## Run OpenSearch SDK

Navigate to the directory that OpenSearch-SDK has been cloned to and run main script using `./gradlew run`.

```
./gradlew run
```

This will execute the main script set within the root `build.gradle` file :

```
mainClassName = 'transportservice.ExtensionsRunner'
```
Bound addresses will then be logged to the terminal :

```bash
[main] INFO  transportservice.TransportService - publish_address {127.0.0.1:3333}, bound_addresses {[::1]:3333}, {127.0.0.1:3333}
[main] INFO  transportservice.TransportService - profile [test]: publish_address {127.0.0.1:5555}, bound_addresses {[::1]:5555}, {127.0.0.1:5555}
```

## Create extensions.yml file

Every extension will require metadata stored in an extensions.yml file in order to be loaded successfully.  In order to make the SDK look like an extension within OpenSearch, there must be an entry for the SDK within `extensions.yml`.

- Start a separate terminal and navigate to the directory that OpenSearch has been cloned to using `cd OpenSearch`.

- Check if extensions directory exists in OpenSearch using `ls`.
- If the directory does not exist, create it using `mkdir extensions`.
- Navigate to the extensions folder using `cd extensions`.
- Manually create a file titled `extensions.yml` within the extensions directory using an IDE or an in-line text editor.

Sample extensions.yml file:

```
extensions:
  - name: opensearch-sdk
    uniqueId: opensearch-sdk-1
    hostName: 'sdk_host'
    hostAddress: '127.0.0.1'
    port: '4532'
    version: '1.0'
    description: Extension for the Opensearch SDK Repo
    opensearchVersion: '3.0.0'
    javaVersion: '14'
    className: sdk
    customFolderName: opensearch-sdk
    hasNativeController: false	
```

## Run OpenSearch

- Return to the OpenSearch directory by using `cd ..`.
- Start OpenSearch feature/extensions branch using `./bin/opensearch`.

During OpenSearch bootstrap, `ExtensionsOrchestrator` will then discover the extension listenening on a pre-defined port and execute the TCP handshake protocol to establish a data transfer connection. A request will be sent to the OpenSearch SDK and upon acknowledgment, the extension will respond with its name which will be logged onto terminal that OpenSearch is running on.

```
[2022-06-16T21:30:18,857][INFO ][o.o.t.TransportService   ] [runTask-0] publish_address {127.0.0.1:9300}, bound_addresses {[::1]:9300}, {127.0.0.1:9300}
[2022-06-16T21:30:18,978][INFO ][o.o.t.TransportService   ] [runTask-0] Action: internal:transport/handshake
[2022-06-16T21:30:18,989][INFO ][o.o.t.TransportService   ] [runTask-0] TransportService:sendRequest action=internal:discovery/extensions
[2022-06-16T21:30:18,989][INFO ][o.o.t.TransportService   ] [runTask-0] Action: internal:discovery/extensions
[2022-06-16T21:30:19,000][INFO ][o.o.e.ExtensionsOrchestrator] [runTask-0] received PluginResponse{examplepluginname}
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

It is important that the OpenSearch SDK is already up and running on a seperate process prior to starting OpenSearch, since extension discovery occurs only if the OpenSearch SDK is already listening on a pre-defined port. Once discovery is complete and the data transfer connection between both nodes has been established, OpenSearch and the OpenSearch SDK will now be able to comminicate. 

## Run Tests

Run tests :
```
./gradlew clean build test
```

## Submitting Changes

See [CONTRIBUTING](CONTRIBUTING.md).
