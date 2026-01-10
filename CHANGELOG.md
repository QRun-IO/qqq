# Changelog

All notable changes to QQQ will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Security
- **Jetty 11.0.26** - Upgraded from 11.0.25 to fix HTTP/2 vulnerability (HIGH)
- **WireMock 3.13.2** - Upgraded from 3.13.0 to fix commons-fileupload vulnerability (HIGH)
- **commons-lang 2.x removed** - Migrated to commons-lang3 3.20.0 (no fix available for 2.x MEDIUM CVE)
- **iq80 snappy excluded** - Excluded vulnerable snappy from checkstyle plugin dependencies (MEDIUM)

### Notes
- commons-lang3 alert (3.18.0 fix) dismissed - already at 3.20.0

## [0.35.0] - 2025-12-28

### Changed

#### Platform
- **Java 21 LTS** - Migrated from Java 17 to Java 21 LTS

#### Dependencies - Major Updates
- **JUnit Jupiter** 5.8.1 → 6.0.1 (major version upgrade)
- **Checkstyle** 10.16.0 → 12.2.0 (major version upgrade)
- **Mockito** 5.14.2 → 5.21.0
- **ByteBuddy** 1.15.4 → 1.18.3

#### Dependencies - Build Plugins
- maven-compiler-plugin 3.10.1 → 3.14.1
- maven-surefire-plugin 3.5.3 → 3.5.4
- maven-jar-plugin 3.4.2 → 3.5.0
- central-publishing-maven-plugin 0.8.0 → 0.9.0

#### Dependencies - Runtime
- MongoDB Driver 5.5.1 → 5.6.2
- SQLite JDBC 3.47.1.0 → 3.51.1.0
- AWS SDK BOM 2.40.13 → 2.40.16
- AWS Lambda Java Core 1.2.3 → 1.4.0
- AWS Lambda Java Events 3.14.0 → 3.16.1
- AWS Lambda Runtime Interface Client 2.6.0 → 2.8.7
- Commons Validator 1.9.0 → 1.10.1
- Jakarta Mail 2.0.1 → 2.0.2
- Angus Activation 2.0.2 → 2.0.3
- Nashorn Core 15.6 → 15.7
- SLF4J API 2.0.16 → 2.0.17
- Kotlin Stdlib 2.2.21 → 2.3.0

#### Dependencies - Test
- H2 Database 2.2.220 → 2.4.240
- Unirest Java 3.13.12 → 3.14.5

### Added
- Enhanced documentation wiki with comprehensive guides
- Improved GitHub templates and contribution process
- Better cross-references between documentation pages
- Health check middleware module (qqq-middleware-health)

## [0.27.0] - 2024-01-XX

### Added
- Comprehensive documentation wiki
- Enhanced development workflow guides
- Improved testing and code review standards
- Better contribution guidelines and templates

### Changed
- Streamlined documentation for QQQ framework developers
- Improved cross-references between wiki pages
- Enhanced GitHub project structure and templates

## [0.26.1] - 2024-01-XX

### Fixed
- Various bug fixes and improvements
- Enhanced stability and performance

## [0.26.0] - 2024-01-XX

### Added
- Core QQQ framework capabilities
- Backend modules for RDBMS, filesystem, MongoDB, SQLite
- Middleware support for Javalin, PicoCLI, Lambda, Slack
- React dashboard framework with Material-UI
- Comprehensive testing and quality standards

### Changed
- Initial public release of QQQ framework
- Established development workflow and standards
- Created modular architecture foundation

---

## 📚 For Detailed Information

**📖 [Complete Documentation Wiki](https://github.com/Kingsrook/qqq/wiki)** - Start here for comprehensive guides

- **[🏠 Home](https://github.com/Kingsrook/qqq/wiki/Home)** - Project overview and quick start
- **[🚀 Release Flow](https://github.com/Kingsrook/qqq/wiki/Release-Flow)** - Detailed release process
- **[🏷️ Changelog & Tagging](https://github.com/Kingsrook/qqq/wiki/Changelog-and-Tagging)** - Commit conventions and release notes
- **[🔧 Developer Onboarding](https://github.com/Kingsrook/qqq/wiki/Developer-Onboarding)** - Setup and contribution guide

## 🔄 Version Compatibility

QQQ follows semantic versioning:
- **MAJOR** versions may contain breaking changes
- **MINOR** versions add new functionality (backward compatible)
- **PATCH** versions contain bug fixes (backward compatible)

For detailed compatibility information, see [Compatibility Matrix](https://github.com/Kingsrook/qqq/wiki/Compatibility-Matrix).

---

**Thank you for using QQQ!** 🚀
