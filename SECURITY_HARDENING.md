# Security Hardening Checklist

This project now includes:

- Firebase App Check bootstrap in `MainActivity`
- Auth attempt throttling in `BaseViewModel` (used by login + account creation)
- Firestore create-event cooldown in `EventsManagementViewModel`
- Hardened Firestore rules template in `firestore.rules`

## 1) Enable App Check

- Firebase Console -> App Check -> Add Android app.
- Provider:
- Development: use **Debug provider**.
- Production: use **Play Integrity**.
- After your debug build runs, copy debug token from Logcat and register it in App Check.
- Enforce App Check for:
- Firestore
- Authentication
- Realtime Database (if used)

## 2) Deploy Firestore Rules

- Install Firebase CLI and login:

```bash
firebase login
```

- Initialize if needed:

```bash
firebase init firestore
```

- Deploy:

```bash
firebase deploy --only firestore:rules
```

## 3) Notes on Rate Limiting

- Current implementation adds:
- Local credential throttling for login/create-account
- Firestore-backed cooldown for event creation (`rate_limits/{uid}`)
- This is strong for app-level abuse control but not equivalent to a fully trusted server limiter.

## 4) Stronger Backend Rate Limiting (recommended next)

- Use a trusted backend endpoint (Cloud Run / your own API) for sensitive operations.
- Verify Firebase ID token server-side.
- Apply IP + user + route limits (sliding window/token bucket).
- Write to Firestore only from trusted backend for high-risk operations.

