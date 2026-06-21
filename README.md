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
│       │   └── processing         # collection-level similarity / recommendation processors
│       ├── gmaf                   # core framework: session factory, plugin chain, UI helpers
│       │   ├── api
│       │   ├── extensions         # semantic extensions, stemming, dictionary defaults
│       │   └── plugin             # plugin API, fusion, video splitting, shot detection, EXIF
│       │       └── fusion         # spatial / union feature-fusion strategies
│       ├── mmfg                   # metadata / semantic models and builders
│       │   ├── builder            # flatteners / unflatteners (JSON, XML, EXIF exports)
│       │   └── extension          # semantic models (SMFG / ESMMFG)
│       ├── ui                     # collection, configuration, and logging helpers
│       └── importers              # RSS and feed ingestion
├── src/test/java
│   └── de/swa/gc, gmaf, mmfg, ui  # unit tests over core, graph and collection logic
├── conf                           # runtime configuration
├── dictionary                      # word lists and linguistic data for semantic extension
├── doc                            # published Javadoc
├── export                         # example export outputs (graphml/json/neo4j)
└── collection                      # sample assets used by tests/examples
```

## Core Modules

### `de.swa.gc` — Graph Code Layer
Graph Codes are the compact 2D graph representations of multimedia features. Key classes:
- **`GraphCode`**: Dictionary + adjacency matrix model; serializable to JSON.
- **`GraphCodeCollection`**: Static utilities to union, subtract, stop-word detection and summarisation of Graph Code collections.
- **`GraphCodeGenerator`**: Builds a `GraphCode` from an `MMFG` tree by iterating vocabulary nodes and relationship edges.
- **`GraphCodeIO`**: JSON-backed file transport for Graph Codes (read / write / asJson).
- **`GraphCodeMetric`**: Tri-metric similarity calculator (node, edge, type-match).

### `de.swa.gmaf` — Core Framework
- **`GMAF`**: Entry point for asset processing. Wires a singleton `PluginChain` and delegates per-asset work.
- **`PluginChain`**: Loads plugins from `conf/plugin.config` and orchestrates general vs. detail (recursive) processing. Handles bounding-box crop & re-entry for recursive plugins.
- **`GMAF_Plugin`**: Interface every plugin implements (`process`, `canProcess`, `isGeneralPlugin`, `providesRecoursiveData`, `getDetectedNodes`).
- **`TempFileProvider` / `TempURLProvider`**: Materialise byte assets to disk or FTP-backed URL space for plugins that require file/URL inputs.

### `de.swa.gmaf.plugin` — Plugin Pipeline & Fusion
- **Built-in plugins**: `ExifHandler` (EXIF thumbnail/metadata extraction), video-splitting / shot-detection, and cloud-vision stubs.
- **`FeatureFusionPlugin`**: Two-plugin fusion interface; combines two plugins before merging into the target `MMFG`.
- **`FeatureFusionStrategy`**: Optimisation pass after fusion.
  - **`SpacialFeatureFusion`**: Adds `RELATION_NEXT_TO / ABOVE / UNDER` composition edges from bounding-box coords.
  - **`UnionFeatureFusion`**: Union of node sets across a collection.

### `de.swa.mmfg` — Metadata / Semantic Models
- **`MMFG`**: Root model — nodes, time ranges, locations, security, and nested collection elements.
- **`Node`**: Vocabulary item carrying technical attributes, semantic/composition relationships, and timeranges.
- **`GeneralMetadata` / `Security` / `Location` / `TechnicalAttribute`**: Structured asset metadata.
- **Builders (`de.swa.mmfg.builder`)**:
  - `FeatureVectorBuilder`: EXIF-backed factory, merge & flatten/unflatten helpers.
  - `JsonFlattener` / `XMLEncodeDecode` / `DetectionExporter`: Serialisation formats (JSON, XML, detection XML).
- **Extensions (`de.swa.mmfg.extension`)**: SMFG / ESMMFG graphs and language-model wrappers.

### `de.swa.ui` — Runtime Helpers
- **`MMFGCollection` / `DefaultMMFGCollection` / `MvMMFGCollection`**: In-memory or MV-backed collection stores.
- **`MMFGCollectionFactory`**: Session-scoped or global collection creation.
- **`Configuration`**: Typed wrapper over `conf/gmaf.config`; validates folders and exposes builder-pattern accessors.

## Runtime and Configuration

Runtime behavior is controlled by files in `conf/`. This repo only defines default configuration; downstream modules may override at runtime.

### `conf/gmaf.config`
Key properties:
- `collectionPath`, `collectionName` — where assets live and the friendly collection name.
- `graphCodeRepository`, `mmfgRepository`, `exportFolder`, `thumbnailFolder` — cache directories.
- `fileExtensions` — extensions accepted by the framework (e.g. `jpg,mp4`).
- `maxNodes`, `maxRecursions`, `autoProcess`, `uiMode` — runtime limits and UI theme.
- `semanticExtension` — pluggable semantic extension class.
- `collectionProcessor` — pluggable collection processor (e.g. `DefaultCollectionProcessor`) for scaled similarity/recommendation runs.
- `collectionManager` — collection implementation class (default `DefaultMMFGCollection`).

### `conf/plugin.config`
Line-delimited, class-per-plugin. Empty lines and `#` comments are ignored. Built-in stubs (EXIF, Google Vision, MPEG7 importers, text detection) are included but commented out; add or remove entries to change the active plugin set.

## Dependencies

Libraries included on the compile classpath are defined in `pom.xml`.

Highlights:
- Apache Jena
- Lucene
- XStream
- ROME
- Neo4j JDBC
- FlatLaf UI toolkit
- Google Gson
- Apache Commons Imaging (EXIF)

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
