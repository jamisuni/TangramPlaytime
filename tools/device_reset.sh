#!/usr/bin/env bash
# WO-003 TASK-015 · seam S-16 (tasks.md § WO-003 › Seams) · platform-notes/android.md § Device hygiene:
# reset the app's data on the attached device before a device step, and check it after.
#
#   bash tools/device_reset.sh           adb shell pm clear app.tangramplaytime (not installed: nothing to clear)
#   bash tools/device_reset.sh --check   clean = the app is not installed, or its data directory holds no
#                                        file (listed through `run-as`, so a debuggable install; the empty
#                                        `cache` / `code_cache` directories the platform makes are not state)
#
# Exit: 0 reset done / clean; 1 reset failed / NOT clean (the files are listed); 2 no adb, no device, a bad
# argument, or data that cannot be read (a non-debuggable install) - an unknown state is never "clean".
# adb comes from $ANDROID_HOME (or $ANDROID_SDK_ROOT)/platform-tools, else the PATH; ANDROID_SERIAL picks
# the device when more than one is attached.
set -uo pipefail
PKG=app.tangramplaytime

find_adb() {
  local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" c
  if [ -n "$sdk" ]; then
    if command -v cygpath >/dev/null 2>&1; then sdk="$(cygpath -u "$sdk")"; fi
    for c in "$sdk/platform-tools/adb" "$sdk/platform-tools/adb.exe"; do
      if [ -x "$c" ]; then echo "$c"; return 0; fi
    done
  fi
  command -v adb 2>/dev/null || command -v adb.exe 2>/dev/null
}

ADB="$(find_adb)"
if [ -z "$ADB" ]; then echo "device_reset: adb not found (ANDROID_HOME or PATH)" >&2; exit 2; fi
if [ "$("$ADB" get-state 2>/dev/null | tr -d '\r')" != "device" ]; then
  echo "device_reset: no device attached (adb get-state)" >&2; exit 2
fi

installed() { [ -n "$("$ADB" shell pm path "$PKG" 2>/dev/null | tr -d '\r')" ]; }

case "${1:-}" in
  "")
    if ! installed; then echo "reset: $PKG is not installed - nothing to clear"; exit 0; fi
    out="$("$ADB" shell pm clear "$PKG" 2>&1 | tr -d '\r')"
    if [ "$out" = "Success" ]; then echo "reset: pm clear $PKG -> Success"; exit 0; fi
    echo "reset: pm clear $PKG failed: $out" >&2
    exit 1
    ;;
  --check)
    if ! installed; then echo "check: clean - $PKG is not installed"; exit 0; fi
    if ! files="$("$ADB" shell run-as "$PKG" find . -type f 2>&1 | tr -d '\r')"; then
      echo "check: unknown - cannot list $PKG's data: $files" >&2
      exit 2
    fi
    if [ -z "$files" ]; then echo "check: clean - $PKG's data directory holds no file"; exit 0; fi
    echo "check: NOT clean - $PKG's data directory holds:"
    echo "$files" | sed 's/^/  /'
    exit 1
    ;;
  *)
    echo "usage: bash tools/device_reset.sh [--check]" >&2
    exit 2
    ;;
esac
