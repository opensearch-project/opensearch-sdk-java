# Extensions Security Guidelines

OpenSearch's support for extensions allows for taking already powerful use cases and expanding on them. With this increased functionality comes a larger surface area for misuse, vulnerabilities, and malicious interactions.

By capturing the current state of OpenSearch ecosystem  and the plans for extensions this document outlines several areas for enhancements, features, and practices to incorporate into extensions for OpenSearch.

## Document Phases
These guidelines and this document are meant to evolve, the follow list captures the different phases this document will undergo.  Some areas might be complete ahead of others. Some areas or items might be marked as invalid/removed using markdown's strike-through.

1. [X] Agreement of areas and 'as-is' state of OpenSearch Plugins and Extensions. **<-- Doc is here**
2. [ ] All areas have recommendations and areas of investigation are filed as issues and linked back on this document.
3. [ ] All investigation conclusions are captured and linked in this document, effectively define the scope of work for these areas.  Implementation of work is can be completed or outstanding.
4. [ ] All planned work has been completed, issues around this work can be completed or outstanding. 
5. [ ] Document complete, future work and issue will be captured out of band instead of as updates this document.

## Terms
To keep concepts consistent, this document is using terminology from [NIST Glossary](https://csrc.nist.gov/glossary).

Additional terms:
* **Plugin** - reference to the existing functionality to extend OpenSearch functionality.
* **Extension** - reference to the in development functionality to extend OpenSearch.

# Areas

## Host security

Plugins depend on use of the Java Security Manager (JSM) to limit interactions on the host operation system resources (cpu/disk/memory/network/...).  JSM has been deprecated, with its removal scheduled in the next release of the JVM, see [OpenSearch discussion](https://github.com/opensearch-project/OpenSearch/issues/1687). Additional measures are needed to protect system resources.

Extensions are sandboxed from the host system by operating via REST APIs.  This security boundary isolates extensions from executing operation system calls directly on OpenSearch hosts.

## Communications security (COMSEC)

Plugins are loaded into the same java virtual machine instance allowing communicate to OpenSearch through in process java APIs.  Plugins can issue REST API requests to the OpenSearch hosts reusing the standard node-to-node communications, internally called the transport client.

Extensions of OpenSearch communicate via https requests between the nodes on the cluster and the extensions endpoint(s).  This is a bi-direction communication also allows extensions to contact the OpenSearch cluster through its available REST APIs.

## Data Security

OpenSearch stores data in memory and local file system storage.  This data is stored unencrypted.

Plugins can use the existing data systems of the OpenSearch.  Several classes of plugins extend storage options out to external services.

### Access Control

With the security plugin installed, role based access control (RBAC) is available with a policy document format specific to OpenSearch.  Access control over native OpenSearch data is possible with this plugin installed.

For resource that are managed by plugins, access control is governed within individual plugin. By examining [user](https://github.com/opensearch-project/common-utils/blob/main/src/main/java/org/opensearch/commons/authuser/User.java) object from OpenSearch's thread context permissions are available for approval/denial. An example from anomaly detection is [checkUserPermissions](https://github.com/opensearch-project/anomaly-detection/blob/875b03c1c7596cb34d74fea285c28d949cfb0d19/src/main/java/org/opensearch/ad/util/ParseUtils.java#L568).  Uniform resource controls and models are needed to protect from misconfiguration and code defects.

As Extensions do not have access OpenSearch's thread context, identity and its associated privileges must be communicated through the REST APIs.

## Auditing

With the security plugin installed, when actions are performed on the OpenSearch cluster they are recorded if filtering criteria are meet to configurable audit log sinks.

## Installation

Plugin installation is managed by using a binary on the node that extract plugin zip files into the file system, this is done outside the active running of OpenSearch itself.  When OpenSearch starts it loads installed plugins into its JVM runtime.

Extensions installation is managed through on disk configuration. 

## Versioning

OpenSearch has a version number following [semver](https://semver.org/).

Plugins for OpenSearch must match their version exactly the version of OpenSearch.  Older version numbers are not compatible, so to resolve CVE in OpenSearch or in plugins - all components be re-released.

Extensions version information is not tied to OpenSearch's version, extensions and OpenSearch are able to independantly release minor/patch versions to address CVEs.

## Configuration

Configuration of OpenSearch uses on disk yml configuration files.  Other settings are manage in-memory through settings that are modifiable at runtime through APIs or indirectly.

Plugins configuration is managed through the same systems as OpenSearch.

Extensions configuration setup is tied to OpenSearch settings, extensions configuration are managed independently of OpenSearch.

## Reliability

OpenSearch plugins can create cluster or node instability if incorrectly configured or by software defects.