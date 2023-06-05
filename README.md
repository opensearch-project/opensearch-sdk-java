[![codecov](https://codecov.io/gh/opensearch-project/opensearch-sdk-java/branch/main/graph/badge.svg)](https://codecov.io/gh/opensearch-project/opensearch-sdk-java)
[![GHA gradle check](https://github.com/opensearch-project/opensearch-sdk-java/actions/workflows/build.yml/badge.svg)](https://github.com/opensearch-project/opensearch-sdk-java/actions/workflows/build.yml)
[![GHA validate pull request](https://github.com/opensearch-project/opensearch-sdk-java/actions/workflows/wrapper.yml/badge.svg)](https://github.com/opensearch-project/opensearch-sdk-java/actions/workflows/wrapper.yml)

# OpenSearch SDK for Java

* [OpenSearch SDK for Java](#opensearch-sdk-for-java)
  * [Introduction](#introduction)
  * [Design](#design)
  * [Creating an extension](#creating-an-extension)
  * [Developer Guide](#developer-guide)
  * [Plugin migration](#plugin-migration)
  * [Contributing](#contributing)
  * [Maintainers](#maintainers)
  * [Code of Conduct](#code-of-conduct)

## Introduction
With OpenSearch plugins, you can extend and enhance various core features. However, the current plugin architecture may fatally impact clusters in the event of failure. To ensure that plugins run safely without impacting the system, our goal is to isolate plugin interactions with OpenSearch. The OpenSearch SDK for Java modularizes the [extension points](https://opensearch.org/blog/technical-post/2021/12/plugins-intro/) onto which plugins hook.

For more information about extensibility, see [this GitHub issue](https://github.com/opensearch-project/OpenSearch/issues/1422).

## Design
For an overview of extension architecture and information about how extensions work, see [DESIGN](DESIGN.md).

## Creating an extension
For information about developing an extension, see [CREATE_YOUR_FIRST_EXTENSION](CREATE_YOUR_FIRST_EXTENSION.md).

## Developer Guide
For instructions on building, testing, and running an extension, see the [DEVELOPER_GUIDE](DEVELOPER_GUIDE.md).

## Plugin migration
For tips on migrating an existing plugin to an extension, see [PLUGIN_MIGRATION](PLUGIN_MIGRATION.md).

## Contributing
For information about contributing, see [CONTRIBUTING](CONTRIBUTING.md).

## Maintainers
For points of contact, see [MAINTAINERS](MAINTAINERS.md).

## Code of Conduct
This project has adopted the [Amazon Open Source Code of Conduct](CODE_OF_CONDUCT.md). For more information see the [Code of Conduct FAQ](https://aws.github.io/code-of-conduct-faq), or contact [opensource-codeofconduct@amazon.com](mailto:opensource-codeofconduct@amazon.com) with any additional questions or comments.
