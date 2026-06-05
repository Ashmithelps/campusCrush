# Unsaid — Frontend Context for Claude Code

App: intimate, pseudonymous connection app for CU Chandigarh students.
North star: *a held breath.*
Stack: React 19 + Vite, React Router 7, @stomp/stompjs, deployed on Vercel.
Backend repo: `../campuscrush` (Spring Boot). Frontend repo: this directory.

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

---

## Routes & components

| Route | Component | Notes |
|---|---|---|
| `/` | `Splash` | Landing, entrance animation |
| `/auth` | `AuthChoice` | Login vs Register choice |
| `/login` | `Login` | Email only, navigates to `/verify` |
| `/register` | `Register` | Email only, navigates to `/verify` |
| `/verify` | `Verify` | Shared OTP screen; gets `{email, flow}` from `location.state` |
| `/dashboard` | `Dashboard` | Inbox — all confession threads |
| `/chat/:confessionId` | `Chat` | Thread view |

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
  createdAt, isSender, hasUnread, isRevealed, showMutualAnimation }
```

**ConfessionState** enum: `INVITED → CREATED → UNLOCKED → REVEALED / BLOCKED`
- `INVITED`: sent to unregistered roll number
- `CREATED`: receiver registered, not yet accepted
- `UNLOCKED`: receiver accepted, free chat
- `REVEALED`: mutual crush (both confessed to each other)
- `BLOCKED`: either party blocked

**MessageResponse** (from `/api/messages/:id`):
```
{ id, from: "SELF"|"OTHER", content, type: TEXT|REVEAL|MUTUAL, sentAt, senderPublicId }
```
Server always prepends icebreaker as `id: -1` (first item in message list).

**Alias logic**:
- Sender sees receiver's roll number (e.g. "23BAI70503")
- Receiver sees sender's display alias (e.g. "Golden Viper")

---

## API endpoints (frontend-relevant)

```
GET    /api/confessions                     list all threads
GET    /api/confessions/:id                 single thread
POST   /api/confessions/:receiverId         send confession (body = message string)
POST   /api/confessions/:id/reply           receiver accepts (CREATED → UNLOCKED)
POST   /api/confessions/:id/block           either party blocks
POST   /api/confessions/:id/unblock         blocker only (→ UNLOCKED)
POST   /api/confessions/:id/reveal          sender only — reveals roll number (one-sided)
POST   /api/confessions/:id/read            mark thread read
POST   /api/confessions/:id/mutual-seen     clear mutual animation flag
POST   /api/confessions/:id/invite          send invite email to unregistered roll number
GET    /api/messages/:confessionId          message list
```

**Reveal is one-sided**: only the sender can reveal; reveals their roll number as a REVEAL-type system message. `REVEALED` state is separate (mutual crush). No report or mute endpoints exist.

---

## Real-time (WebSocket)

Service: `src/services/socket.js` — singleton STOMP client.
Connect: `socket.connect(token)`. Send: `socket.sendMessage(confessionId, content)`.
Subscribe pattern: `socket.subscribe(topic, callback) → Promise<subscription>`.

Topics:
- `/user/queue/confessions` — dashboard refresh (new confessions, state changes)
- `/user/queue/confession/:id` — thread events: `"MUTUAL"`, `"REVEALED"`, `"NEW_MESSAGE"`, `"UPDATE"`

---

## CSS class conventions

**Auth screens** — prefix: `rg-` (register), `lg-` (login), `vf-` (verify), `ac-` (auth choice), `splash-`
**Dashboard** — prefix: `dash-`, `conf-card`, `conf-avatar`, `skel-`, `fab`
**Chat** — prefix: `chat-`
**Shared** — `btn-full`, `btn-accent`, `btn-surface`, `btn-ghost`, `btn-danger`, `btn-theme`, `badge`, `sheet-overlay`, `sheet`, `field`, `field-input`

Key dashboard classes: `.conf-card`, `.conf-card--unread`, `.conf-card--invited`, `.conf-card-preview`, `.dash-section-label`, `.skel-row`, `.skel-pulse`, `.fab`, `.fab--breathe`

Key chat classes: `.chat-header-avatar`, `.chat-icebreaker-wrap`, `.chat-bubble`, `.chat-bubble--mid`, `.chat-system-pill--reveal`, `.chat-system-pill--mutual`, `.chat-scroll-btn`, `.chat-sheet-item`

---

## Conventions

- No comments unless the WHY is non-obvious.
- No hardcoded hex — always use tokens.
- Skeleton rows (`.skel-row` + `.skel-pulse`) instead of `<HeartLoader>` for list loading states.
- Confirm destructive actions with a bottom sheet (`.sheet-overlay` + `.sheet`), not inline.
- `apiError(err, fallback)` from `src/services/api.js` for user-facing error messages.
- Avatar color: hash alias string → index into `AVATAR_COLORS` array (10 warm earth tones). Letter: `alias[0].toUpperCase()`.
- Optimistic sends: push temp message with `id: temp-${Date.now()}`, server echoes via socket and `fetchMessages()` replaces it.
