# Supabase Push Setup

This project sends push notification requests from app code to a Supabase Edge Function.
The Edge Function is responsible for calling FCM using a server-side secret.

## App Config

Set these values in `gradle.properties`:

```properties
SUPABASE_FUNCTIONS_BASE_URL=https://<project-ref>.supabase.co
SUPABASE_FUNCTIONS_ANON_KEY=<supabase-anon-key>
SUPABASE_FUNCTIONS_PUSH_PATH=functions/v1/send-push
```

These are compiled into `BuildConfig` fields and used by
`FirestoreNotificationSenderViewModel`.

## Expected Function Request Body

```json
{
  "title": "string",
  "body": "string",
  "targetRole": "all | user | admin",
  "route": "detailed_event_screen | detailed_event_screen_admin",
  "eventId": "string"
}
```

## Suggested Function Behavior

1. Validate request payload.
2. Enforce API-key allowlist from server env against `apikey` header.
3. Map `targetRole` to topic:
    - `all` -> `eventglow_general`
    - `user` -> `eventglow_user`
    - `admin` -> `eventglow_admin`
4. Send push to FCM using a server-side secret.
5. Enforce IP-based rate limits and route/role allowlists server-side.

## Required Supabase Secrets / Env

Required:

- `GOOGLE_SERVICE_ACCOUNT_JSON` (Firebase service account JSON, minified to one line)
- `FIREBASE_PROJECT_ID` (optional if present in service account JSON)

Recommended hardening:

- `ALLOWED_SUPABASE_API_KEYS` (comma-separated values accepted by this function)
- `ALLOWED_TARGET_ROLES` (comma-separated, defaults: `all,user,admin`)
- `ALLOWED_ROUTES` (comma-separated, defaults: `detailed_event_screen,detailed_event_screen_admin`)
- `PUSH_MAX_REQ_PER_MINUTE` (default: `20`)
- `PUSH_MIN_SEND_INTERVAL_MS` (default: `5000`)

## Security Notes

- App-side keys in `BuildConfig` are still client-visible.
- Keep real FCM secrets server-side only.
- Token verification is not used in this mode.
- Add strict API-key allowlist, rate limits, and route/role allowlists in function env.

## Function Files In This Repo

- `supabase/functions/send-push/index.ts`
- `supabase/config.toml`
- `supabase/functions/.env.example`

## Deploy Steps

1. Install and login to Supabase CLI.
2. Link project:

```bash
supabase link --project-ref <project-ref>
```

3. Set function secrets (do not commit real secrets):

```bash
supabase secrets set GOOGLE_SERVICE_ACCOUNT_JSON='<one-line-json>'
supabase secrets set FIREBASE_PROJECT_ID='<firebase-project-id>'
supabase secrets set ALLOWED_SUPABASE_API_KEYS='<anon-or-publishable-key>'
supabase secrets set ALLOWED_TARGET_ROLES='all,user,admin'
supabase secrets set ALLOWED_ROUTES='detailed_event_screen,detailed_event_screen_admin'
supabase secrets set PUSH_MAX_REQ_PER_MINUTE='20'
supabase secrets set PUSH_MIN_SEND_INTERVAL_MS='5000'
```

4. Deploy function:

```bash
supabase functions deploy send-push
```

5. Verify endpoint:

`https://<project-ref>.supabase.co/functions/v1/send-push`
