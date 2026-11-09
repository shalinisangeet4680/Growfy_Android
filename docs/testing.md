# Testing the Android app on a device or emulator

Prefer **deep links + test tags** over screen coordinates: they survive layout changes and work on any
screen size. `scripts/ui.sh` wraps both.

## Setup

```bash
# backend in mock mode on :4000 (see README), then:
adb reverse tcp:4000 tcp:4000          # again after every emulator restart
./gradlew :androidApp:installDevDebug
```

Sign in: `8434230999`, code `963101`. The backend allows one code request per 30 s and 5 per 15 min
per number.

## scripts/ui.sh

| Command | Does |
|---|---|
| `scripts/ui.sh link reviews` | Opens `growfy://app/reviews` |
| `scripts/ui.sh tap tab_posts` | Taps the element with that test tag |
| `scripts/ui.sh type login_phone 8434230999` | Taps a field, then types |
| `scripts/ui.sh swipe-left review_row_0` | Swipe-to-send on the first review |
| `scripts/ui.sh wait error_state` / `wait loading_state gone` | Waits up to 30 s |
| `scripts/ui.sh exists home_focus_send` | Exit code 0 if on screen |
| `scripts/ui.sh tags` | Lists tags on screen with bounds |

Tags are exposed to uiautomator through `testTagsAsResourceId` in `MainActivity`. Read on-screen text with
`adb shell uiautomator dump /sdcard/ui.xml && adb exec-out cat /sdcard/ui.xml`.

## Deep links (`growfy://app/...`)

Handled once signed in (`navigation/AppLinks.kt`).

| Link | Opens |
|---|---|
| `home`, `reviews`, `posts`, `more` | That tab |
| `reply/<reviewId>` | Reply |
| `compose` | New post |
| `post/<postId>` | Edit post |
| `templates`, `accounts`, `notifications` | Settings sub-screens |

`growfy://business/connect?connected=N` is the Google OAuth return link (docs/backend-gaps.md §1).

## Test tags

All ids live in `core/ui/TestTags.kt`.

| Area | Tags |
|---|---|
| States | `loading_state`, `error_state`, `error_retry`, `empty_state` |
| Sign in | `login_phone`, `login_code`, `login_submit` |
| Bottom bar | `tab_home`, `tab_reviews`, `tab_posts`, `tab_more` |
| Home | `home_focus_send`, `home_focus_edit` |
| Reviews | `reviews_search`, `reviews_filter_waiting`, `reviews_filter_replied`, `reviews_tip_dismiss`, `review_row_<i>` |
| Reply | `reply_text`, `reply_send`, `reply_skip`, `reply_tone_warmer` / `_shorter` / `_formal` |
| Posts | `posts_new`, `posts_calendar`, `posts_filter_all` / `_scheduled` / `_live` / `_drafts`, `post_card_<i>` |
| New post | `compose_summary`, `compose_publish`, `compose_draft`, `compose_close` |
| More | `location_<i>_auto`, `location_<i>_approval`, `more_templates`, `more_accounts`, `more_notifications`, `more_sign_out` |

## Fallback coordinates (Medium_Phone_API_37, 1080 × 2400)

Only for when a tag isn't available. They shift when the keyboard is open.

| Target | x, y |
|---|---|
| Bottom tabs (Home / Reviews / Posts / More) | 127 / 402 / 677 / 952, 2220 |
| Sign in: phone/code field | 540, 725 |
| Sign in: Send code / Continue (keyboard open) | 540, 1369 |
| Sign in: Send code / Continue (keyboard closed) | 540, 2190 |
| Reviews: first row | 540, 500 |
| Reviews: swipe a row | `input swipe 1000 <y> 150 <y> 900` (slow; a fast flick may not register) |
| Posts: New post FAB | 865, 2010 |
| New post: text field / Post button | 540, 800 / 945, 147 |
| More: 2nd location Auto | 312, 990 |
| System "isn't responding" dialog: Wait | 540, 1327 |

## Gotchas

- Google connect in mock mode opens `http://localhost:3200/api/v1/business/oauth/callback` (the web app's
  proxy). Without the web app, forward that port to the backend too: `adb reverse tcp:3200 tcp:4000`.
  On a fresh emulator, skip Chrome's first-run screen once:
  `adb shell 'echo "_ --disable-fre --no-default-browser-check --no-first-run" > /data/local/tmp/chrome-command-line' && adb shell am set-debug-app --persistent com.android.chrome`.
- After `adb install -r`, the system package-update screen can steal focus and cause a fake ANR. If taps
  stop landing (`dumpsys window | grep mCurrentFocus` shows null or ANR), reboot the emulator and
  re-run `adb reverse`.
- The first launch after a reinstall takes several seconds (DataStore class verification in debug builds).
  `scripts/ui.sh wait tab_home` handles it.
- `adb shell input text` can drop characters while the keyboard is switching; wait for the field first.
