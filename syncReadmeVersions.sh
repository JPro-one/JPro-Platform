#!/bin/bash
# Rewrites (or, with --check, validates) the one.jpro.platform version pins in every
# README.md so the published documentation stays in sync with the release. See RELEASING.md.
#
#   ./syncReadmeVersions.sh X.Y.Z            rewrite all pins to X.Y.Z
#   ./syncReadmeVersions.sh --check X.Y.Z    list pins that differ from X.Y.Z (exit 1 if any)
set -e

CHECK=0
if [ "$1" = "--check" ]; then
    CHECK=1
    shift
fi

VERSION=$1
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "usage: ./syncReadmeVersions.sh [--check] X.Y.Z"
    exit 1
fi

cd "$(dirname "$0")"
READMES=$(git ls-files '*README.md')

if [ "$CHECK" = "1" ]; then
    STALE=$(VERSION="$VERSION" perl -0777 -ne '
      my $v = $ENV{VERSION};
      while (/one\.jpro\.platform:[A-Za-z0-9_.-]+:([0-9]+\.[0-9]+\.[0-9]+(?:-SNAPSHOT)?)/g) {
        print "$ARGV: $1\n" if $1 ne $v;
      }
      while (/<groupId>\s*one\.jpro\.platform\s*<\/groupId>\s*<artifactId>[^<]+<\/artifactId>\s*<version>([0-9]+\.[0-9]+\.[0-9]+(?:-SNAPSHOT)?)<\/version>/sg) {
        print "$ARGV: $1\n" if $1 ne $v;
      }
    ' $READMES)
    if [ -n "$STALE" ]; then
        echo "$STALE"
        exit 1
    fi
    exit 0
fi

VERSION="$VERSION" perl -0777 -i -pe '
  my $v = $ENV{VERSION};
  s/(one\.jpro\.platform:[A-Za-z0-9_.-]+:)[0-9]+\.[0-9]+\.[0-9]+(?:-SNAPSHOT)?/${1}$v/g;
  s/(<groupId>\s*one\.jpro\.platform\s*<\/groupId>\s*<artifactId>[^<]+<\/artifactId>\s*<version>)[0-9]+\.[0-9]+\.[0-9]+(?:-SNAPSHOT)?(<\/version>)/${1}$v${2}/gs;
' $READMES
echo "Updated README.md version pins to $VERSION."
