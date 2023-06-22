- [Overview](#overview)
- [Branching](#branching)
  - [Release Branching](#release-branching)
  - [Feature Branches](#feature-branches)
- [Releasing](#releasing)
- [Snapshots](#snapshot-builds)

## Overview

This document explains the release strategy for artifacts in this organization.

## Branching

### Release Branching

Given the current major release of 1.0, projects in this organization maintain the following active branches.

* **main**: The next _major_ release. This is the branch where all merges take place and code moves fast.
* **1.x**: The next _minor_ release. Once a change is merged into `main`, decide whether to backport it to `1.x`.

Label PRs with the next major version label (e.g. `2.0.0`) and merge changes into `main`. Label PRs that you believe need to be backported as `1.x`. Backport PRs by checking out the versioned branch, cherry-pick changes and open a PR against each target backport branch.

### Feature Branches

Do not create branches in the upstream repo, use your fork, for the exception to long-lasting feature branches that require active collaboration from multiple developers. Name feature branches `feature/<name>`. Once the work is merged to `main`, please make sure to delete the feature branch.

## Releasing

The release process is standard across repositories in this org and is run by a release manager volunteering from amongst [maintainers](MAINTAINERS.md).

1. Create a tag, e.g. 1.0.0, and push it to this GitHub repository.
2. The [release-drafter.yml](.github/workflows/release-drafter.yml) will be automatically kicked off and a draft release will be created.
3. This draft release triggers the [jenkins release workflow](https://build.ci.opensearch.org/job/opensearch-sdk-java-release) as a result of which the sdk is released on [maven central](https://search.maven.org/search?q=org.opensearch.sdk). Please note that the release workflow is triggered only if created release is in draft state.
4. Once the above release workflow is successful, the drafted release on GitHub is published automatically.
5. Increment "version" in [build.gradle](https://github.com/opensearch-project/opensearch-sdk-java/blob/main/build.gradle#L79) to the next iteration, e.g. v1.0.1.

## Snapshot Builds
The [snapshots builds](https://aws.oss.sonatype.org/content/repositories/snapshots/org/opensearch/sdk/opensearch-sdk-java/) are published to sonatype using [publish-snapshots.yml](./.github/workflows/publish-snapshots.yml) workflow. Each `push` event to the main branch triggers this workflow.
