import { importPKCS8, SignJWT } from "npm:jose@5.9.6";

type ServiceAccount = {
  project_id: string;
  client_email: string;
  private_key: string;
};

type CreateUserPayload = {
  firebaseIdToken: string;
  username: string;
  email: string;
  password: string;
  role: string;
};

const GOOGLE_OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token";
const FIREBASE_AUTH_BASE_URL = "https://identitytoolkit.googleapis.com/v1";
const FIRESTORE_BASE_URL = "https://firestore.googleapis.com/v1";
const FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
const DEFAULT_ALLOWED_ROLES = ["user", "admin"];

let cachedGoogleAccessToken = "";
let cachedGoogleAccessTokenExpMs = 0;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function getEnv(name: string): string {
  return Deno.env.get(name)?.trim() ?? "";
}

function getRequiredEnv(name: string): string {
  const value = getEnv(name);
  if (!value) throw new Error(`Missing required env var: ${name}`);
  return value;
}

function parseCsvEnv(name: string): string[] {
  return getEnv(name)
    .split(",")
    .map((it) => it.trim())
    .filter((it) => it.length > 0);
}

function getAllowedRoles(): Set<string> {
  const roles = parseCsvEnv("ALLOWED_CREATE_USER_ROLES").map((it) => it.toLowerCase());
  return new Set(roles.length > 0 ? roles : DEFAULT_ALLOWED_ROLES);
}

function getServiceAccount(): ServiceAccount {
  const rawJson = getRequiredEnv("GOOGLE_SERVICE_ACCOUNT_JSON");
  const parsed = JSON.parse(rawJson) as Partial<ServiceAccount>;
  if (!parsed.project_id || !parsed.client_email || !parsed.private_key) {
    throw new Error("GOOGLE_SERVICE_ACCOUNT_JSON is invalid.");
  }
  return {
    project_id: parsed.project_id,
    client_email: parsed.client_email,
    private_key: parsed.private_key,
  };
}

function enforceApiKeyAllowlist(request: Request): void {
  const configuredKeys = parseCsvEnv("ALLOWED_SUPABASE_API_KEYS");
  if (configuredKeys.length === 0) return;
  const incomingApiKey = request.headers.get("apikey")?.trim() ?? "";
  if (!incomingApiKey || !configuredKeys.includes(incomingApiKey)) {
    throw new Error("API key not allowed.");
  }
}

function parsePayload(data: unknown): CreateUserPayload {
  if (!data || typeof data !== "object") throw new Error("Invalid JSON body.");
  const body = data as Record<string, unknown>;
  const firebaseIdToken = String(body.firebaseIdToken ?? "").trim();
  const username = String(body.username ?? "").trim();
  const email = String(body.email ?? "").trim();
  const password = String(body.password ?? "");
  const role = String(body.role ?? "").trim().toLowerCase();

  if (!firebaseIdToken || !username || !email || !password || !role) {
    throw new Error("firebaseIdToken, username, email, password and role are required.");
  }
  if (username.length < 3 || username.length > 40) {
    throw new Error("username length must be between 3 and 40.");
  }
  if (password.length < 6) throw new Error("password must be at least 6 characters.");
  if (email.length > 120) throw new Error("email is too long.");
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) throw new Error("Invalid email address.");

  return { firebaseIdToken, username, email, password, role };
}

async function getGoogleAccessToken(
  serviceAccount: ServiceAccount,
  scopes: string[],
): Promise<string> {
  const nowMs = Date.now();
  if (cachedGoogleAccessToken && nowMs < cachedGoogleAccessTokenExpMs - 30_000) {
    return cachedGoogleAccessToken;
  }

  const nowSec = Math.floor(nowMs / 1000);
  const privateKey = await importPKCS8(serviceAccount.private_key, "RS256");
  const assertion = await new SignJWT({
    scope: scopes.join(" "),
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(serviceAccount.client_email)
    .setAudience(GOOGLE_OAUTH_TOKEN_URL)
    .setIssuedAt(nowSec)
    .setExpirationTime(nowSec + 3600)
    .sign(privateKey);

  const formBody = new URLSearchParams({
    grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
    assertion,
  });
  const tokenRes = await fetch(GOOGLE_OAUTH_TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: formBody.toString(),
  });
  if (!tokenRes.ok) {
    const txt = await tokenRes.text();
    throw new Error(`Google OAuth token request failed: ${txt}`);
  }

  const tokenJson = await tokenRes.json() as { access_token?: string; expires_in?: number };
  const accessToken = tokenJson.access_token?.trim();
  if (!accessToken) throw new Error("Google OAuth response missing access_token.");

  cachedGoogleAccessToken = accessToken;
  cachedGoogleAccessTokenExpMs = nowMs + ((tokenJson.expires_in ?? 3600) * 1000);
  return accessToken;
}

async function verifyFirebaseIdToken(
  webApiKey: string,
  idToken: string,
): Promise<{ uid: string }> {
  const res = await fetch(`${FIREBASE_AUTH_BASE_URL}/accounts:lookup?key=${encodeURIComponent(webApiKey)}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ idToken }),
  });
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(`Firebase token verification failed: ${txt}`);
  }

  const json = await res.json() as { users?: Array<{ localId?: string }> };
  const uid = json.users?.[0]?.localId?.trim();
  if (!uid) throw new Error("Invalid Firebase user token.");
  return { uid };
}

async function getUserRoleFromFirestore(
  accessToken: string,
  projectId: string,
  uid: string,
): Promise<string> {
  const url = `${FIRESTORE_BASE_URL}/projects/${projectId}/databases/(default)/documents/users/${uid}`;
  const res = await fetch(url, {
    method: "GET",
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(`Failed to load caller role: ${txt}`);
  }
  const json = await res.json() as {
    fields?: { role?: { stringValue?: string } };
  };
  return json.fields?.role?.stringValue?.trim().toLowerCase() ?? "";
}

async function usernameExists(
  accessToken: string,
  projectId: string,
  username: string,
): Promise<boolean> {
  const parent = `${FIRESTORE_BASE_URL}/projects/${projectId}/databases/(default)/documents:runQuery`;
  const runQueryBody = {
    structuredQuery: {
      from: [{ collectionId: "users" }],
      where: {
        fieldFilter: {
          field: { fieldPath: "username" },
          op: "EQUAL",
          value: { stringValue: username },
        },
      },
      limit: 1,
    },
  };
  const res = await fetch(parent, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(runQueryBody),
  });
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(`Username check failed: ${txt}`);
  }
  const rows = await res.json() as Array<{ document?: unknown }>;
  return rows.some((row) => Boolean(row.document));
}

async function createFirebaseUser(
  webApiKey: string,
  email: string,
  password: string,
): Promise<{ uid: string; idToken: string }> {
  const res = await fetch(`${FIREBASE_AUTH_BASE_URL}/accounts:signUp?key=${encodeURIComponent(webApiKey)}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      email,
      password,
      returnSecureToken: true,
    }),
  });
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(`Firebase user creation failed: ${txt}`);
  }
  const json = await res.json() as { localId?: string; idToken?: string };
  const uid = json.localId?.trim();
  const idToken = json.idToken?.trim();
  if (!uid || !idToken) throw new Error("Firebase user creation returned invalid response.");
  return { uid, idToken };
}

async function sendVerificationEmail(webApiKey: string, idToken: string): Promise<void> {
  const res = await fetch(`${FIREBASE_AUTH_BASE_URL}/accounts:sendOobCode?key=${encodeURIComponent(webApiKey)}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      requestType: "VERIFY_EMAIL",
      idToken,
    }),
  });
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(`Failed to send verification email: ${txt}`);
  }
}

async function createUserDocument(
  accessToken: string,
  projectId: string,
  uid: string,
  payload: CreateUserPayload,
): Promise<void> {
  const nowIso = new Date().toISOString();
  const url = `${FIRESTORE_BASE_URL}/projects/${projectId}/databases/(default)/documents/users?documentId=${encodeURIComponent(uid)}`;
  const body = {
    fields: {
      username: { stringValue: payload.username },
      email: { stringValue: payload.email },
      role: { stringValue: payload.role },
      notificationsEnabled: { booleanValue: true },
      fcmToken: { nullValue: null },
      profilePictureUrl: { nullValue: null },
      headerPictureUrl: { nullValue: null },
      boughtTickets: { arrayValue: { values: [] } },
      bookmarks: { arrayValue: { values: [] } },
      favouriteEvents: { arrayValue: { values: [] } },
      createdAt: { timestampValue: nowIso },
    },
  };

  const res = await fetch(url, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(`Failed to write user profile: ${txt}`);
  }
}

Deno.serve(async (request) => {
  if (request.method !== "POST") {
    return jsonResponse({ error: "Method not allowed." }, 405);
  }

  let step = "start";
  try {
    step = "api_key_allowlist";
    enforceApiKeyAllowlist(request);
    step = "parse_payload";
    const payload = parsePayload(await request.json());
    step = "validate_role_allowlist";
    const allowedRoles = getAllowedRoles();
    if (!allowedRoles.has(payload.role)) {
      throw new Error("role is not allowlisted.");
    }

    step = "load_env";
    const serviceAccount = getServiceAccount();
    const webApiKey = getRequiredEnv("FIREBASE_WEB_API_KEY");
    const explicitProjectId = getEnv("FIREBASE_PROJECT_ID");
    const projectId = explicitProjectId || serviceAccount.project_id;
    if (!projectId) throw new Error("Missing Firebase project id.");

    step = "google_oauth_firestore_scope";
    const firestoreToken = await getGoogleAccessToken(serviceAccount, [FIRESTORE_SCOPE]);
    step = "firebase_token_verify";
    const caller = await verifyFirebaseIdToken(webApiKey, payload.firebaseIdToken);
    step = "firestore_role_lookup";
    const callerRole = await getUserRoleFromFirestore(firestoreToken, projectId, caller.uid);
    if (callerRole !== "admin") throw new Error("Only admins can create users.");

    step = "username_check_exact";
    const exactExists = await usernameExists(firestoreToken, projectId, payload.username);
    step = "username_check_lower";
    const lowerExists = payload.username.toLowerCase() !== payload.username
      ? await usernameExists(firestoreToken, projectId, payload.username.toLowerCase())
      : false;
    if (exactExists || lowerExists) {
      throw new Error("Username is already taken.");
    }

    step = "firebase_signup";
    const created = await createFirebaseUser(webApiKey, payload.email, payload.password);
    step = "firebase_send_verification";
    await sendVerificationEmail(webApiKey, created.idToken);
    step = "firestore_write_user";
    await createUserDocument(firestoreToken, projectId, created.uid, payload);

    step = "success";
    return jsonResponse({
      ok: true,
      step,
      userId: created.uid,
      verificationEmailSent: true,
    }, 200);
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown server error.";
    const lower = message.toLowerCase();
    const status = lower.includes("only admins")
      ? 403
      : lower.includes("api key")
      ? 401
      : 400;
    console.error(`[create-user-admin] step=${step} error=${message}`);
    return jsonResponse({ error: message, step }, status);
  }
});
