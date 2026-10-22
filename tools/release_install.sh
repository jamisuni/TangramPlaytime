#!/usr/bin/env bash
# WO-009 TASK-067 · seam S-62 (tasks.md § WO-009 › Seams); decisions.md 2026-10-22 "REQ-048.A3 is the release manifest and
# the installed app; the throwaway-signed release install is evidence, not a token."; platform-notes/android.md § Device
# hygiene ("the signed copy lives in the session scratchpad only, with the local debug key").
#
# The release install leg: the release APK as the platform sees it once installed. Run-log evidence beside the kit's
# REQ-048 A3 / REQ-010 A2 tokens (ReleasePromiseKitTest on the APK file, PlayThroughKitTest on the installed debug app);
# it carries no token.
#
#   bash tools/release_install.sh <scratch dir> [--launch] [--allow-real-device]
#
#   0. Refuses a device that is not an emulator (reviews/WO-009-code-review.md S2; decisions.md 2026-10-22 "S1, S2, S3
#      fixed now ..."): step 2 uninstalls app.tangramplaytime, and on a phone that erases the debug build's saved
#      progress, settings and play times (the owner's children play it). Before anything is signed, installed or
#      uninstalled, the device is read: it is an emulator when its adb serial is `emulator-<port>` or its own
#      `ro.kernel.qemu` or `ro.boot.qemu` property reads 1. Anything else exits 2, with nothing done on the device.
#      --allow-real-device lifts the refusal for a deliberate run on a real device whose app data may be lost.
#   1. Refuses a scratch dir inside the project. Copies the release APK (app/build/outputs/apk/release/
#      app-release-unsigned.apk, built by run 1 of the JVM line) there, `zipalign -p -f 4`, then `apksigner sign` with
#      the local debug key store (the user's .android/debug.keystore, alias androiddebugkey) into
#      <scratch dir>/app-release-throwaway.apk. Nothing is signed with any other key, and nothing signed lands in the
#      project (hard-stop 1): the build stays app-release-unsigned.apk, and the project holds no signing config.
#   2. `adb uninstall app.tangramplaytime` (the debug and the release package share the id, not the signature), then
#      `adb install` of the copy.
#   3. Reads `dumpsys package app.tangramplaytime`: in the package's block, the requested, install, runtime and
#      declared permission sections hold no entry (REQ-048 A3's installed view; REQ-010 A2). Positive control: the same
#      parse over `com.android.shell` finds at least one entry, so the parse can see a permission.
#   4. With --launch (only after the owner's marks: an empty release bundle fails at start by design, S-55):
#      `am start -W -n app.tangramplaytime/app.tangramplaytime.app.MainActivity`; then MainActivity is the resumed
#      activity, steadily, and logcat holds no FATAL EXCEPTION for the package.
#   5. Uninstalls the package at the end.
#
# One PASS or FAIL line per check, INFO lines beside them. Exit 0 every check passed; 1 a check failed; 2 a setup error
# (a bad argument, the scratch dir inside the project, no SDK, adb, build tools, java, debug key store, release APK or
# device, or a device that is not an emulator without --allow-real-device). The adb device: the one attached, or
# $ANDROID_SERIAL. Every adb call is bounded by `timeout`.
#
# The debug key store's passwords are the platform's published defaults for the local debug key ("android"), the
# key every Android build machine generates for debug builds; no other key store is ever named here.
set -uo pipefail

PKG=app.tangramplaytime
ACTIVITY=app.tangramplaytime.app.MainActivity
CONTROL_PKG=com.android.shell
SECTIONS="requested install runtime declared"

usage() {
  cat >&2 <<'EOF'
usage: bash tools/release_install.sh <scratch dir> [--launch] [--allow-real-device]
  <scratch dir>         where the throwaway-signed copy is made; refused inside the project
  --launch              also start MainActivity (only after the owner's marks: an empty release set fails at start)
  --allow-real-device   run on a device that is not an emulator. Without it such a device is refused (exit 2),
                        because the leg uninstalls app.tangramplaytime there, erasing the installed app's data
                        (a phone's debug-build progress, settings and play times)
EOF
  exit 2
}
setup() { echo "SETUP ERROR: $*" >&2; exit 2; }

SCRATCH=""; LAUNCH=0; ALLOW_REAL=0
for arg in "$@"; do
  case "$arg" in
    --launch) LAUNCH=1 ;;
    --allow-real-device) ALLOW_REAL=1 ;;
    -*) usage ;;
    *) [ -z "$SCRATCH" ] || usage; SCRATCH="$arg" ;;
  esac
done
[ -n "$SCRATCH" ] || usage

# A path as one comparable absolute POSIX spelling (Git Bash: /c/...; the drive and case folded on Windows).
posix() { if command -v cygpath >/dev/null 2>&1; then cygpath -u "$1"; else printf '%s\n' "$1"; fi; }
native() { if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else printf '%s\n' "$1"; fi; }
canon() { realpath -m "$(posix "$1")"; }
fold() { if command -v cygpath >/dev/null 2>&1; then printf '%s\n' "${1,,}"; else printf '%s\n' "$1"; fi; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
ROOT_C="$(canon "$ROOT")"
SCRATCH_C="$(canon "$SCRATCH")"
case "$(fold "$SCRATCH_C")/" in
  "$(fold "$ROOT_C")"/*) setup "the scratch dir $SCRATCH_C is inside the project $ROOT_C: the signed copy never lands in the project (hard-stop 1)" ;;
esac

# The SDK: $ANDROID_HOME, $ANDROID_SDK_ROOT, else local.properties' sdk.dir.
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ] && [ -f "$ROOT/local.properties" ]; then
  SDK="$(sed -n 's/^sdk\.dir=//p' "$ROOT/local.properties" | tr -d '\r' | head -1)"
fi
[ -n "$SDK" ] || setup "no Android SDK (ANDROID_HOME, ANDROID_SDK_ROOT or local.properties sdk.dir)"
SDK="$(posix "$SDK")"
ADB=""
for c in "$SDK/platform-tools/adb" "$SDK/platform-tools/adb.exe"; do [ -x "$c" ] && { ADB="$c"; break; }; done
[ -n "$ADB" ] || setup "no adb in $SDK/platform-tools"
BT=""
for v in $(ls "$SDK/build-tools" 2>/dev/null | sort -V -r); do
  if { [ -x "$SDK/build-tools/$v/zipalign" ] || [ -x "$SDK/build-tools/$v/zipalign.exe" ]; } && [ -f "$SDK/build-tools/$v/lib/apksigner.jar" ]; then
    BT="$SDK/build-tools/$v"; break
  fi
done
[ -n "$BT" ] || setup "no build tools with zipalign and lib/apksigner.jar under $SDK/build-tools"
ZIPALIGN="$BT/zipalign"; [ -x "$ZIPALIGN" ] || ZIPALIGN="$BT/zipalign.exe"
JAVA=""
if [ -n "${JAVA_HOME:-}" ]; then
  for c in "$(posix "$JAVA_HOME")/bin/java" "$(posix "$JAVA_HOME")/bin/java.exe"; do [ -x "$c" ] && { JAVA="$c"; break; }; done
fi
[ -n "$JAVA" ] || JAVA="$(command -v java 2>/dev/null || true)"
[ -n "$JAVA" ] || setup "no java (JAVA_HOME or the PATH) to run apksigner"
KEYSTORE="$(posix "${ANDROID_USER_HOME:-$HOME/.android}")/debug.keystore"
[ -f "$KEYSTORE" ] || setup "no local debug key store at $KEYSTORE"
APK="$ROOT/app/build/outputs/apk/release/app-release-unsigned.apk"
[ -f "$APK" ] || setup "no $APK - build it with run 1 of the JVM line (:app:assembleRelease ... :app:packageRelease --rerun)"
[ "$(timeout 30 "$ADB" get-state 2>/dev/null | tr -d '\r')" = device ] || setup "no single adb device (adb get-state; set ANDROID_SERIAL when several are attached)"

FAILS=0
pass() { echo "PASS $*"; }
fail() { echo "FAIL $*"; FAILS=$((FAILS + 1)); }
info() { echo "INFO $*"; }
adb_() { timeout 120 "$ADB" "$@" 2>&1 | tr -d '\r'; }

# ── 0. The device guard (code review S2): an emulator only, unless --allow-real-device ──
# Read before anything is signed, installed or uninstalled. A read that fails gives no "1" and no emulator serial,
# so a device that cannot be read is refused too.
SERIAL="$(timeout 30 "$ADB" get-serialno 2>/dev/null | tr -d '\r')"
KERNEL_QEMU="$(adb_ shell getprop ro.kernel.qemu)"
BOOT_QEMU="$(adb_ shell getprop ro.boot.qemu)"
READ="serial '$SERIAL', ro.kernel.qemu '$KERNEL_QEMU', ro.boot.qemu '$BOOT_QEMU'"
if [[ "$SERIAL" =~ ^emulator-[0-9]+$ ]] || [ "$KERNEL_QEMU" = 1 ] || [ "$BOOT_QEMU" = 1 ]; then
  pass "the device is an emulator ($READ)"
elif [ "$ALLOW_REAL" = 1 ]; then
  info "the device is not an emulator ($READ); allowed by --allow-real-device: its app.tangramplaytime install and that app's data are removed"
else
  setup "the device is not an emulator ($READ): this leg uninstalls $PKG, which erases the installed app's data (a phone's debug-build progress, settings and play times). Nothing was done on the device. Run it on an emulator, or pass --allow-real-device to accept that loss"
fi

info "device $SERIAL, API $(adb_ shell getprop ro.build.version.sdk)"
info "release APK $APK, modified $(date -r "$APK" '+%Y-%m-%d %H:%M:%S')"

# ── 1. The throwaway-signed copy, in the scratch dir only ──
mkdir -p "$SCRATCH_C" || setup "cannot make $SCRATCH_C"
COPY="$SCRATCH_C/app-release-unsigned.apk"
ALIGNED="$SCRATCH_C/app-release-aligned.apk"
SIGNED="$SCRATCH_C/app-release-throwaway.apk"
rm -f -- "${COPY:?}" "${ALIGNED:?}" "${SIGNED:?}"
cp -- "$APK" "$COPY" || setup "cannot copy the release APK to $COPY"
out="$("$ZIPALIGN" -p -f 4 "$(native "$COPY")" "$(native "$ALIGNED")" 2>&1)" || setup "zipalign failed: $out"
out="$("$JAVA" -jar "$(native "$BT/lib/apksigner.jar")" sign --ks "$(native "$KEYSTORE")" --ks-key-alias androiddebugkey \
  --ks-pass pass:android --key-pass pass:android --out "$(native "$SIGNED")" "$(native "$ALIGNED")" 2>&1)" \
  || setup "apksigner sign failed: $out"
cert="$("$JAVA" -jar "$(native "$BT/lib/apksigner.jar")" verify --print-certs "$(native "$SIGNED")" 2>&1 | tr -d '\r' | grep -m2 -E 'certificate DN|certificate SHA-256' | tr '\n' ' ')"
pass "signed a throwaway copy with the local debug key only: $SIGNED ($cert)"

# ── 2. Install it ──
info "adb uninstall $PKG: $(adb_ uninstall "$PKG" | tr '\n' ' ')"
out="$(adb_ install "$(native "$SIGNED")")"
if grep -q '^Success' <<<"$out"; then
  pass "adb install of the throwaway-signed release copy: Success"
  INSTALLED=1
else
  fail "adb install of the throwaway-signed release copy: $(tr '\n' ' ' <<<"$out")"
  INSTALLED=0
fi

# The permission entries of <package>'s block in `dumpsys package <package>` (stdin): one line "<section><TAB><entry>"
# per entry of a requested / install / runtime / declared permissions section, then "BLOCKS<TAB><n>".
perm_entries() {
  awk -v pkg="$1" '
    function ind(s) { match(s, /^[ ]*/); return RLENGTH }
    {
      sub(/\r$/, "")
      line = $0; i = ind(line); t = line; sub(/^[ ]+/, "", t)
      if (t == "") next
      if (inblock && i <= blockind) { inblock = 0; sect = "" }
      if (index(t, "Package [" pkg "]") == 1) { inblock = 1; blockind = i; blocks++; sect = ""; next }
      if (!inblock) next
      if (sect != "" && i <= sectind) sect = ""
      if (t == "requested permissions:" || t == "install permissions:" || t == "runtime permissions:" || t == "declared permissions:") {
        sect = t; sub(/ permissions:$/, "", sect); sectind = i; next
      }
      if (sect != "") print sect "\t" t
    }
    END { print "BLOCKS\t" (blocks + 0) }
  '
}

# ── 3. The installed package's permissions, and the parse's positive control ──
if [ "$INSTALLED" = 1 ]; then
  report="$(adb_ shell dumpsys package "$PKG" | perm_entries "$PKG")"
  blocks="$(awk -F '\t' '$1 == "BLOCKS" { print $2 }' <<<"$report")"
  if [ "${blocks:-0}" -ge 1 ]; then pass "dumpsys package $PKG: the package's block is read ($blocks block(s))"
  else fail "dumpsys package $PKG: no 'Package [$PKG]' block - the installed package cannot be judged"; fi
  for s in $SECTIONS; do
    entries="$(awk -F '\t' -v s="$s" '$1 == s { print $2 }' <<<"$report")"
    n="$(grep -c . <<<"$entries")"
    if [ "$n" = 0 ]; then pass "$PKG: $s permissions: 0 entries"
    else fail "$PKG: $s permissions: $n entries: $(tr '\n' ';' <<<"$entries")"; fi
  done
  control="$(adb_ shell dumpsys package "$CONTROL_PKG" | perm_entries "$CONTROL_PKG")"
  counts=""; total=0
  for s in $SECTIONS; do
    n="$(awk -F '\t' -v s="$s" '$1 == s { n++ } END { print n + 0 }' <<<"$control")"
    counts="$counts $s $n,"; total=$((total + n))
  done
  if [ "$total" -ge 1 ]; then pass "positive control: the same parse over $CONTROL_PKG finds $total entries (${counts%,} )"
  else fail "positive control: the same parse over $CONTROL_PKG finds no entry - the parse CANNOT see a permission"; fi
fi

# ── 4. The launch, only with --launch ──
if [ "$LAUNCH" = 1 ] && [ "$INSTALLED" = 1 ]; then
  adb_ logcat -c >/dev/null
  out="$(adb_ shell am start -W -n "$PKG/$ACTIVITY")"
  info "am start -W: $(tr '\n' ' ' <<<"$out")"
  if grep -q 'Status: ok' <<<"$out"; then pass "am start -W -n $PKG/$ACTIVITY: Status ok"
  else fail "am start -W -n $PKG/$ACTIVITY: no 'Status: ok'"; fi
  # Wait, bounded (30 s), until MainActivity has been the resumed activity for 3 reads in a row, 1 s apart.
  steady=0; seen=""
  for i in $(seq 1 30); do
    seen="$(adb_ shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' | head -2 | tr '\n' ' ')"
    if grep -qE "$PKG/(\.app\.MainActivity|$ACTIVITY)" <<<"$seen"; then steady=$((steady + 1)); else steady=0; fi
    [ "$steady" -ge 3 ] && break
    sleep 1
  done
  if [ "$steady" -ge 3 ]; then pass "MainActivity is the resumed activity, 3 reads in a row ($seen)"
  else fail "MainActivity is not steadily the resumed activity within 30 s (last read: $seen)"; fi
  crash="$(adb_ logcat -d | grep -A2 'FATAL EXCEPTION' | grep -E "Process: $PKG(,|$| )" | head -3)"
  if [ -z "$crash" ]; then pass "logcat holds no FATAL EXCEPTION for $PKG"
  else fail "logcat holds a FATAL EXCEPTION for $PKG: $(tr '\n' ' ' <<<"$crash")"; fi
fi

# ── 5. Uninstall ──
if [ "$INSTALLED" = 1 ]; then
  out="$(adb_ uninstall "$PKG")"
  if grep -q '^Success' <<<"$out"; then pass "adb uninstall $PKG: Success"
  else fail "adb uninstall $PKG: $(tr '\n' ' ' <<<"$out")"; fi
fi

if [ "$FAILS" -eq 0 ]; then echo "release_install: all checks passed"; exit 0; fi
echo "release_install: $FAILS check(s) failed"
exit 1
