# Working on the sbt Kotlin plugin

## Run sbt scripted test

```shell
sbt scripted
```

## Test manually

Publish the plugin locally with:

```shell
sbt publishLocal
```

Then use the locally published version in your test build.

## Release a new version:

- Update the version number in [build.sbt](build.sbt).
- Update the version number in the [README file](README.md).
- Tag the release commit, e.g. `v2.0.0`.

The [release workflow](.github/workflows/release.yml) will perform the release. You will still need to close and promote manually the staging repo.

Then update the version number in [build.sbt](build.sbt) to the next SNAPSHOT version.
