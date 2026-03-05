import { createHash } from "node:crypto";
import { importPKCS8, SignJWT } from "npm:jose@5.9.6";

type ServiceAccount = {
  project_id: string;
  client_email: string;
  private_key: string;
};

type DeletePayload = {
  publicId: string;
};

const FIREBASE_AUTH_BASE_URL = "https://identitytoolkit.googleapis.com/v1";
const FIRESTORE_BASE_URL = "https://firestore.googleapis.com/v1";
const GOOGLE_OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token";
const FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore";

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

function enforceApiKeyAllowlist(request: Request): void {
  const configuredKeys = parseCsvEnv("ALLOWED_SUPABASE_API_KEYS");
  if (configuredKeys.length === 0) return;
  const incomingApiKey = request.headers.get("apikey")?.trim() ?? "";
  if (!incomingApiKey || !configuredKeys.includes(incomingApiKey)) {
    throw new Error("API key not allowed.");
  }
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

let cachedGoogleAccessToken = "";
let cachedGoogleAccessTokenExpMs = 0;

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

function parsePayload(data: unknown): DeletePayload {
  if (!data || typeof data !== "object") throw new Error("Invalid JSON body.");
  const body = data as Record<string, unknown>;
  const publicId = String(body.publicId ?? "").trim();
  if (!publicId) throw new Error("publicId is required.");
  if (publicId.length > 240) throw new Error("publicId is too long.");
  return { publicId };
}

function signCloudinaryDelete(publicId: string, timestamp: number, apiSecret: string): string {
  const payload = `public_id=${publicId}&timestamp=${timestamp}${apiSecret}`;
  return createHash("sha1").update(payload).digest("hex");
}

async function destroyCloudinaryImage(publicId: string): Promise<void> {
  const cloudName = getRequiredEnv("CLOUDINARY_CLOUD_NAME");
  const apiKey = getRequiredEnv("CLOUDINARY_API_KEY");
  const apiSecret = getRequiredEnv("CLOUDINARY_API_SECRET");
  const timestamp = Math.floor(Date.now() / 1000);
  const signature = signCloudinaryDelete(publicId, timestamp, apiSecret);

  const form = new URLSearchParams({
    public_id: publicId,
    timestamp: String(timestamp),
    api_key: apiKey,
    signature,
  });

  const res = await fetch(`https://api.cloudinary.com/v1_1/${encodeURIComponent(cloudName)}/image/destroy`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: form.toString(),
  });
  const text = await res.text();
  if (!res.ok) {
    throw new Error(`Cloudinary destroy failed (${res.status}): ${text}`);
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

    step = "validate_firebase_token_header";
    const firebaseIdToken = request.headers.get("X-Firebase-Id-Token")?.trim() ?? "";
    if (!firebaseIdToken) throw new Error("Missing Firebase ID token header.");

    step = "load_env";
    const serviceAccount = getServiceAccount();
    const webApiKey = getRequiredEnv("FIREBASE_WEB_API_KEY");
    const explicitProjectId = getEnv("FIREBASE_PROJECT_ID");
    const projectId = explicitProjectId || serviceAccount.project_id;
    if (!projectId) throw new Error("Missing Firebase project id.");

    step = "google_oauth_firestore_scope";
    const firestoreToken = await getGoogleAccessToken(serviceAccount, [FIRESTORE_SCOPE]);

    step = "firebase_token_verify";
    const caller = await verifyFirebaseIdToken(webApiKey, firebaseIdToken);

    step = "firestore_role_lookup";
    const callerRole = await getUserRoleFromFirestore(firestoreToken, projectId, caller.uid);
    if (callerRole !== "admin") throw new Error("Only admins can delete event images.");

    step = "cloudinary_destroy";
    await destroyCloudinaryImage(payload.publicId);

    return jsonResponse({ ok: true, step, publicId: payload.publicId }, 200);
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown server error.";
    const lower = message.toLowerCase();
    const status = lower.includes("api key")
      ? 401
      : lower.includes("only admins")
      ? 403
      : 400;
    console.error(`[delete-cloudinary-image] step=${step} error=${message}`);
    return jsonResponse({ error: message, step }, status);
  }
});
