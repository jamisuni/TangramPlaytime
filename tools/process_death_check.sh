#!/usr/bin/env bash
# WO-004 TASK-022 · seam S-24 (designs/WO-004-design.md §2; tasks.md § WO-004 seam note 1): the process-death check, a
# test-channel harness (not a verifier). It builds nothing and refuses (exit 2) APKs older than the newest source or
# build input (reviews/WO-004-code-review.md S2). Gradle leaves an APK unwritten when its inputs are unchanged in content
# (a touched file), so build with the two package tasks forced: see BUILD below.
#
#   bash tools/process_death_check.sh                     install both APKs, pm clear, ProcessDeathKitTest#phase1
#                                                         (-e deathPhase 1), am force-stop, #phase2 (-e deathPhase 2)
#   bash tools/process_death_check.sh --positive-control  install, pm clear, #phase2 alone: it must run and fail
#   bash tools/process_death_check.sh --negative-run      the main run with #phase2 started without -e deathPhase
#
# A phase passes only when its `am instrument -r -w` output shows exactly one finished test, that phase's own method,
# with status 0 (failed: -1 or -2; not run: a skip -3 / -4, no test, a crash). Never by an exit status (the runner never
# varies it, design §3 fact 6) nor by "OK (1 test)", which a phase skipped by its guard prints too.
# The last step, whatever the verdict, is tools/device_reset.sh (pm clear). Every verdict line names the APK times.
# Exit: 0 PASS (the control: phase 2 failed); 1 not passed; 2 cannot judge (no adb, device or APK, stale APKs, or a
# failed install, clear or reset). adb is found as tools/device_reset.sh finds it; ANDROID_SERIAL picks the device.
set -uo pipefail
PKG=app.tangramplaytime
RUNNER="$PKG.test/androidx.test.runner.AndroidJUnitRunner"
KIT=app.tangramplaytime.app.ProcessDeathKitTest
APKS=(app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk)
BUILD="gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:packageDebug --rerun :app:packageDebugAndroidTest --rerun"
MODE="${1:-main}"
case "$MODE" in main|--positive-control|--negative-run) ;; *) echo "usage: bash tools/process_death_check.sh [--positive-control|--negative-run]" >&2; exit 2 ;; esac
cd "$(dirname "$0")/.." || exit 2

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
if [ -z "$ADB" ]; then echo "process-death: adb not found (ANDROID_HOME or PATH)" >&2; exit 2; fi
if [ "$("$ADB" get-state 2>/dev/null | tr -d '\r')" != "device" ]; then echo "process-death: no device attached (adb get-state)" >&2; exit 2; fi

say() { echo "process-death: $*"; }
stamp() { date -r "$1" '+%F %T'; }
APKTIMES="not read"

# The reset (the last step), then the verdict line; a failed reset never leaves a 0.
finish() {
  local rc=$1 msg=$2
  if ! bash tools/device_reset.sh; then msg="$msg; the reset FAILED"; [ "$rc" = 0 ] && rc=2; fi
  say "[$MODE] $msg [APKs: $APKTIMES] (exit $rc)"
  exit "$rc"
}

# phase <method> <deathPhase value, or "" for none>: one `am instrument` (a new process); sets VERDICT from the finished
# tests, one "<class>#<method> <status>" line each (a start, status 1, is left out).
phase() {
  local args=() label="no deathPhase argument" out tests
  if [ -n "$2" ]; then args=(-e deathPhase "$2"); label="-e deathPhase $2"; fi
  out="$("$ADB" shell am instrument -r -w "${args[@]}" -e class "$KIT#$1" "$RUNNER" 2>&1 | tr -d '\r')"
  tests="$(printf '%s\n' "$out" | awk '
    /^INSTRUMENTATION_STATUS: class=/ { c = $0; sub(/^INSTRUMENTATION_STATUS: class=/, "", c) }
    /^INSTRUMENTATION_STATUS: test=/ { t = $0; sub(/^INSTRUMENTATION_STATUS: test=/, "", t) }
    /^INSTRUMENTATION_STATUS_CODE: / { if ($2 != "1") print c "#" t, $2; c = ""; t = "" }')"
  case "$tests" in
    "$KIT#$1 0") VERDICT=passed ;;
    "$KIT#$1 -1" | "$KIT#$1 -2") VERDICT=failed ;;
    *) VERDICT="not run" ;;
  esac
  say "$1 ($label): $VERDICT - finished tests: ${tests:-none}"
  [ "$VERDICT" = passed ] || printf '%s\n' "$out" | grep -m 3 -E '^INSTRUMENTATION_(STATUS: stack|RESULT: (shortMsg|longMsg))=' | cut -c1-240 | sed 's/^/    /'
}

# Never judge stale APKs: the newest of every source and build input that can reach them.
newest=""
while IFS= read -r f; do
  if [ -z "$newest" ] || [ "$f" -nt "$newest" ]; then newest="$f"; fi
done < <(find app/src play/src store/src content/src kernel/src contracts/src -type f
  printf '%s\n' Tangrams/*.json {app,play,store,content,kernel,contracts}/build.gradle.kts build.gradle.kts settings.gradle.kts \
    gradle.properties gradle/libs.versions.toml)
for apk in "${APKS[@]}"; do [ -f "$apk" ] || finish 2 "ERROR - $apk not found: run $BUILD"; done
APKTIMES="${APKS[0]##*/} $(stamp "${APKS[0]}"), ${APKS[1]##*/} $(stamp "${APKS[1]}")"
for apk in "${APKS[@]}"; do
  [ "$newest" -nt "$apk" ] && finish 2 "STALE - ${apk##*/} is older than $newest ($(stamp "$newest")): run $BUILD"
done

for apk in "${APKS[@]}"; do
  out="$("$ADB" install -r -t "$apk" 2>&1 | tr -d '\r' | tail -n 1)"
  [ "$out" = "Success" ] || finish 2 "ERROR - adb install $apk: $out"
  say "installed $apk"
done
out="$("$ADB" shell pm clear "$PKG" 2>&1 | tr -d '\r')"
[ "$out" = "Success" ] || finish 2 "ERROR - pm clear $PKG: $out"
say "pm clear $PKG"

if [ "$MODE" = --positive-control ]; then
  phase phase2 2
  [ "$VERDICT" = failed ] && finish 0 "CONTROL PASS - over a cleared store phase 2 ran and failed, as it must"
  finish 1 "CONTROL FAIL - over a cleared store phase 2 was $VERDICT, not failed"
fi
phase phase1 1
[ "$VERDICT" = passed ] || finish 1 "FAIL - phase 1 $VERDICT"
"$ADB" shell am force-stop "$PKG"
say "am force-stop $PKG"
if [ "$MODE" = --negative-run ]; then phase phase2 ""; else phase phase2 2; fi
[ "$VERDICT" = passed ] || finish 1 "FAIL - phase 2 $VERDICT (phase 1 passed)"
finish 0 "PASS - phase 1, a force-stop, then phase 2 in a new process: both passed"
