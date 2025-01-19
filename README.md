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

- **Maven Publishing**:
  - Automates the publication of the source distribution ZIP with transitive dependency information included in the POM file.

---

## Getting Started

### Expected Structure

This plugin is based on a certain file structure for VHDL projects.
The gradle project is expected to be integrated at the root level of a VHDL library.
Each VHDL module shall be located in a separate subdirectory, located directly under root.
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

### Applying the Plugin

To use the plugin, include it in your `build.gradle` file:

``` groovy
plugins {
  id 'io.github.vhdlivery.vhdlArtifacts' version "1.0.0"
}
```

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

