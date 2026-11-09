# Backend changes for the Growfy mobile app

The mobile app (this repo) is the reference client for the TreeVook Google Business API
(`facebook_automation/backend`, base path `/api/v1`). Everything below is called by the app
already; the backend implements it to match. Section numbers (§) are referenced from code comments.

**Keep this file current:** whenever the app starts using an endpoint, field or param the backend
doesn't have, add it here in the same change.

## Status

Implemented on the backend branch `feature/mobile-api` (`facebook_automation`, not merged yet), with
e2e tests in `backend/test/mobile-api.e2e-spec.ts` and the contract in `backend/docs/20-mobile-app.md`.
Verified from the Android app against a local mock backend.

| § | Change | Backend |
|---|---|---|
| 1 | Mobile completion for Google Business OAuth | Done (`MOBILE_REDIRECT_URIS`) |
| 7 | `GET /business/reviews/:id` | Done |
| 3 | `GET /business/home`, `POST /business/setup/steps/:key/complete` | Done (`gbp_setup_steps`) |
| 6 | `POST /business/uploads` | Done |
| 5 | Inbox search (`q`) and cursor pagination | Done (`cursor` pages `pending`/`replied`; `all` unchanged for the web) |
| 4 | Notification preferences + push devices | Storage and endpoints done; **push delivery not built** (needs Firebase) |
| 9 | Removed locations stay removed | Done (`excluded_at`, plus `POST /business/locations/:id/restore`) |

Existing endpoints the app uses unchanged: `auth/otp/request`, `auth/otp/verify`, `auth/refresh`,
`auth/logout`, `me`, `business/connections` (GET, DELETE), `business/discover`, `business/locations`
(GET list, GET `:id`, PUT `:id`, DELETE `:id`), `business/inbox`, `business/reviews/:id/reply`
(POST, DELETE), `business/reviews/:id/ai-draft`, `business/reviews/:id/skip`, `business/insights`,
`business/templates` (GET, PUT `:id/visibility`), `business/posts` (GET, POST, PUT `:id`, DELETE `:id`).

## Conventions the app relies on

- Every request sends `X-Client: mobile` and `Authorization: Bearer <accessToken>`.
- Tokens come back in the body of `otp/verify` and `auth/refresh` (`accessToken`, `refreshToken`).
  Refresh is `POST /auth/refresh { refreshToken }`; `409 SESSION_REFRESHED` makes the app retry with the
  stored token; any other failure signs the user out.
- Errors are `{ code, message }`; the app shows `message` verbatim, so keep it user-facing.
- The global `ValidationPipe` uses `forbidNonWhitelisted: true`. New optional fields/params must be added
  to the DTOs or the app's requests are rejected.

---

## §1 Mobile completion for Google Business OAuth (P0)

**Problem.** `POST /business/oauth/start` builds the callback from a web origin, and
`GET /business/oauth/callback` always redirects to `{origin}/business/connect`. A mobile app can't
get control back.

**Change.**

```
POST /api/v1/business/oauth/start
{ "client": "mobile", "redirectUri": "growfy://business/connect" }
→ 200 { "url": "https://accounts.google.com/o/oauth2/v2/auth?..." }
```

- Add optional `client` (`"web" | "mobile"`) and `redirectUri` to `StartOAuthDto`.
- When `client = "mobile"`, validate `redirectUri` against a new env allowlist
  `MOBILE_REDIRECT_URIS` (comma-separated, default `growfy://business/connect`) and store it in the
  encrypted `state`. Google's `redirect_uri` stays the backend's own https callback.
- In `callback`, when the state carries a mobile `redirectUri`, redirect there instead of the web page:
  - success: `growfy://business/connect?connected=<added + restored>`
  - failure: `growfy://business/connect?error=<user-facing message>`
- Nothing else changes: the backend still exchanges the code and stores the refresh token.

The app opens `url` in a Custom Tab / Safari and listens for the deep link (Android intent filter,
iOS `CFBundleURLTypes`). If the user closes the tab without the deep link, the app re-checks
`GET /business/connections` when it resumes.

**Not needed for the app:** Google *sign-in* (`auth/google/*`) is also web-only (cookies + web
redirect). The app signs in with phone OTP, which already returns tokens to mobile. If Google sign-in
is wanted on mobile later, it needs the same `client=mobile` treatment plus a one-time code exchanged
for tokens (`POST /auth/google/exchange { code } → tokens`).

## §3 Home summary and setup checklist (P1)

**Problem.** The web app computes profile health and the "Getting started" checklist in the browser
(`web/src/features/business/home-page.tsx`, `shell.tsx`), and stores "templates checked" in
localStorage. The API guidelines say clients never compute what's next.

```
GET /api/v1/business/home?locationId=<optional>
→ 200 {
  "health": { "score": 72, "note": "Reply to 3 reviews to reach 90+" },
  "setup": {
    "steps": [
      { "key": "connect_google",  "label": "Connect Google",        "done": true  },
      { "key": "pick_locations",  "label": "Pick locations",        "done": true  },
      { "key": "reply_mode",      "label": "Choose reply mode",     "done": true  },
      { "key": "first_reply",     "label": "Send your first reply", "done": false },
      { "key": "templates",       "label": "Check reply templates", "done": false }
    ]
  }
}

POST /api/v1/business/setup/steps/:key/complete   → 204
```

- Score: the web formula, moved server-side:
  `round(50 × replyRate + 30 × (a non-draft, non-failed post in the last 7 days) + 20 × (visible templates cover 1–5★ ? 1 : 0.5))`.
- Note: `score ≥ 90` "Looking great. Keep the streak going." · waiting reviews "Reply to N reviews to reach 90+"
  · no post this week "Post this week to push this up." · else "Turn on templates for every rating to push this up."
- `first_reply` is done once the workspace has sent a reply. `templates` is done once
  `POST …/steps/templates/complete` was called (the app calls it when the Templates screen opens).
  Store completion per workspace.
- The app hides the setup card when every step is done, and acts on keys: `templates` → Templates
  screen, `first_reply` → Reviews, anything else → onboarding.

## §4 Notifications (P2)

```
GET /api/v1/notifications/preferences
→ 200 { "newReviews": true, "lowRatings": true, "weeklySummary": false }

PUT /api/v1/notifications/preferences
{ "newReviews": true, "lowRatings": true, "weeklySummary": false }
→ 200 (same shape)
```

Per user. `lowRatings` = 1–3★, sent immediately. `weeklySummary` = Monday digest.

Push delivery (not wired in the app yet; needs a Firebase project and `google-services.json`):

```
POST   /api/v1/devices          { "platform": "android" | "ios", "token": "<FCM/APNs token>" } → 204
DELETE /api/v1/devices/:token   → 204   (called on sign-out)
```

Today the only alert is the Slack escalation webhook for 1–2★ reviews.

## §5 Inbox search and pagination (P1)

```
GET /api/v1/business/inbox?filter=pending&locationId=…&q=<text>&cursor=<opaque>&limit=<1–100>
→ 200 { "data": [...], "counts": {...}, "approvable": [...], "nextCursor": "…" | null }
```

- `q`: case-insensitive match on author name and review text. `counts` reflect the location filter,
  not the search.
- Cursor pagination per the API guidelines (default 20, max 100). Today the inbox returns everything.
- Add `q`, `cursor`, `limit` to `BusinessInboxQueryDto`. The app only sends `q` while searching and
  `cursor` when scrolling past the first page.

## §6 Photo upload for posts (P1)

**Problem.** `content.mediaUrl` must be a public https URL. The only upload,
`POST /business/locations/:id/media`, also adds the photo to the public Google gallery.

```
POST /api/v1/business/uploads        (multipart/form-data, field "file")
→ 201 { "url": "https://treevook.com/media/gbp-post-….jpg" }
```

Same file rules as the media upload (JPEG/PNG, 10 KB–5 MB, at least 250 × 250), stored under
`/media/`, **not** sent to the Google gallery. The app checks the 5 MB limit before uploading.

## §7 Single review (P0)

```
GET /api/v1/business/reviews/:id → 200 GbpReviewDto   (404 outside the caller's workspace)
```

The Reply screen opens by review id (from Home, Reviews, and "next waiting review").

## §8 No undo for deleted posts (no change)

Deleting a post removes it from Google; re-creating it republishes it as a new post. The app has no
Undo for post deletion. Undo for replies uses the existing `DELETE /business/reviews/:id/reply`.

## §9 Removed locations stay removed (P2)

Onboarding imports every location (`POST /business/discover`) and then `DELETE`s the ones the owner
unticked. `deleteLocation` hard-deletes, so the daily discovery job re-imports them. Record an
exclusion (e.g. `gbp_locations.excluded_at`) on delete and skip excluded locations in discovery until
the owner adds them back.

---

## Gaps in the mobile design (for design, not backend)

- **Sign-in.** The design opens on "Connect Google" but the API needs a TreeVook session first. The app
  adds a phone + code screen in the same layout (`feature/auth/LoginScreen.kt`).
- **Offer and Event posts.** Google requires a title and start/end dates; buttons other than Call need an
  https link; offers can carry a coupon code. The app adds these fields to New post.
- **Failed states.** Posts and replies can be `failed`, locations can be unverified or paused. The app
  shows a "Failed" pill and the error text on posts; locations' sync errors aren't shown yet.
- **More sub-pages.** Reply templates, Google accounts and Notifications follow the Android prototype,
  not the mobile design file.

## Client-side follow-ups (app, not backend)

- Android only for now; iOS is out of scope (its photo picker is a stub and it hasn't been compiled).
- Push notifications (FCM/APNs) once §4 ships.
- Tokens are stored in DataStore; move to Keystore/Keychain-backed storage before launch.
