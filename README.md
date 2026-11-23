# VHDL Artifacts Gradle Plugin

The VHDL Artifacts Plugin is a custom Gradle plugin designed to streamline the management of VHDL module source code
artifacts. It provides configuration, dependency resolution, distribution packaging,
and publishing mechanisms tailored for VHDL projects.

## Features

- **Custom Configurations**:
  - `rtl`: Manages VHDL module source code artifacts for RTL usage. (transitive)
  - `sim`: Manages VHDL module source code artifacts for simulation purposes. (not transitive)

- **Dependency Conflict Detection**:
  - Automatically checks for conflicting versions between the `rtl` and `sim` configurations
  when extracting dependencies, validating a release, or publishing. Unrelated tasks do not resolve dependencies.

- **Artifact Unzipping**:
  - Tasks to extract dependencies into the `build/dependency` directory:
    - `unzipRtlArtifacts`
    - `unzipSimArtifacts`

- **Distribution Packaging**:
  - Creates a source distribution ZIP (`srcDistZip`) containing:
    - The `src` directory under a structured path (`libName/modName`).
    - A `readme.md` or `README.md` file.

- **Release Check**:
  - Checks whether every resolved dependency, including transitive RTL dependencies, ends with `-RELEASE`.
  - Checks whether the version of the current project ends with `-RELEASE`.
  - Publishing a `-RELEASE` version automatically runs the checks. Snapshot publishing remains available.

- **Maven Publishing**:
  - Automates the publication of the source distribution ZIP with transitive dependency information included in the POM file.

---

## Getting Started

### Expected Structure

This plugin is based on a certain file structure for VHDL modules.
There are two ways:

#### Module as Root Project

Each module is a Gradle project of its own.

```
module/
├── gradle
├── build.gradle
├── settings.gradle
├── src
├── ...
```

Note: As Gradle is not aware of the library this module belongs to, the library must be either given by the user or
`work` is used as a default library. See [Plugin Configuration](#plugin-configuration) for more info.

#### Modules as Subprojects

In this case, each VHDL module is located in a separate subdirectory, directly under the root project.
For each VHDL module, a separate gradle subproject has to be setup.

```
library/
├── gradle
├── build.gradle
├── settings.gradle
├── module1/
│   ├── build.gradle
│   └── src
├── module2/
│   ├── build.gradle
│   └── src
├── module3/
│   ├── build.gradle
│   └── src
├── ...
```

Note: Gradle will assume the name of the root project as the library name. This can be manually overwritten.
See [Plugin Configuration](#plugin-configuration) for more info.

### Applying the Plugin

To use the plugin, include it in your `build.gradle` file:

``` groovy
plugins {
  id 'io.github.vhdlivery.vhdlArtifacts' version "1.2.1"
}
```

### Plugin Configuration

To configure the plugin, include the following in your `build.gradle`:

``` groovy
vhdlArtifacts {
  libraryName = "example"
}
```

The following properties are supported:

| Property    | Type   | Default                       | Description                                      |
|-------------|--------|-------------------------------|--------------------------------------------------|
| moduleName  | string | Project Name                  | Overwrite the module name used for the artifact  |
| libraryName | string | Parent Project Name or "work" | Overwrite the library name used for the artifact |

Both extraction entry points synchronize the union of `rtl` and `sim` dependencies into the shared `build/dependency/<library>/<module>` tree through one task, `unzipVhdlArtifacts`. An artifact present in both configurations is extracted only once. Stale sources are removed without either configuration deleting sources required by the other. `rtl` remains transitive and `sim` remains nontransitive.

### Declaring Dependencies

Two configurations are provided ready for use within the dependency clause, they do not have to be defined manually.

``` groovy
dependencies {
  rtl '<group>:<artifactId>:<version>'
  sim '<group>:<artifactId>:<version>'
}
```

### Tasks

1. ***Unzipping Artifacts***:
  - `unzipRtlArtifacts`: Synchronizes shared RTL and simulation artifacts into `build/dependency`
  - `unzipSimArtifacts`: Synchronizes shared RTL and simulation artifacts into `build/dependency`
2. ***Source Distribution***:
  - `srcDistZip`: Generates a ZIP containing the `src` directory and the `readme.md` or `README.md` file:
    ``` bash
    build/distributions/{project-name}-{project-version}-src.zip
    ```
    
### Publishing

The plugin configures a Maven publication named `srcArtifact`. This publication:
  - Includes the source distribution ZIP.
  - Appends `rtl` dependencies to the POM file as dependencies.
To publish the artifacts, configure the `publishing` extension in your `build.gradle`:

``` groovy
publishing {
    repositories {
        maven {
            url = uri("https://your.repository.url")
            credentials {
                username = "your-username"
                password = "your-password"
            }
        }
    }
}
```

Run the publishing task:

``` bash
./gradlew publish
```

---


## Releasing this plugin

The GitHub workflow builds and tests every push and pull request. Publishing a stable GitHub Release triggers a fresh build of the release commit and, after success, publishes this plugin to the Gradle Plugin Portal. The release tag (for example `v1.2.1`) must match `version` in `plugin/build.gradle.kts`; a mismatch stops publication. Tag pushes and manual workflow runs only build and test. Draft releases and prereleases do not publish to the Plugin Portal.

Configure these GitHub repository secrets using the API credentials of a Plugin Portal account authorized to publish this plugin:

- `GRADLE_PUBLISH_KEY`
- `GRADLE_PUBLISH_SECRET`

Generate the credentials in your [Gradle Plugin Portal profile](https://plugins.gradle.org/user/login). See the [Gradle publishing documentation](https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html#account_setup).

Commit and push the release changes first. In GitHub, open **Releases → Draft a new release**, select an existing matching tag or create `v1.2.1` on the release commit, and choose **Publish release**. Leave **Set as a pre-release** unchecked. You can save a draft first; only publishing the stable release starts the portal publication.


## License

This plugin is licensed under the MIT license. See the [License](./LICENSE) file for details.

## Contributing

Contributions are welcome! Please submit a pull request or open an issue on the project's repository.

