# Unsaid — Full Context for Claude Code

App: intimate, pseudonymous connection app for CU Chandigarh students.
North star: *a held breath.*
Stack: React 19 + Vite, React Router 7, @stomp/stompjs, Framer Motion, deployed on Vercel.
Backend repo: `../campuscrush` (Spring Boot 4, PostgreSQL on Supabase, deployed on Render).

---

## Design system — never fork, always reuse

**Tokens** live in `src/index.css` `:root` (light) and `[data-theme="dark"]` (dark).
Key tokens: `--bg`, `--surface`, `--surface-2`, `--text`, `--text-2`, `--text-3`, `--accent` (#C75D3F light / #D4704E dark), `--border`, `--border-2`, `--radius`, `--font` (Inter), `--font-serif` (Cormorant Garamond).

**Auth screens** have their own `--auth-*` token set (34 tokens, light in `:root`, dark in `[data-theme="dark"]`). Use these on `/auth`, `/login`, `/register`, `/verify` — never hardcode hex values there.

**Theme**: `ThemeContext` sets `data-theme` on `document.documentElement`. Light is the default (warm cream `#F4EFE6`). Dark is dusk (`#1C1814`). `[data-theme="dark"]` overrides apply. Always test both.

**Shared components**:
- `<Logo size={N} settled />` — SVG three-dot wordmark, terracotta. Use everywhere.
- `<HeartLoader />` — spinner, use sparingly (prefer skeletons).
- `<MutualCrushOverlay />` — full-screen mutual crush celebration.
- `<SegmentedInput />` — 6-box OTP input with paste/keyboard/a11y.
- `<TabBar active="inbox"|"feed" />` — fixed bottom nav (Inbox / Feed). Present on Dashboard and Feed pages.

**Animation config**:
- `--splash-ease: cubic-bezier(0.16, 1, 0.3, 1)` — primary easing
- `--spring: cubic-bezier(0.34, 1.56, 0.64, 1)` — bouncy press
- `fadeRise` keyframe — standard entrance (opacity 0→1, translateY 10→0)
- `breathe` keyframe — box-shadow pulse for idle CTAs
- `--ri` CSS custom property — stagger: `animation-delay: calc(var(--ri, 0) * 55ms)`
- Always add `prefers-reduced-motion` overrides for new animations

**Mobile-first rules**:
- Height: `100dvh`. Scrollable containers: `overflow-y: auto` (not `hidden`).
- Safe areas: `max(Xpx, calc(env(safe-area-inset-top/bottom/left/right) + Ypx))`
- Min tap targets: 44px. Composer input font: 16px (prevents iOS zoom).
- `touch-action: manipulation` globally (already set).
- `@media (hover: hover)` guards on all hover states.
- `.chat-page` has `position: relative` — required so the `.chat-scroll-btn` (position: absolute) centers within the column.
- `.fab` z-index is 35 (above tab bar's 30). Bottom override: `max(70px, calc(env(safe-area-inset-bottom) + 74px))` — defined after the tab-bar block so it wins.

---

## Routes & components

| Route | Component | Notes |
|---|---|---|
| `/` | `Splash` | Landing, entrance animation |
| `/auth` | `AuthChoice` | Login vs Register choice |
| `/login` | `Login` | Email only, navigates to `/verify` |
| `/register` | `Register` | Email only, navigates to `/verify` |
| `/verify` | `Verify` | Shared OTP screen; gets `{email, flow}` from `location.state` |
| `/dashboard` | `Dashboard` | Inbox — all confession threads. Has TabBar + FAB. |
| `/feed` | `Feed` | Public confession feed. Has TabBar + FAB. |
| `/chat/:confessionId` | `Chat` | Thread view with reveal guessing game panels. |

`ProtectedRoute` → redirects to `/login` if not authed.
`PublicRoute` → redirects to `/dashboard` if already authed.

---

## Auth

Passwordless OTP. Flow:
1. `POST /api/public/auth/login` or `/register` → sends 6-digit OTP to `@cuchd.in` email
2. `POST /api/public/auth/verify-otp` → returns JWT
3. JWT stored in `localStorage` as `token`. `AuthContext` manages `user` state.

Only `@cuchd.in` emails accepted. Validation regex: `/^[a-zA-Z0-9]{4,20}@cuchd\.in$/i` — lives in `src/utils/auth.js`.

OTP rate limits (backend): 60s resend cooldown, 5-attempt brute-force lockout (15 min).

---

## Data model

**ConfessionResponse** (from `/api/confessions`):
```
{ id, otherUserAlias, otherUserPublicId, icebreakerMessage, state,
  createdAt, isSender, hasUnread, isRevealed, showMutualAnimation, isBlocker }
```

**ConfessionState** enum: `INVITED → CREATED → UNLOCKED → REVEALED / BLOCKED`
- `INVITED`: sent to unregistered roll number
- `CREATED`: receiver registered, not yet accepted
- `UNLOCKED`: receiver accepted, free chat
- `REVEALED`: mutual crush (both confessed to each other) — completely separate from identity reveal
- `BLOCKED`: either party blocked

**isBlocker** (boolean on ConfessionResponse): true only for the user who initiated the block. Only the blocker sees the Unblock button. The blocked non-initiating user sees the blocked banner only.

**Alias logic**:
- Sender always sees receiver's roll number (e.g. "23BAI70503")
- Receiver sees sender's `displayAlias` (e.g. "Golden Viper") until identity is revealed
- After reveal (`isRevealed = true`), receiver also sees sender's roll number — backend returns it as `otherUserAlias`

**MessageResponse** (from `/api/messages/:id`):
```
{ id, from: "SELF"|"OTHER", content, type: TEXT|REVEAL|MUTUAL, sentAt, senderPublicId }
```
Server always prepends icebreaker as `id: -1` (first item in message list).

**RevealStateResponse** (from `/api/confessions/:id/reveal-state`):
```
{
  role: "SENDER" | "RECEIVER",
  status: "HIDDEN" | "GUESSED" | "MANUALLY_REVEALED",

  // Receiver-only (null for sender):
  canGuess, guessesRemaining, hintsUnlocked, hints: string[], locked,

  // Sender-only (null for receiver):
  guessingEnabled, kitComplete, hint1, hint2, hint3,
  guesses: [{ guessedRoll, correct, guessedAt }]
}
```

**FeedItemResponse** (from `/api/feed`):
```
{ id, content, viewCount, createdAt }
```
Feed posts expire 24h after `createdAt`. Max 3 posts/day per user. Max 300 chars. Auto-hidden at 3 reports.

---

## API endpoints (frontend-relevant)

```
GET    /api/confessions                        list all threads
GET    /api/confessions/:id                    single thread
POST   /api/confessions/:receiverId            send confession (body = message string)
POST   /api/confessions/:id/reply             receiver accepts (CREATED → UNLOCKED)
POST   /api/confessions/:id/block             either party blocks
POST   /api/confessions/:id/unblock           blocker only (→ UNLOCKED)
POST   /api/confessions/:id/reveal            sender only — manual identity reveal
POST   /api/confessions/:id/read              mark thread read
POST   /api/confessions/:id/mutual-seen       clear mutual animation flag
POST   /api/confessions/:id/invite            send invite email to unregistered roll number
GET    /api/messages/:confessionId            message list

GET    /api/confessions/:id/reveal-state      role-gated reveal/guess state
POST   /api/confessions/:id/guess             { rollNumber } → { correct, guessesRemaining, hintsUnlocked, status }
GET    /api/reveal/kit                        sender's current hint kit
PUT    /api/reveal/kit                        { hint1, hint2, hint3, guessingEnabled }

GET    /api/feed                              unseen-first feed (20 items, 24h expiry)
POST   /api/feed                              { content } → post anonymously
POST   /api/feed/:id/view                     record view (fire-and-forget, 204)
POST   /api/feed/:id/report                   { reason } → report (204)
```

**Reveal is two-path**:
1. Manual: sender taps "Reveal yourself" → `POST /reveal` → REVEAL system message in chat
2. Guessing: receiver submits roll → `POST /guess` → on correct, backend creates REVEAL message + pushes WebSocket events to both parties

`REVEALED` (ConfessionState) = mutual crush — completely separate from identity reveal (`isRevealed` boolean).

---

## Real-time (WebSocket)

Service: `src/services/socket.js` — singleton STOMP client.
Connect: `socket.connect(token)`. Send: `socket.sendMessage(confessionId, content)`.
Subscribe pattern: `socket.subscribe(topic, callback) → Promise<subscription>`.

Topics:
- `/user/queue/confessions` — dashboard refresh (new confessions, state changes)
- `/user/queue/confession/:id` — thread events (mix of bare strings and JSON):

```
Bare strings (legacy):  "MUTUAL" | "REVEALED" | "NEW_MESSAGE" | "UPDATE"
JSON events (new):      { type: "GUESS", guessedRoll, correct, guessesRemaining }
                        { type: "GUESSED", guessedRoll, correct: true, guessesRemaining: 0 }
```

**Parse pattern** (in Chat.jsx):
```js
let parsed = null;
try { parsed = JSON.parse(raw); } catch {}
const eventType = parsed?.type || raw;
```
On `GUESS` or `GUESSED`: call `fetchRevealState()`. On `GUESSED` or `"REVEALED"`: also `fetchMessages()` and `fetchConfession()` to update the alias.

---

## Reveal guessing game (Chat.jsx)

Three inline sub-components rendered in `.reveal-panels` between `.chat-messages` and `.chat-action-bar`:

**`GuessingPanel`** (receiver, when `canGuess`):
- Shows unlocked hints with `--ri` stagger, tries counter, roll number input
- On wrong: shake animation (`.reveal-guess-form--shake`), calls `onRefresh` to re-fetch state
- On correct: calls `onGuessed()` which triggers `fetchMessages` + `fetchRevealState`
- On locked: shows locked state panel, no input

**`LiveGuessFeed`** (sender, when `guessingEnabled`):
- Shows real-time list of guesses: roll + ✓/✗ + time
- Updates whenever socket pushes a GUESS/GUESSED event

**`RevealKitSheet`** (sender, from ⋮ menu → "Set up guessing" / "Manage guessing hints"):
- 3 hint inputs (max 200 chars each) + char counters
- Toggle switch (`role="switch"`) for "Let them guess me"
- `PUT /api/reveal/kit` on save

**Guessing rules (enforced backend)**:
- 3 tries per round; wrong guess unlocks next hint (hint 1 → 2 → 3)
- 3 wrong in a row → 24h lockout; resets automatically after
- Kit must have all 3 hints filled AND `guessingEnabled = true` for receiver to see the panel
- Guess validated ONLY against that one sender's roll number — never a global lookup

---

## Public Feed (Feed.jsx)

Framer Motion swipe deck. Cards render in reverse DOM order so top card is last (z-index via `stackIndex`).

**Cycling behavior**: swipe moves top card to **back** of the local array (not removed). A `seenThisRound` Set tracks which IDs have been swiped; once all cards are cycled, re-fetches from server. Server re-prioritizes unseen-first on each fetch.

**Ordering** (server): `LEFT JOIN public_confession_views` → unseen posts (no view record) sort first (`CASE WHEN pv.viewer_id IS NULL THEN 0 ELSE 1 END ASC`), then by `id DESC` within each group.

**Expiry**: server excludes posts where `created_at < NOW() - 24h`. Hourly `@Scheduled` cleanup purges expired rows (views and reports deleted first to avoid FK violations, then the post).

**View recording**: fired fire-and-forget on swipe (`POST /feed/:id/view`). Uses `INSERT ... ON CONFLICT DO NOTHING` — only increments `viewCount` when the row is newly inserted (atomic, idempotent).

**EndState**: only shown when server returns 0 items (no posts exist). Not shown just from swiping.

---

## CSS class conventions

**Auth screens** — prefix: `rg-` (register), `lg-` (login), `vf-` (verify), `ac-` (auth choice), `splash-`
**Dashboard** — prefix: `dash-`, `conf-card`, `conf-avatar`, `skel-`, `fab`
**Chat** — prefix: `chat-`
**Feed** — prefix: `feed-`
**Reveal panels** — prefix: `reveal-`
**Tab bar** — prefix: `tab-bar`
**Shared** — `btn-full`, `btn-accent`, `btn-surface`, `btn-ghost`, `btn-danger`, `btn-theme`, `badge`, `sheet-overlay`, `sheet`, `field`, `field-input`, `.data`

Key dashboard classes: `.conf-card`, `.conf-card--unread`, `.conf-card--invited`, `.conf-card-preview`, `.dash-section-label`, `.skel-row`, `.skel-pulse`, `.fab`, `.fab--breathe`

Key chat classes: `.chat-header-avatar`, `.chat-icebreaker-wrap`, `.chat-bubble`, `.chat-bubble--mid`, `.chat-system-pill--reveal`, `.chat-system-pill--mutual`, `.chat-scroll-btn`, `.chat-sheet-item`

Key reveal classes: `.reveal-panels`, `.reveal-panel`, `.reveal-panel--locked`, `.reveal-panel--sender`, `.reveal-hints-list`, `.reveal-hint`, `.reveal-guess-form`, `.reveal-guess-form--shake`, `.reveal-feed`, `.reveal-guess-row--correct`, `.reveal-guess-row--wrong`, `.reveal-toggle`, `.reveal-toggle--on`, `.reveal-kit-sheet`

Key feed classes: `.feed-page`, `.feed-deck-area`, `.feed-deck`, `.feed-card`, `.feed-card--top`, `.feed-card-content`, `.feed-card-footer`, `.feed-hint`, `.feed-end`, `.feed-compose-textarea`, `.feed-compose-count`

`.data` utility — tabular lining figures for roll numbers, IDs, counts.

---

## Backend schema (DB migrations required before deploy)

Two manual SQL migrations live in `../campuscrush/src/main/resources/`:

**`feed_migration.sql`** — 4 tables:
- `public_confessions` (id, author_id→users, content, campus_tag, status, view_count, report_count, created_at)
- `public_confession_views` (confession_id, viewer_id — PK composite, ON CONFLICT DO NOTHING)
- `public_confession_reports` (id, confession_id, reporter_id, reason, created_at)
- `user_blocks` (blocker_id, blocked_id — PK composite)

**`reveal_migration.sql`** — 3 tables:
- `reveal_kits` (id, user_id→users UNIQUE, hint1/2/3 VARCHAR(200) nullable, guessing_enabled BOOLEAN NOT NULL DEFAULT FALSE, created_at, updated_at)
- `reveal_states` (id, confession_id→confessions UNIQUE, status VARCHAR(25) NOT NULL DEFAULT 'HIDDEN', guesses_remaining INT NOT NULL DEFAULT 3, hints_unlocked INT NOT NULL DEFAULT 1, locked_until TIMESTAMPTZ nullable, created_at)
- `reveal_guesses` (id, confession_id→confessions, guessed_roll VARCHAR(50), correct BOOLEAN, guessed_at)

**DDL rule**: every `NOT NULL` DB column needs `@Column(nullable = false)` or `@JoinColumn(nullable = false)` on the entity. Missing annotations cause startup failure with `DDL_AUTO=validate`.

---

## Backend architectural constraints

- `DDL_AUTO=validate` — schema must match entities exactly. Never add columns without a migration.
- `server.error.include-message=never` — no error details leak to clients.
- `jwt.secret=${JWT_SECRET}` — no hardcoded fallback.
- CORS allowed: `https://campusfrontend*.vercel.app`, `https://campuscrush*.vercel.app`, `http://localhost:*`
- WebSocket: `convertAndSendToUser` only — no public topic broadcasts.
- `author_id` from `public_confessions` NEVER returned to clients.
- Guess validation runs ONLY against the one sender's roll — never a global "who is this anon" lookup.
- No-leak contract: `/guess` response to recipient is `{ correct, guessesRemaining, hintsUnlocked, status }` only.
- Jackson (`ObjectMapper`) is NOT on the classpath in the Docker build — use string concatenation or manual JSON for WebSocket payloads in services.

---

## Conventions

- No comments unless the WHY is non-obvious.
- No hardcoded hex — always use tokens.
- Skeleton rows (`.skel-row` + `.skel-pulse`) instead of `<HeartLoader>` for list loading states.
- Confirm destructive actions with a bottom sheet (`.sheet-overlay` + `.sheet`), not inline.
- `apiError(err, fallback)` from `src/services/api.js` for user-facing error messages.
- Avatar color: hash alias string → index into `AVATAR_COLORS` array (10 warm earth tones). Letter: `alias[0].toUpperCase()`.
- Optimistic sends: push temp message with `id: temp-${Date.now()}`, server echoes via socket and `fetchMessages()` replaces it.
- Native SQL queries in Spring Data JPA: use `Pageable` for LIMIT (not `:lim` named param — Hibernate 6 won't bind it). Bulk DELETE across relations must use native SQL subqueries, not JPQL navigation paths.
- Scheduled cleanup (`@Scheduled`): requires `@EnableScheduling` on the main application class. Delete child rows before parent rows when no `ON DELETE CASCADE` on FKs.
