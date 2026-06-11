#!/usr/bin/env bash
# PostToolUse hook: auto-formats Java files edited by Claude using Spotless.
# Formatting becomes a fact, not an instruction the model must remember.

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

case "$FILE_PATH" in
  *.java)
    # Find the Maven module root (nearest pom.xml above the file)
    DIR=$(dirname "$FILE_PATH")
    while [ "$DIR" != "/" ] && [ ! -f "$DIR/pom.xml" ]; do DIR=$(dirname "$DIR"); done
    if [ -f "$DIR/pom.xml" ]; then
      # spotlessFiles takes a regex matched against absolute paths
      (cd "$DIR" && mvn -q spotless:apply -DspotlessFiles=".*$(basename "$FILE_PATH")" >/dev/null 2>&1)
    fi
    ;;
esac

exit 0
