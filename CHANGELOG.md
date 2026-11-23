# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.1] - 2026-11-23

### Added

- Publish to the Gradle Plugin Portal when a stable GitHub Release is published, after CI succeeds and its tag matches the plugin version.

### Fixed

- Resolve VHDL source ZIP dependencies only when needed, preserving exclusions and transitivity.
- Execute extraction as a real task action and remove stale dependency sources.
- Preserve the shared `build/dependency/<library>/<module>` layout using a single extraction task for RTL and simulation dependencies; extract shared artifacts only once.
- Check all resolved dependency versions for releases and gate release publication.
- Include uppercase `README.md` files in source distributions.
- Add functional coverage for resolution, extraction, publication, and configuration cache reuse.

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
