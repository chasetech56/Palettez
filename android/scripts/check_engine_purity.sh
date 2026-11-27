#!/usr/bin/env bash
# check_engine_purity.sh
#
# CI guard: verifies that ColorEngine.kt and LayoutEngine.kt contain zero
# "import android.*" or "import androidx.*" lines.
#
# MODULE-BOUNDARY: These files must remain free of Android imports so they can
# be extracted into pure-JVM Gradle modules (:core:color / :core:layout) without
# any code changes.
#
# Usage:
#   bash android/scripts/check_engine_purity.sh
#
# Exit codes:
#   0 — all files are pure (no Android imports found)
#   1 — one or more Android imports detected (CI failure)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SRC_ROOT="${ANDROID_ROOT}/app/src/main/java/app/palettez"

ENGINE_FILES=(
  "${SRC_ROOT}/color/ColorEngine.kt"
  "${SRC_ROOT}/layout/LayoutEngine.kt"
)

FAIL=0

for FILE in "${ENGINE_FILES[@]}"; do
  if [ ! -f "${FILE}" ]; then
    echo "ERROR: Expected engine file not found: ${FILE}" >&2
    FAIL=1
    continue
  fi

  # Look for any Android/AndroidX import
  VIOLATIONS=$(grep -n "^import android\." "${FILE}" || true)
  VIOLATIONS+=$(grep -n "^import androidx\." "${FILE}" || true)

  if [ -n "${VIOLATIONS}" ]; then
    echo "FAIL: Android import(s) found in ${FILE}:" >&2
    echo "${VIOLATIONS}" >&2
    FAIL=1
  else
    echo "OK:   ${FILE} — no Android imports"
  fi
done

if [ "${FAIL}" -eq 1 ]; then
  echo ""
  echo "Engine purity check FAILED. Remove all 'import android.*' / 'import androidx.*'" >&2
  echo "lines from the files listed above before merging." >&2
  exit 1
fi

echo ""
echo "Engine purity check PASSED."
exit 0
