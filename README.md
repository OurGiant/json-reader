# JSON Viewer

[![Build](https://github.com/OurGiant/json-reader/actions/workflows/build.yml/badge.svg)](https://github.com/OurGiant/json-reader/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/v/release/OurGiant/json-reader)](https://github.com/OurGiant/json-reader/releases/latest)
[![License: MIT](https://img.shields.io/github/license/OurGiant/json-reader)](LICENSE)
[![Java 24](https://img.shields.io/badge/Java-24-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Platforms](https://img.shields.io/badge/platform-Linux%20%7C%20macOS%20%7C%20Windows-blue)](#build)

A Java Swing desktop application for viewing, formatting, and linting JSON. Provides syntax highlighting and one-click pretty-printing or minification.

## Features

- **Syntax highlighting**: Keys, strings, numbers, booleans, and nulls each rendered in a distinct colour
- **Format**: Pretty-print compact or malformed JSON with proper indentation
- **Stringify**: Collapse formatted JSON back to a single-line compact string
- **Lint**: Validate JSON and report errors with position information
- **Copy**: Copy the current output to the clipboard
- **Clear**: Reset the editor in one click
- **About & update check**: Help > About shows the app name, version, and copyright, and checks GitHub Releases for a newer version — both a manual check from the dialog and a silent, non-blocking startup check that's deduped so the same available version is only surfaced once
- **Theme selection**: View > Theme switches between Swing's default look-and-feels and FlatLaf/IntelliJ themes; the choice persists across restarts via `AppPreferences`

## Prerequisites

- Java 24 or higher

## Build

```bash
mvn clean package
```

Produces `target/json-viewer-all.jar`.

## Run

```bash
java -jar target/json-viewer-all.jar
```

## Project Structure

```
src/main/java/com/ourgiant/utilities/
├── Main.java              # Entry point
├── AppPreferences.java    # java.util.prefs wrapper (update-check dedup version, theme choice)
├── ThemeManager.java      # FlatLaf/Swing look-and-feel selection, applied at startup and from View > Theme
├── core/                  # Swing-free JSON logic (format/compact/stringify/validate/tokenize)
├── model/                 # Plain data types (JsonToken, JsonTokenType)
├── gui/                   # MainWindow, AboutDialog, and all Swing wiring
└── util/                  # AppVersion, UpdateChecker, and other shared helpers with no business meaning of their own
src/main/resources/
├── app-icon.png            # Loaded by AboutDialog; distinct from src/packaging/linux/app-icon.png (jpackage input)
├── logback.xml             # Logging configuration
└── version.properties      # Filtered at build time with the Maven project version
src/packaging/
├── app-icon.ico            # Windows installer/app icon
├── app-icon.icns           # macOS installer/app icon
└── linux/                  # Linux .desktop, PNG icon, and postinst/prerm scripts for jpackage's --resource-dir
```

`gui/` depends one-way on `core/` and `model/`; neither of those imports `javax.swing.*`, so the JSON logic is unit-testable without a display.

## Dependencies

- [FlatLaf](https://www.formdev.com/flatlaf/) (+ intellij-themes, + extras) — look and feel
- SLF4J + Logback — logging (console + rolling file at `~/.json-viewer/logs/`)
- JUnit 5 + Mockito — testing only

See `SPEC.md` for the reasoning behind keeping JSON parsing itself hand-rolled rather than adopting a library like Jackson.

## Hardening

`core/JsonProcessor` processes arbitrary pasted/opened text, which should be
treated as untrusted input (it isn't validated or sanitized before reaching
the parser). Two limits guard against pathological input:

- **Size cap** (`JsonProcessor.MAX_INPUT_LENGTH`, 10,000,000 characters): every
  entry point (`format`, `compact`, `stringify`, `escape`, `unescape`,
  `validate`, `tokenize`) rejects oversized input up front with a
  `JsonProcessingException`, instead of doing O(n) work on an unbounded
  string on the Swing event thread (which would otherwise hang the UI for a
  very large paste, or risk an `OutOfMemoryError`).
- **Nesting-depth cap** (`JsonProcessor.MAX_NESTING_DEPTH`, 1,000 levels):
  `format` and `validate` both reject input nested deeper than this, so a
  pathologically nested document (e.g. thousands of `[` in a row) can't build
  an unbounded bracket stack or indent string.
- **Malformed input degrades gracefully**: `format` is a best-effort
  pretty-printer that can run *before* `validate` has confirmed the input is
  well-formed, so it never assumes balanced brackets — e.g. more closing
  brackets than opening ones no longer throws an unhandled
  `IllegalArgumentException` from a negative indent.

These limits live in `core/JsonProcessor` itself (not just the GUI layer), so
they hold regardless of caller and are covered directly by
`JsonProcessorTest`.

## License

See LICENSE file for details.
