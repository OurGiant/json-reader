# JSON Viewer

A Java Swing desktop application for viewing, formatting, and linting JSON. Provides syntax highlighting and one-click pretty-printing or minification.

## Features

- **Syntax highlighting**: Keys, strings, numbers, booleans, and nulls each rendered in a distinct colour
- **Format**: Pretty-print compact or malformed JSON with proper indentation
- **Stringify**: Collapse formatted JSON back to a single-line compact string
- **Lint**: Validate JSON and report errors with position information
- **Copy**: Copy the current output to the clipboard
- **Clear**: Reset the editor in one click
- **No dependencies**: Runs on the standard Java SE library only

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
└── JsonFormatter.java    # Main application window, parser, and highlighter
```

## License

See LICENSE file for details.
