[![codecov](https://codecov.io/gh/opensearch-project/opensearch-sdk-java/branch/main/graph/badge.svg)](https://codecov.io/gh/opensearch-project/opensearch-sdk-java)

# OpenSearch SDK
- [Introduction](#introduction)
- [Contributing](#contributing)
- [Code of Conduct](#code-of-conduct)

## Introduction
Opensearch plugins have allowed the extension and ehancements of various core features however, current plugin architecture carries the risk of fatally impacting clusters should they fail. In order to ensure that plugins may run safely without impacting the system, our goal is to effectively isolate plugin interactions with OpenSearch by modularizing the [extension points](https://opensearch.org/blog/technical-post/2021/12/plugins-intro/) to which they hook onto.

Read more about extensibility [here](https://github.com/opensearch-project/OpenSearch/issues/1422)

## Contributing
See [DEVELOPER_GUIDE](DEVELOPER_GUIDE.md)

## Code of Conduct

This project has adopted the [Amazon Open Source Code of Conduct](CODE_OF_CONDUCT.md). For more information see the [Code of Conduct FAQ](https://aws.github.io/code-of-conduct-faq), or contact [opensource-codeofconduct@amazon.com](mailto:opensource-codeofconduct@amazon.com) with any additional questions or comments.
