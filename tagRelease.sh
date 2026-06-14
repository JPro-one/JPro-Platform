#!/bin/bash
# Tags the current commit as release X.Y.Z (no publishing happens locally).
# The tag push triggers .github/workflows/release.yml, which builds and publishes.
# Tags have no prefix, matching this repository's existing tags.
set -e

# First arg is the project name, guarding against running this in the wrong repo.
PROJECT="jpro-platform"
if [ "$1" != "$PROJECT" ]; then
    echo "usage: ./tagRelease.sh $PROJECT X.Y.Z"
    exit 1
fi

VERSION=$2
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "usage: ./tagRelease.sh $PROJECT X.Y.Z"
    exit 1
fi

if [ -n "$(git status --porcelain -uno)" ]; then
    echo "error: uncommitted changes to tracked files"
    exit 1
fi

BRANCH=$(git branch --show-current)
if [ "$BRANCH" != "main" ]; then
    echo "error: not on main (on $BRANCH)"
    exit 1
fi

git fetch origin
if [ "$(git rev-parse HEAD)" != "$(git rev-parse origin/main)" ]; then
    echo "error: HEAD is not in sync with origin/main"
    exit 1
fi

if git rev-parse "$VERSION" >/dev/null 2>&1; then
    echo "error: tag $VERSION already exists"
    exit 1
fi

if ! grep -q "^### $VERSION " CHANGELOG.md; then
    echo "error: CHANGELOG.md has no entry '### $VERSION ...'"
    exit 1
fi
if grep "^### $VERSION " CHANGELOG.md | grep -qi "unreleased"; then
    echo "error: CHANGELOG.md entry for $VERSION is still marked (unreleased) — set the release date"
    exit 1
fi

git tag "$VERSION"
git push origin "$VERSION"
echo "Tagged and pushed $VERSION — the release workflow takes it from here:"
echo "https://github.com/JPro-one/JPro-Platform/actions"
