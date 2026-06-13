# Releasing

The version is **derived from git tags** — it exists nowhere in the repository
(the former `JPRO_PLATFORM_VERSION` in `gradle.properties` is gone). Tags use the
repository's existing style: `X.Y.Z`, no prefix.

- HEAD exactly on tag `X.Y.Z` → version `X.Y.Z`
- commits after tag `X.Y.Z` (or a dirty tree) → version `X.Y.(Z+1)-SNAPSHOT`
- no tag → `0.7.0-SNAPSHOT` (fallback)

The derived version is printed at the start of every build.

## Releasing a version

```bash
./tagRelease.sh 0.7.0
```

The script only tags: it verifies a clean tree on an up-to-date `main` and a dated
`### 0.7.0` CHANGELOG entry, then tags and pushes `0.7.0`. The tag push triggers
`.github/workflows/release.yml`, which builds and publishes via the scripts:

| Script | Registry | Versions |
|---|---|---|
| `publishSandecArtifactory.sh` | Sandec Artifactory | snapshots (every main push, via CI) and releases |
| `publishMavenCentral.sh` | Maven Central | releases only (refuses snapshots) |

There is no version bump and no bump-back — after the release, builds automatically
become `X.Y.(Z+1)-SNAPSHOT`.

## Required repository secrets

- `SANDEC_ARTIFACTORY_USERNAME` / `SANDEC_ARTIFACTORY_PASSWORD`
- `SANDEC_SIGNING_KEY_ID` / `SANDEC_SIGNING_SECRET_KEY` / `SANDEC_SIGNING_PASSWORD` — GPG (signing is skipped when unset, so snapshot/local builds work without it)
- `MAVEN_CENTRAL_AUTH_TOKEN` — Sonatype Central Portal token

## Notes

- Transition: the latest existing tag is `0.6.3`, so until the first `0.7.0` tag exists, builds derive `0.6.4-SNAPSHOT` even though the next release is `0.7.0`. The first `./tagRelease.sh 0.7.0` realigns everything.
- To release `0.7.0`: set the date on the `### 0.7.0` CHANGELOG entry (remove `(unreleased)`), commit, then `./tagRelease.sh 0.7.0`.
- CI checkouts use `fetch-depth: 0` (configured) — a shallow clone can't see tags and would fall back to the SNAPSHOT default.
- Set `MAVEN_CENTRAL_PUBLISHING_TYPE=USER_MANAGED` to review the deployment in the portal before it goes live; default is `AUTOMATIC`.
