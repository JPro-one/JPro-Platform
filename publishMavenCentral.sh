#!/bin/bash
# Assembles the signed bundle and uploads it to Maven Central.
set -e
cd "$(dirname "$0")"

VERSION=$(./gradlew help -q 2>/dev/null | grep "Version derived" | sed -E 's/.*git tags: ([^ ]+).*/\1/')
echo "Publishing version $VERSION to Maven Central"

if [[ "$VERSION" == *-SNAPSHOT ]]; then
    echo "error: $VERSION is a SNAPSHOT — Maven Central only takes releases (tag with ./tagRelease.sh)"
    exit 1
fi

./gradlew publishAllPublicationsToCentralBundleRepository publishToMavenCentralPortal
