#!/bin/bash
# Publishes all modules to the Sandec Artifactory.
# With --snapshot-only, refuses release versions (used by the per-commit CI job;
# releases reach Artifactory through the tag-triggered release workflow).
set -e
cd "$(dirname "$0")"

if [ -z "$SANDEC_ARTIFACTORY_USERNAME" ]; then
    echo "error: SANDEC_ARTIFACTORY_USERNAME/PASSWORD not set"
    exit 1
fi

VERSION=$(./gradlew help -q 2>/dev/null | grep "Version derived" | sed -E 's/.*git tags: ([^ ]+).*/\1/')
echo "Publishing version $VERSION to Sandec Artifactory"

if [ "$1" = "--snapshot-only" ] && [[ "$VERSION" != *-SNAPSHOT ]]; then
    echo "Version $VERSION is a release — skipping (the release workflow publishes releases)."
    exit 0
fi

./gradlew publishAllPublicationsToArtifactoryRepository
