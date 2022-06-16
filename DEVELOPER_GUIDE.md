
# OpenSearch SDK Developer Guide
- [Introduction](#introduction)
- [Getting Started](#getting-started)
	- [Git Clone OpenSearch-SDK Repo](#git-clone-OpenSearch-SDK-repo)
	- [Git Clone OpenSearch Repo](#git-clone-opensearch-repo)
	- [Publish OpenSearch Feature/Extensions branch to Maven local](#publish-opensearch-feature/extensions-branch-to-maven-local)
	- [Run Extension](#run-extension)
	- [Run Tests](#run-tests)
	- [Send Message using Telnet](#send-message-using-telnet)

## Introduction
Opensearch plugins have allowed the extension and ehancements of various core features however, current plugin architecture carries the risk of fatally impacting clusters should they fail. In order to ensure that plugins may run safely without impacting the system, our goal is to effectively isolate plugin interactions with OpenSearch by modularizing the [extension points](https://opensearch.org/blog/technical-post/2021/12/plugins-intro/) to which they hook onto. 

Read more about extensibility [here](https://github.com/opensearch-project/OpenSearch/issues/1422)

## Getting Started

### Git Clone OpenSearch SDK Repo
Fork [OpenSearch SDK](https://github.com/opensearch-project/opensearch-sdk) and clone locally, e.g. `git clone https://github.com/[your username]/opensearch-sdk.git`.

### Git Clone OpenSearch Repo
Fork [OpenSearch](https://github.com/opensearch-project/OpenSearch) and clone locally, e.g. `git clone https://github.com/[your username]/OpenSearch.git`.

## Publish OpenSearch feature/extensions Branch to Maven local
The work done to support the extensions framework is located on the `feature/extensions` branch of the OpenSearch project. It is necessary to publish the dependencies of this branch to your local maven repository prior to running the extension on a seperate process. 

- First navigate to the directory that OpenSearch has been cloned to
- Checkout the correct branch, e.g. `git checkout feature/extensions`.
- Run `./gradlew publishToMavenLocal`. 

It is necessary to publish dependencies to a local maven repository until this branch is merged to `main`, at which point all dependencies will be published to Maven central.

### Run OpenSearch SDK

Navigate to the directory that OpenSearch-SDK has been cloned to and run main script using `gradlew run`.

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

Once the bound addresses are logged to the terminal, navigate to the directory that OpenSearch has been cloned to and start OpenSearch feature/extensions branch using `./gradlew run`.
It is important that the extension is already up and running prior to starting OpenSearch, since extension discovery occurs only if the extension is already listening on a pre-defined port. Once discovery is complete and the data transfer connection between both nodes has been established, OpenSearch and the extension will now be able to comminicate. 

### Run Tests

Run tests :
```
./gradlew clean build test
```

### Send Message using Telnet

To send a message, first run the OpenSearch-SDK :

```
./gradlew run
```
In another terminal, run : 
```
telnet localhost 5555
```
Once Telnet Client is connected, the terminal will print out :
```
Trying 127.0.0.1...
Connected to localhost.
Escape character is '^]'.
```
The original terminal used to run the OpenSearch-SDK will log the connection request :
```
[opensearch[NettySizeHeaderFrameDecoderTests][transport_worker][T#5]] TRACE transportservice.TcpTransport - Tcp transport channel accepted: Netty4TcpChannel{localAddress=/127.0.0.1:5555, remoteAddress=/127.0.0.1:57302}
[opensearch[NettySizeHeaderFrameDecoderTests][transport_worker][T#5]] TRACE transportservice.netty4.OpenSearchLoggingHandler - [id: 0x8c1cc239, L:/127.0.0.1:5555 - R:/127.0.0.1:57302] REGISTERED
[opensearch[NettySizeHeaderFrameDecoderTests][transport_worker][T#5]] TRACE transportservice.netty4.OpenSearchLoggingHandler - [id: 0x8c1cc239, L:/127.0.0.1:5555 - R:/127.0.0.1:57302] ACTIVE
```
Messages sent through the Telnet Client must begin with an 'ES', for example : 
```
ES1234SHDDF
```
The original terminal will then log the recieved message if format is validated :
```
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 45 53 31 32 33 34 53 48 44 44 46 0d 0a          |ES1234SHDDF..   |
+--------+-------------------------------------------------+----------------+
MESSAGE RECEIVED:ES1234SHDDF

REFERENCE LENGTH 13 ES1234SHDDF
```


