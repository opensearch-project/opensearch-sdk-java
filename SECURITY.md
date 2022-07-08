- [Extensions Security Guidelines](#extensions-security-guidelines)
  - [Document Phases](#document-phases)
  - [Terms](#terms)
- [Areas](#areas)
  - [Host security](#host-security)
  - [Communications security (COMSEC)](#communications-security-comsec)
  - [Data Security](#data-security)
    - [Access Control](#access-control)
  - [Auditing](#auditing)
  - [Installation](#installation)
  - [Versioning](#versioning)
  - [Configuration](#configuration)
  - [Reliability](#reliability)
- [Projects](#projects)
  - [Anomaly Detection Plugin](#anomaly-detection-plugin)
    - [User identity OpenSearch#3846 :negative_squared_cross_mark:](#user-identity-opensearch3846-negative_squared_cross_mark)
      - [Aquiring User objects sdk#37](#aquiring-user-objects-sdk37)
      - [Resource user/role checks sdk#40](#resource-userrole-checks-sdk40)
    - [Delegate Authority  OpenSearch#3850 :negative_squared_cross_mark:](#delegate-authority--opensearch3850-negative_squared_cross_mark)
      - [Extension identity sdk#41](#extension-identity-sdk41)
      - [Delayed action API sdk#42](#delayed-action-api-sdk42)

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
* **Plugin** - reference to the existing functionality to extend OpenSearch functionality. Learn more from [Introduction to OpenSearch Plugins](https://opensearch.org/blog/technical-post/2021/12/plugins-intro/).
* **Extension** - reference to the in development functionality to extend OpenSearch. Learn more from [Modular architecture in OpenSearch](https://github.com/opensearch-project/OpenSearch/issues/1422).

# Areas

## Host security

Plugins depend on use of the Java Security Manager (JSM) to limit interactions on the host operation system resources (cpu/disk/memory/network/...).  JSM has been deprecated, with its removal scheduled in the next release of the JVM, see [OpenSearch discussion](https://github.com/opensearch-project/OpenSearch/issues/1687).   Additional measures are needed to protect system resources.

Extensions are sandboxed from the host system by operating via APIs.  This security boundary isolates extensions from executing operation system calls directly on OpenSearch hosts.

## Communications security (COMSEC)

Plugins are loaded into the same java virtual machine instance allowing communicate to OpenSearch through in-process java APIs.  Plugins can issue API requests to the OpenSearch hosts reusing the standard node-to-node communications, internally called the transport client.

Extensions of OpenSearch communicate via https requests between the nodes on the cluster and the extensions endpoint(s).  This is a bi-direction communication also allows extensions to contact the OpenSearch cluster through its available APIs.

* :warning: The communication protocal has not been locked-in, following up with [Extensions to OpenSearch communication #34](https://github.com/opensearch-project/opensearch-sdk/issues/34).

## Data Security

OpenSearch stores data in memory and local file system storage.  This data is stored unencrypted.

Plugins can use the existing data systems of the OpenSearch.  Several implementations of plugins extend storage options out to external services.

### Access Control

With the security plugin installed, role based access control (RBAC) is available with a policy document format specific to OpenSearch.  Access control over native OpenSearch data is possible with this plugin installed.

For resource that are managed by plugins, access control is governed within individual plugin. By examining [user](https://github.com/opensearch-project/common-utils/blob/main/src/main/java/org/opensearch/commons/authuser/User.java) object from OpenSearch's thread context permissions are available for approval/denial. An example from anomaly detection is [checkUserPermissions](https://github.com/opensearch-project/anomaly-detection/blob/875b03c1c7596cb34d74fea285c28d949cfb0d19/src/main/java/org/opensearch/ad/util/ParseUtils.java#L568).  Uniform resource controls and models are needed to protect from misconfiguration and code defects.

* :building_construction: Adding a uniform resource permission check is being worked on in [sdk#40](https://github.com/opensearch-project/opensearch-sdk/issues/40). 


As Extensions do not have access OpenSearch's thread context, identity and its associated privileges must be communicated through APIs.

* :building_construction: User identity is being worked on in [sdk#37](https://github.com/opensearch-project/opensearch-sdk/issues/37).

## Auditing

With the security plugin installed, when actions are performed on the OpenSearch cluster they are recorded if filtering criteria are meet to configurable audit log sinks.

## Installation

Plugin installation is managed by using a binary on the node, it is used when OpenSearch is not running. The tool can perform signature the native plugins and extracts the plugin zip files into the file system.  When OpenSearch starts it discovers and loads installed plugins into its JVM runtime.

Extensions installation is managed through on disk configuration. 

## Versioning

OpenSearch has a version number following [semver](https://semver.org/).

Plugins for OpenSearch must match their version exactly the version of OpenSearch.  Older version numbers are not compatible, so to resolve CVE in OpenSearch or in plugins - all components be re-released.

Extensions version information is not tied to OpenSearch's version, extensions and OpenSearch are able to independently release minor/patch versions to address CVEs.

## Configuration

Configuration of OpenSearch uses on disk yml configuration files.  Other settings are manage in-memory through settings that are modifiable at runtime through APIs or indirectly.

Plugins configuration is managed through the same systems as OpenSearch.

Extensions configuration setup is tied to OpenSearch settings, extensions configuration are managed independently of OpenSearch.

## Reliability

OpenSearch plugins can create cluster or node instability if incorrectly configured or by software defects.

# Projects
To stretch out the design process while fulfilling scenarios some security efforts will be tracked as longer running projects.  There will be tracking issues in github tied to the work, documentation here is justification for a project and how it ties into the security space.  Tasks will be denotated as incomplete with :negative_squared_cross_mark: `:negative_squared_cross_mark:` or completed with :white_check_mark:
 `:white_check_mark:`.

## Anomaly Detection Plugin
Overall project is tracked with [[FEATURE] Migrate Anomaly Detector plugin to work as an Extension](https://github.com/opensearch-project/opensearch-sdk/issues/24).  By migrating this plugin it will exercise the general extensions and security specific scenarios.

Additional background avaliable from [Security#1895](https://github.com/opensearch-project/security/issues/1895)

### User identity [OpenSearch#3846](https://github.com/opensearch-project/OpenSearch/issues/3846) :negative_squared_cross_mark:
Replace [commons.authuser.User](https://github.com/opensearch-project/common-utils/blob/main/src/main/java/org/opensearch/commons/authuser/User.java) the common identity object used by plugins.  The new object should be obtainable by the extension through other means than [InjectSecurity](https://github.com/opensearch-project/common-utils/blob/main/src/main/java/org/opensearch/commons/InjectSecurity.java#L69) depends on the thread context.

#### Aquiring User objects [sdk#37](https://github.com/opensearch-project/opensearch-sdk/issues/37)
When OpenSearch sents a request to an extension the identiy should be avaliable, more discussion see [Handling identity in extensions](https://github.com/opensearch-project/opensearch-sdk/issues/14).

#### Resource user/role checks [sdk#40](https://github.com/opensearch-project/opensearch-sdk/issues/40)
anomaly Detection has detectors that analyzer data and store its results so it can be inspected or alerted on, [more details](https://opensearch.org/docs/latest/monitoring-plugins/ad/index/). OpenSearch should be responsible for inspecting the user, roles, resources to ensure standard practices are used.  A permissions check API should be designed and implemented to offload this work from extensions creators.

### Delegate Authority  [OpenSearch#3850](https://github.com/opensearch-project/OpenSearch/issues/3850) :negative_squared_cross_mark:
anomaly Detection runs background jobs to scan for anamolies and alerts that trigger if conditions are detected.  Background tasks should be tied to an idenity and a delegated identity so permissions can be verified.  The underlying systems depends on the [Job Scheduler](https://github.com/opensearch-project/job-scheduler/blob/main/src/main/java/org/opensearch/jobscheduler/scheduler/JobScheduler.java) plugin to execute these requests. 

#### Extension identity [sdk#41](https://github.com/opensearch-project/opensearch-sdk/issues/41)
There should be different levels of permissions granularity interactive allowing for disgushing a user actions or user action through an extension.  Extensions should have an identity and there should be a way that the identity of action is layered with all the parties that have triggered it. 

#### Delayed action API [sdk#42](https://github.com/opensearch-project/opensearch-sdk/issues/42)
When actions are triggered without an interactive user session OpenSearch will need to permit the action to occur or not.  Create an API for these background tasks to get an identity associated with the session.