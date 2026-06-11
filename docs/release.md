# Release Guide

## Versioning

The mod version is defined in `gradle.properties` as `mod_version`. The first release version is `0.1.0`.

Use matching git tags for releases:

```bash
git tag v0.1.0
git push origin v0.1.0
```

## GitHub Secrets

The publish workflow requires these repository secrets:

- `MODRINTH_PROJECT_ID`: Modrinth project ID or slug.
- `MODRINTH_TOKEN`: Modrinth personal access token with version upload permission.
- `CURSEFORGE_PROJECT_ID`: CurseForge numeric project ID.
- `CURSEFORGE_TOKEN`: CurseForge API token with project upload permission.

No publishing token or project ID is stored in the repository.

## Workflows

- `.github/workflows/build.yml` runs on pushes and pull requests, builds the NeoForge platform, and uploads jars as CI artifacts.
- `.github/workflows/publish.yml` runs on `v*` tags or manual dispatch, builds the jar, and publishes to Modrinth and CurseForge using `Kir-Antipov/mc-publish`.

## Manual Publish

From GitHub Actions, run the `Publish` workflow manually and provide:

- `version`: release version, for example `0.1.0`.
- `version_type`: `alpha`, `beta`, or `release`.

Tag publishing is preferred for real releases because the tag is immutable and auditable.
