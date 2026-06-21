[![Java CI with Maven](https://github.com/marquies/gmaf-mvr/actions/workflows/maven.yml/badge.svg)](https://github.com/marquies/gmaf-mvr/actions/workflows/maven.yml)
# GMAF (MVR Edition)

GMAF is a Java media-analysis framework for feature extraction, graph-based representation, metadata enrichment, and semantic extension. This fork adds MVR-specific integrations and a plugin pipeline for video/image processing.

## Status

- **Default branch:** `master`
- **CI:** Java CI with Maven on `master` and `develop`
- **JDK:** build on JDK 17+; CI runs Temurin JDK 21

## Prerequisites

- JDK 17 or 21 (recommended); CI uses Java 21
- Maven 3.8+
- Git

## Build and Test

```bash
# run unit tests
mvn -B test

# build artifacts without tests
mvn -B package -DskipTests

# install into local Maven repository
mvn -B install -DskipTests
```

Runs JUnit via Maven Surefire. Test sources are under `src/test`.

### Artifact

- Group/artifact: `de.swa:gmaf-mvr:1.0.6`
- Type: `jar`
- Final name: `GMAF`

## Project Structure

```
.
├── src/main/java
│   └── de/swa
│       ├── gc                     # graph code collection, IO and metrics
│       ├── gmaf                   # core framework: session factory, plugin chain, UI helpers
│       │   ├── api
│       │   ├── extensions         # semantic extensions, stemming, dictionary defaults
│       │   └── plugin             # plugin API, fusion, video splitting, shot detection, EXIF
│       └── mmfg                   # metadata / semantic models and builders
├── src/test/java
│   └── de/swa/gc, gmaf, mmfg, ui  # unit tests over core, graph and collection logic
├── conf                           # runtime configuration
├── dictionary                      # word lists and linguistic data for semantic extension
├── doc                            # published Javadoc
├── export                         # example export outputs (graphml/json/neo4j)
└── collection                      # sample assets used by tests/examples
```

## Runtime and Configuration

Runtime behavior is controlled by files in `conf/`. This repo only defines default configuration; downstream modules may override at runtime.

## Dependencies

Libraries included on the compile classpath are defined in `pom.xml`.

Highlights:
- Apache Jena
- Lucene
- XStream
- ROME
- Neo4j JDBC
- FlatLaf UI toolkit

## CI/CD

Workflow: `.github/workflows/maven.yml`

- Build job: `mvn -B package`
- Snapshot publish: `mvn deploy -DskipTests` to GitHub Packages when required
- Test results: JUnit report action writes `**/target/surefire-reports/TEST-*.xml`

## Samples and Exports

The `export/` directory contains generated outputs such as GraphML, JSON, XML, and Neo4j-ready exports based on the sample collection.

## Contributing

Keep changes aligned with the existing package layout and Maven build. Add tests alongside changed code, and run `mvn test` before submitting work.

## Changelog

See [CHANGELOG.md](https://github.com/marquies/gmaf-mvr/blob/master/CHANGELOG.md).
