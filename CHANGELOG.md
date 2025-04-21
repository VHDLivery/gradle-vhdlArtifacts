# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2025-04-21

### Added

- New task `releaseCheck` to prepare a release. This includes:
  - Checking whether the version of each dependency ends with `-RELEASE`.
  - Checking whether the version of the current project has been set to `-RELEASE`.
  - Note: Additional checks will follow soon.

### Style

- Removed print statement of library (left over from debugging)

## [1.1.0] - 2025-04-13

### Added

- Support for standalone gradle projects for modules has been added

## [1.0.1] - 2025-02-15

### Fixed

- Removed lower case transformation of subproject names for github as it creates issues on other package registries
