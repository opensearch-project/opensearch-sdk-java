# Extensions Security Guidelines

OpenSearch's support for extensions allows for taking already powerful use cases and expanding on them. With this increased functionality comes a larger surface area for misuse, vunerabilities, and malicious interactions.

By capturing the current state of OpenSearch ecocsystem and the plans for extensions this document outlines several areas for enhancements, features, and practices to incorperate into extensions for OpenSearch.


To keep concepts consistant, this document is using terminology from [NIST Glossary](https://csrc.nist.gov/glossary).

Additional terms:
* **Plugin** - reference to the existing functionality to extend OpenSearch functionality.
* **Extension** - reference to the in development functionality to extend OpenSearch.

## Host security

Plugins depend on use of the Java Security Manager is use to limit interactions on the host operation system resources (cpu/disk/memory/network/...).  JSM has been deprecated, with its removal scheduled in the next release of the JVM, see [OpenSearch discussion](https://github.com/opensearch-project/OpenSearch/issues/1687). Additional measures are needed to protect system resources.

Extensions are sandboxed from the host system by operating via REST APIs.  This security boundary isolates extensions from executing operation system calls directly on OpenSearch hosts.

## Communications security (COMSEC)

Plugins are loaded into the same java virtual machine instance allowing communicate to OpenSearch through in process java APIs.  Plugins can issue REST API requests to the OpenSearch hosts reusing the standard node-to-node communications, internally called the transport client.

Extensions of OpenSearch communicate via https requests between the nodes on the cluster and the extensions endpoint(s).  This is a bi-direction communication also allows extensions to contact the OpenSearch cluster through its avaliable REST APIs.

## Data Security

OpenSearch stores data in memory and local file system storage.  This data is stored unencrypted.

Plugins can use the existing data systems of the OpenSearch.  Several classes of plugins extend storage options out to external services.

### Access Control

With the security plugin installed, role based access control (RBAC) is available with a proprietary policy document format.  Access control over native OpenSearch data is possible with this plugin installed.

For resource that are managed by plugins, access control is governed within individual plugin, by examining [user](https://github.com/opensearch-project/common-utils/blob/main/src/main/java/org/opensearch/commons/authuser/User.java) object from OpenSearch's thread context permissions are avaliable for approval/denial. Example from anomaly detection, [checkUserPermissions](https://github.com/opensearch-project/anomaly-detection/blob/875b03c1c7596cb34d74fea285c28d949cfb0d19/src/main/java/org/opensearch/ad/util/ParseUtils.java#L568).  Uniform resource controls and models are needed to protect from misconfiguration and code defects.

As Extensions do not have access OpenSearch's thread context, identity and its associated prileveages must be communicated through the REST APIs.

## Auditing

With the security plugin installed, when actions are performed on the OpenSearch cluster they are recorded if filtering criteria are meet to configurable audit log sinks.

## Installation

Plugin installation is managed by using a binary on the node that extract plugin.zip files into the file system, this is done outside the active running of OpenSearch itself.  When OpenSearch starts it loads installed plugins into its JVM runtime.

## Versioning

OpenSearch systems have ways to deprecate unsupported patterns, feature, and APIs.

## Configuration

Configuration of OpenSearch is split between on disk yml files and various in OpenSearch systems such as cluster settings.

Plugins configuration is loaded and checked at service startup time for correctness.  If there is an error OpenSearch can fail to start.

## Reliability

OpenSearch plugins can create node instability if incorrectly configured, or there are code defects.