#!/usr/bin/env bash
# Compiles and prints only the compiler errors (first lines), for quick iteration.
cd "$(dirname "$0")/.." || exit 1
./gradlew "${@:-compileJava}" --console=plain 2>&1 | grep -E "error:|warning: \[|BUILD|FAILED|What went wrong|^\s+symbol:|^\s+location:|Exception|Caused by" | grep -v "Note:" | head -${LINES_MAX:-60}
