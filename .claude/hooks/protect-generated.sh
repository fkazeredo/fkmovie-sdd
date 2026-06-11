#!/usr/bin/env bash
# PreToolUse hook: blocks manual edits to generated files (architecture/delivery.md, 24.3).
# Exit code 2 blocks the tool call and feeds stderr back to Claude.

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

[ -z "$FILE_PATH" ] && exit 0

case "$FILE_PATH" in
  */target/*|*/generated/*|*/generated-sources/*|*/node_modules/*|*/dist/*|*/.angular/*)
    echo "BLOCKED: '$FILE_PATH' is a generated/build artifact. Modify the generation source (OpenAPI contract, .proto, schema or generator config) instead. See architecture/delivery.md." >&2
    exit 2
    ;;
esac

exit 0
