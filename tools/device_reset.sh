#!/usr/bin/env bash
# WO-003 TASK-015 · seam S-16 (tasks.md § WO-003 › Seams); WO-006 TASK-028 · seam S-37 (tasks.md § WO-006 › Seams)
# · platform-notes/android.md § Device hygiene:
# reset the app's data and the device-level state the WO-006 kit and the floor channel change on the attached
# device before a device step, and check it after. The kit restores everything itself (tasks.md § WO-006 seam
# note 3); this script is the net under it, and its --check names any leftover.
#
#   bash tools/device_reset.sh           adb shell pm clear app.tangramplaytime (not installed: nothing to clear),
#                                        then every device-level item below that is off its baseline is restored
#                                        and read back
#   bash tools/device_reset.sh --check   clean = the app is not installed, or its data directory holds no file
#                                        (listed through `run-as`, so a debuggable install; the empty `cache` /
#                                        `code_cache` directories the platform makes are not state), AND every
#                                        device-level item below is at its baseline
#
# Device-level items (S-37), each named in the output; an item whose command the API level lacks is skipped by
# ro.build.version.sdk, never reported clean:
#   display-size, display-density   no `wm size` / `wm density` override          reset: wm size|density reset
#   rotation                        accelerometer_rotation 1, user_rotation 0      reset: settings put system ...
#   show-ime-with-hard-keyboard     the image's own system default as the settings provider records it
#                                   (`dumpsys settings`: default:N defaultSystemSet:true): 0 on the two API 37
#                                   images, 1 on Phone_API_26 (measured 2026-10-17)  reset: settings put secure ...
#   app-locales (API 33+)           no app locales for app.tangramplaytime         reset: pm clear, then
#                                                                                  cmd locale set-app-locales (empty)
#   device-locale                   the device's locale list is en-US: system_locales (when null: persist.sys.locale,
#                                   then ro.product.locale), persist.sys.locale empty or en-US, and on images whose
#                                   `cmd locale` has get-device-locale, that primary locale en-US.
#                                   reset: where `cmd locale` has set-device-locale, set-device-locale en-US (and
#                                   system_locales en-US); elsewhere (Phone_API_26) reported only, with the route
#   airplane-mode                   airplane_mode_on 0                             reset: API 30+ cmd connectivity
#                                                                                  airplane-mode disable; below 30
#                                                                                  the root route (adb root, the
#                                                                                  setting and the AIRPLANE_MODE
#                                                                                  broadcast as root)
#   adbd-root                       adbd not running as root (`id -u` not 0)       reset: adb unroot (last)
#
# Exit: 0 reset done / clean; 1 reset failed / NOT clean (the files and the items off their baseline are listed);
# 2 no adb, no device, a bad argument, or state that cannot be read (a non-debuggable install, a setting or
# property that reads nothing usable) - an unknown state is never "clean", and 2 wins over 1.
# adb comes from $ANDROID_HOME (or $ANDROID_SDK_ROOT)/platform-tools, else the PATH; ANDROID_SERIAL picks the
# device when more than one is attached.
set -uo pipefail
PKG=app.tangramplaytime
PKG_RE="${PKG//./\\.}"
LOCALE=en-US
# The floor's proven locale route (TASK-028 probe, 2026-10-17): a `-change-locale` boot sets persist.sys.locale
# only, and on API 26 the system_locales setting wins over it, so neither a fi-FI nor an en-US boot moves it; and a
# locale stored in vold's encryption footer (SystemLocale, written by any user-set locale change) wins over both at
# every framework start, so the route clears it first.
FLOOR_LOCALE_ROUTE="adb root; adb shell 'vdc cryptfs setfield SystemLocale \"\"'; adb shell settings put system system_locales $LOCALE; adb shell setprop persist.sys.locale $LOCALE; wait until /data/system/users/0/settings_system.xml holds it; adb shell stop; adb shell start; adb unroot"

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
case "${1:-}" in
  ""|--check) ;;
  *) echo "usage: bash tools/device_reset.sh [--check]" >&2; exit 2 ;;
esac

sh_() { "$ADB" shell "$1" 2>&1 | tr -d '\r'; }
is_int() { case "$1" in ''|*[!0-9]*) return 1 ;; *) return 0 ;; esac; }
installed() { [ -n "$("$ADB" shell pm path "$PKG" 2>/dev/null | tr -d '\r')" ]; }

SDK="$(sh_ 'getprop ro.build.version.sdk')"
if ! is_int "$SDK"; then echo "device_reset: unknown - ro.build.version.sdk reads '$SDK'" >&2; exit 2; fi
SERIAL="$("$ADB" get-serialno 2>/dev/null | tr -d '\r')"

LOCALE_HELP=""
if [ "$SDK" -ge 33 ]; then LOCALE_HELP="$(sh_ 'cmd locale help')"; fi
# grep on a here-string, never `adb | grep -q`: under pipefail the early exit of grep fails the pipe (SIGPIPE).
locale_has() { grep -qE "^[[:space:]]+$1([[:space:]]|\$)" <<<"$LOCALE_HELP"; }

# Switch adbd to root or back and wait until `id -u` agrees (adbd restarts, so the device drops for a moment).
switch_adbd() {
  local out i u=""
  out="$("$ADB" "$1" 2>&1 | tr -d '\r')"
  case "$out" in *"cannot run as root"*) echo "    adb $1: $out"; return 1 ;; esac
  for i in $(seq 1 30); do
    if [ "$("$ADB" get-state 2>/dev/null | tr -d '\r')" = device ]; then
      u="$(sh_ 'id -u')"
      if [ "$1" = root ] && [ "$u" = 0 ]; then return 0; fi
      if [ "$1" = unroot ] && is_int "$u" && [ "$u" != 0 ]; then return 0; fi
    fi
    sleep 1
  done
  echo "    adb $1: id -u still reads '$u' after 30 s"
  return 1
}

# Each read_* sets ST (ok | off | unk | skip) and MSG; each restore_* acts only on an item read as off.
read_display() {  # $1 = size | density
  local out phys over
  if ! out="$(sh_ "wm $1")"; then ST=unk; MSG="wm $1 failed: $out"; return; fi
  phys="$(sed -n 's/^Physical [a-z]*: //p' <<<"$out" | head -1)"
  over="$(sed -n 's/^Override [a-z]*: //p' <<<"$out" | head -1)"
  if [ -z "$phys" ]; then ST=unk; MSG="wm $1 printed no physical value: $out"; return; fi
  if [ -n "$over" ]; then ST=off; MSG="override $over (physical $phys)"; else ST=ok; MSG="no override (physical $phys)"; fi
}
restore_display() { sh_ "wm $1 reset" >/dev/null; }

read_rotation() {
  local acc usr
  acc="$(sh_ 'settings get system accelerometer_rotation')"
  usr="$(sh_ 'settings get system user_rotation')"
  if ! is_int "$acc" || ! is_int "$usr"; then ST=unk; MSG="accelerometer_rotation '$acc', user_rotation '$usr'"; return; fi
  if [ "$acc" = 1 ] && [ "$usr" = 0 ]; then ST=ok; else ST=off; fi
  MSG="accelerometer_rotation $acc, user_rotation $usr (baseline 1, 0)"
}
restore_rotation() { sh_ 'settings put system user_rotation 0; settings put system accelerometer_rotation 1' >/dev/null; }

IME_DEFAULT=""
read_ime() {
  local v
  v="$(sh_ 'settings get secure show_ime_with_hard_keyboard')"
  IME_DEFAULT="$(sh_ 'dumpsys settings' | grep -m1 -E '^[[:space:]]*_id:[0-9]+ name:show_ime_with_hard_keyboard ' \
    | sed -n 's/.* default:\([^ ]*\) defaultSystemSet:true.*/\1/p')"
  if ! is_int "$IME_DEFAULT"; then
    ST=unk; MSG="show_ime_with_hard_keyboard '$v'; dumpsys settings records no system default for it"; return
  fi
  if [ "$v" = "$IME_DEFAULT" ]; then ST=ok; else ST=off; fi
  MSG="show_ime_with_hard_keyboard $v (this image's system default $IME_DEFAULT)"
}
restore_ime() { sh_ "settings put secure show_ime_with_hard_keyboard $IME_DEFAULT" >/dev/null; }

read_app_locales() {
  local out l
  if [ "$SDK" -lt 33 ]; then ST=skip; MSG="API $SDK has no per-app locales (cmd locale is API 33+)"; return; fi
  if ! installed; then ST=ok; MSG="$PKG is not installed"; return; fi
  out="$(sh_ "cmd locale get-app-locales $PKG")"
  if ! grep -qE "^Locales for $PKG_RE for user [0-9]+ are \[.*\]\$" <<<"$out"; then
    ST=unk; MSG="cmd locale get-app-locales printed: $out"; return
  fi
  l="$(sed -nE "s/^Locales for $PKG_RE for user [0-9]+ are \[(.*)\]\$/\1/p" <<<"$out")"
  if [ -z "$l" ]; then ST=ok; MSG="none set for $PKG"; else ST=off; MSG="[$l] set for $PKG"; fi
}
restore_app_locales() { sh_ "cmd locale set-app-locales $PKG" >/dev/null; }

read_device_locale() {
  local sl pl rl eff prim="" tag='[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*'
  sl="$(sh_ 'settings get system system_locales')"
  pl="$(sh_ 'getprop persist.sys.locale')"
  rl="$(sh_ 'getprop ro.product.locale')"
  if [ -n "$sl" ] && [ "$sl" != null ]; then eff="$sl"; elif [ -n "$pl" ]; then eff="$pl"; else eff="$rl"; fi
  MSG="system_locales '$sl', persist.sys.locale '$pl'"
  if locale_has get-device-locale; then prim="$(sh_ 'cmd locale get-device-locale')"; MSG="$MSG, primary '$prim'"; fi
  if ! grep -qE "^$tag(,$tag)*\$" <<<"$eff" || { [ -n "$pl" ] && ! grep -qE "^$tag\$" <<<"$pl"; } \
     || { locale_has get-device-locale && ! grep -qE "^$tag\$" <<<"$prim"; }; then
    ST=unk; MSG="$MSG, ro.product.locale '$rl' - no usable locale"; return
  fi
  if [ "$eff" = "$LOCALE" ] && { [ -z "$pl" ] || [ "$pl" = "$LOCALE" ]; } && { [ -z "$prim" ] || [ "$prim" = "$LOCALE" ]; }; then
    ST=ok; MSG="$MSG (baseline: the list $LOCALE)"
  else
    ST=off; MSG="$MSG (baseline: the list $LOCALE)"
    if ! locale_has set-device-locale; then MSG="$MSG - reported only on API $SDK; restore: $FLOOR_LOCALE_ROUTE"; fi
  fi
}
restore_device_locale() {
  local sl
  if ! locale_has set-device-locale; then return; fi
  sh_ "cmd locale set-device-locale $LOCALE" >/dev/null
  # set-device-locale leaves a stale list in system_locales when the primary already reads en-US (measured on API 37)
  sl="$(sh_ 'settings get system system_locales')"
  if [ "$sl" != null ] && [ "$sl" != "$LOCALE" ]; then sh_ "settings put system system_locales $LOCALE" >/dev/null; fi
}

read_airplane() {
  local v
  v="$(sh_ 'settings get global airplane_mode_on')"
  if ! is_int "$v"; then ST=unk; MSG="airplane_mode_on '$v'"; return; fi
  if [ "$v" = 0 ]; then ST=ok; else ST=off; fi
  MSG="airplane_mode_on $v (baseline 0)"
}
restore_airplane() {
  if [ "$SDK" -ge 30 ]; then sh_ 'cmd connectivity airplane-mode disable' >/dev/null; return; fi
  # Below API 30 the AIRPLANE_MODE broadcast is protected: the root route (TASK-028 floor probe, 2026-10-17).
  # adbd is left as root here; the adbd-root item, last, unroots it.
  if ! switch_adbd root; then echo "    adb root refused - airplane mode cannot be switched off on API $SDK"; return; fi
  sh_ 'settings put global airplane_mode_on 0; am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false' >/dev/null
}

read_adbd() {
  local u
  u="$(sh_ 'id -u')"
  if ! is_int "$u"; then ST=unk; MSG="id -u reads '$u'"; return; fi
  if [ "$u" = 0 ]; then ST=off; MSG="adbd runs as root (id -u 0)"; else ST=ok; MSG="adbd runs as uid $u"; fi
}
restore_adbd() { switch_adbd unroot || true; }

ITEMS="display-size display-density rotation show-ime-with-hard-keyboard app-locales device-locale airplane-mode adbd-root"
read_item() {
  case "$1" in
    display-size) read_display size ;;
    display-density) read_display density ;;
    rotation) read_rotation ;;
    show-ime-with-hard-keyboard) read_ime ;;
    app-locales) read_app_locales ;;
    device-locale) read_device_locale ;;
    airplane-mode) read_airplane ;;
    adbd-root) read_adbd ;;
  esac
}
restore_item() {
  case "$1" in
    display-size) restore_display size ;;
    display-density) restore_display density ;;
    rotation) restore_rotation ;;
    show-ime-with-hard-keyboard) restore_ime ;;
    app-locales) restore_app_locales ;;
    device-locale) restore_device_locale ;;
    airplane-mode) restore_airplane ;;
    adbd-root) restore_adbd ;;
  esac
}

OFF=(); UNK=()
verdict() {  # $1 = reset | check
  if [ "${#UNK[@]}" -gt 0 ]; then echo "$1: unknown - ${UNK[*]}${OFF[*]:+ (also off: ${OFF[*]})}" >&2; exit 2; fi
  if [ "${#OFF[@]}" -gt 0 ]; then
    if [ "$1" = reset ]; then echo "reset: failed - ${OFF[*]}" >&2; else echo "check: NOT clean - ${OFF[*]}"; fi
    exit 1
  fi
  if [ "$1" = reset ]; then echo "reset: done"; else echo "check: clean"; fi
  exit 0
}

case "${1:-}" in
  "")
    if ! installed; then
      echo "reset: $PKG is not installed - nothing to clear"
    else
      out="$("$ADB" shell pm clear "$PKG" 2>&1 | tr -d '\r')"
      if [ "$out" = "Success" ]; then echo "reset: pm clear $PKG -> Success"
      else echo "reset: pm clear $PKG failed: $out" >&2; OFF+=(app-data); fi
    fi
    echo "reset: device-level items on $SERIAL (API $SDK):"
    for it in $ITEMS; do
      read_item "$it"
      case "$ST" in
        ok) echo "  $it: ok - $MSG" ;;
        skip) echo "  $it: skipped - $MSG" ;;
        unk) echo "  $it: unknown - $MSG"; UNK+=("$it") ;;
        off)
          before="$MSG"
          restore_item "$it"
          read_item "$it"
          case "$ST" in
            ok) echo "  $it: restored - was: $before" ;;
            unk) echo "  $it: unknown after the restore - $MSG"; UNK+=("$it") ;;
            *) echo "  $it: NOT restored - $MSG"; OFF+=("$it") ;;
          esac
          ;;
      esac
    done
    verdict reset
    ;;
  --check)
    echo "check: $PKG and the device-level items on $SERIAL (API $SDK):"
    if ! installed; then
      echo "  app-data: ok - $PKG is not installed"
    elif ! files="$("$ADB" shell run-as "$PKG" find . -type f 2>&1 | tr -d '\r')"; then
      echo "  app-data: unknown - cannot list $PKG's data: $files"; UNK+=(app-data)
    elif [ -z "$files" ]; then
      echo "  app-data: ok - $PKG's data directory holds no file"
    else
      echo "  app-data: NOT clean - $PKG's data directory holds:"
      echo "$files" | sed 's/^/    /'
      OFF+=(app-data)
    fi
    for it in $ITEMS; do
      read_item "$it"
      case "$ST" in
        ok) echo "  $it: ok - $MSG" ;;
        skip) echo "  $it: skipped - $MSG" ;;
        unk) echo "  $it: unknown - $MSG"; UNK+=("$it") ;;
        off) echo "  $it: NOT clean - $MSG"; OFF+=("$it") ;;
      esac
    done
    verdict check
    ;;
esac
