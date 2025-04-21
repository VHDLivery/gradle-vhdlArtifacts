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
  during the project evaluation phase. If conflicts are found, the build will fail with detailed logs.

- **Artifact Unzipping**:
  - Tasks to extract dependencies into the `build/dependency` directory:
    - `unzipRtlArtifacts`
    - `unzipSimArtifacts`

- **Distribution Packaging**:
  - Creates a source distribution ZIP (`srcDistZip`) containing:
    - The `src` directory under a structured path (`libName/modName`).
    - A `readme.md` file.

- **Release Check**:
  - Checks whether the version of each dependency ends with `-RELEASE`.
  - Checks whether the version of the current project has been set to `-RELEASE`.

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
  id 'io.github.vhdlivery.vhdlArtifacts' version "1.0.0"
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
  - `unzipRtlArtifacts`: Unzips `rtl` configuration artifacts into `build/dependency`
  - `unzipSimArtifacts`: Unzips `sim` configuration artifacts into `build/dependency`
2. ***Source Distribution***:
  - `srcDistZip`: Generates a ZIP containing the `src` directory and the `readme.md` file:
    ``` bash
    build/distribution/{project-name}-{project-version}-src.zip
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

## License

This plugin is licensed under the MIT license. See the [License](./LICENSE) file for details.

## Contributing

Contributions are welcome! Please submit a pull request or open an issue on the project's repository.

