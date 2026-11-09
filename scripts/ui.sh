#!/usr/bin/env bash
# Drive the dev app by test tag / deep link instead of screen coordinates. See docs/testing.md.
#
#   scripts/ui.sh link reviews            open growfy://app/reviews
#   scripts/ui.sh tap tab_posts           tap the element whose testTag is tab_posts
#   scripts/ui.sh type login_phone 8434230999
#   scripts/ui.sh swipe-left review_row_0
#   scripts/ui.sh exists error_state      exit 0 if the tag is on screen
#   scripts/ui.sh tags                    list tags currently on screen with their bounds
#   scripts/ui.sh wait loading_state gone wait until a tag appears (default) or disappears
set -euo pipefail

ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
PKG="${PKG:-org.mobile.growfy.dev}"

dump() {
  "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  "$ADB" exec-out cat /sdcard/ui.xml
}

# Prints "x y" (center) for a resource-id, or nothing.
center() {
  dump | python3 -I -c '
import re, sys
tag = sys.argv[1]
xml = sys.stdin.read()
for node in re.findall(r"<node [^>]*>", xml):
    rid = re.search(r"resource-id=\"([^\"]*)\"", node)
    if rid and rid.group(1).split("/")[-1] == tag:
        x1, y1, x2, y2 = map(int, re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", node).groups())
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
' "$1"
}

# Sets X and Y to the tag's center, or exits when it isn't on screen.
locate() {
  local c; c="$(center "$1")"
  [ -n "$c" ] || { echo "tag not on screen: $1" >&2; exit 1; }
  read -r X Y <<<"$c"
}

case "${1:-}" in
  link) "$ADB" shell am start -W -a android.intent.action.VIEW -d "growfy://app/$2" "$PKG" >/dev/null ;;
  tap) locate "$2"; "$ADB" shell input tap "$X" "$Y" ;;
  type)
    locate "$2"; "$ADB" shell input tap "$X" "$Y"; sleep 2
    "$ADB" shell input text "${3// /%s}" ;;
  swipe-left) locate "$2"; "$ADB" shell input swipe 1000 "$Y" 150 "$Y" 900 ;;
  exists) [ -n "$(center "$2")" ] ;;
  wait)
    for _ in $(seq 1 30); do
      found="$(center "$2")"
      if [ "${3:-}" = "gone" ]; then [ -z "$found" ] && exit 0; else [ -n "$found" ] && exit 0; fi
      sleep 1
    done
    echo "timed out waiting for $2 ${3:-}" >&2; exit 1 ;;
  tags)
    dump | python3 -I -c '
import re, sys
for node in re.findall(r"<node [^>]*>", sys.stdin.read()):
    rid = re.search(r"resource-id=\"([^\"]*)\"", node)
    if rid and rid.group(1) and "/" not in rid.group(1):
        print(rid.group(1), re.search(r"bounds=\"([^\"]*)\"", node).group(1))
' ;;
  *) sed -n '2,12p' "$0"; exit 1 ;;
esac
