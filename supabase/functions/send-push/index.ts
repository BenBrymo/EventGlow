import { importPKCS8, SignJWT } from "npm:jose@5.9.6";

type PushPayload = {
  title: string;
  body: string;
  targetRole: "all" | "user" | "admin";
  route:
    | "detailed_event_screen"
    | "detailed_event_screen_admin"
    | "admin_main_screen"
    | "user_main_screen";
  eventId?: string;
};

type ServiceAccount = {
  project_id: string;
  client_email: string;
  private_key: string;
};

type IpRateState = {
  windowStartMs: number;
  count: number;
  lastSendAtMs: number;
};

const GOOGLE_OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token";
const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const DEFAULT_WINDOW_MS = 60_000;
const DEFAULT_MAX_REQ_PER_MIN = 20;
const DEFAULT_MIN_SEND_INTERVAL_MS = 5_000;

const ipRateStates = new Map<string, IpRateState>();

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

function getAllowedRoles(): Set<string> {
  const fromEnv = parseCsvEnv("ALLOWED_TARGET_ROLES").map((it) => it.toLowerCase());
  const defaults = ["all", "user", "admin"];
  return new Set(fromEnv.length > 0 ? fromEnv : defaults);
}

function getAllowedRoutes(): Set<string> {
  const fromEnv = parseCsvEnv("ALLOWED_ROUTES");
  const defaults = [
    "detailed_event_screen",
    "detailed_event_screen_admin",
    "admin_main_screen",
    "user_main_screen",
  ];
  return new Set(fromEnv.length > 0 ? fromEnv : defaults);
}

function enforceApiKeyAllowlist(request: Request): void {
  const configuredKeys = parseCsvEnv("ALLOWED_SUPABASE_API_KEYS");
  if (configuredKeys.length === 0) return;

  const incomingApiKey = request.headers.get("apikey")?.trim() ?? "";
  if (!incomingApiKey || !configuredKeys.includes(incomingApiKey)) {
    throw new Error("API key not allowed.");
  }
}

function getClientIp(request: Request): string {
  const forwardedFor = request.headers.get("x-forwarded-for")?.split(",")[0]?.trim();
  const realIp = request.headers.get("x-real-ip")?.trim();
  const cfIp = request.headers.get("cf-connecting-ip")?.trim();
  return forwardedFor || realIp || cfIp || "unknown";
}

function enforceIpRateLimit(clientIp: string): void {
  const maxReqPerMinute = Number(getEnv("PUSH_MAX_REQ_PER_MINUTE")) || DEFAULT_MAX_REQ_PER_MIN;
  const minSendIntervalMs =
    Number(getEnv("PUSH_MIN_SEND_INTERVAL_MS")) || DEFAULT_MIN_SEND_INTERVAL_MS;
  const nowMs = Date.now();

  const state = ipRateStates.get(clientIp) ?? {
    windowStartMs: nowMs,
    count: 0,
    lastSendAtMs: 0,
  };

  if (nowMs - state.windowStartMs >= DEFAULT_WINDOW_MS) {
    state.windowStartMs = nowMs;
    state.count = 0;
  }

  state.count += 1;
  if (state.count > maxReqPerMinute) {
    ipRateStates.set(clientIp, state);
    throw new Error("Rate limit exceeded for this IP.");
  }

  if (nowMs - state.lastSendAtMs < minSendIntervalMs) {
    const secondsLeft = Math.max(1, Math.ceil((minSendIntervalMs - (nowMs - state.lastSendAtMs)) / 1000));
    ipRateStates.set(clientIp, state);
    throw new Error(`Please wait ${secondsLeft}s before sending another push.`);
  }

  state.lastSendAtMs = nowMs;
  ipRateStates.set(clientIp, state);
}

function parsePayload(
  data: unknown,
  allowedRoles: Set<string>,
  allowedRoutes: Set<string>,
): PushPayload {
  if (!data || typeof data !== "object") throw new Error("Invalid JSON body.");

  const body = data as Record<string, unknown>;
  const title = String(body.title ?? "").trim();
  const messageBody = String(body.body ?? "").trim();
  const targetRole = String(body.targetRole ?? "").trim().toLowerCase();
  const route = String(body.route ?? "").trim();
  const eventId = String(body.eventId ?? "").trim();

  if (!title || !messageBody || !targetRole || !route) {
    throw new Error("title, body, targetRole and route are required.");
  }
  if (title.length > 120) throw new Error("title max length is 120.");
  if (messageBody.length > 400) throw new Error("body max length is 400.");
  if (!allowedRoles.has(targetRole)) throw new Error("targetRole is not allowlisted.");
  if (!allowedRoutes.has(route)) throw new Error("route is not allowlisted.");
  if (eventId.length > 120) throw new Error("eventId max length is 120.");

  return {
    title,
    body: messageBody,
    targetRole: targetRole as PushPayload["targetRole"],
    route: route as PushPayload["route"],
    eventId,
  };
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

function roleToTopic(role: PushPayload["targetRole"]): string {
  if (role === "admin") return "eventglow_admin";
  if (role === "user") return "eventglow_user";
  return "eventglow_general";
}

async function sendFcmTopicMessage(
  projectId: string,
  fcmAccessToken: string,
  payload: PushPayload,
): Promise<void> {
  const topic = roleToTopic(payload.targetRole);
  const url = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;
  const message = {
    message: {
      topic,
      notification: {
        title: payload.title,
        body: payload.body,
      },
      data: {
        route: payload.route,
        eventId: payload.eventId ?? "",
      },
      android: { priority: "high" },
    },
  };

  const res = await fetch(url, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${fcmAccessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(message),
  });
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(`FCM send failed (${res.status}): ${txt}`);
  }
}

Deno.serve(async (request) => {
  if (request.method !== "POST") {
    return jsonResponse({ error: "Method not allowed." }, 405);
  }

  try {
    enforceApiKeyAllowlist(request);
    const clientIp = getClientIp(request);
    enforceIpRateLimit(clientIp);

    const allowedRoles = getAllowedRoles();
    const allowedRoutes = getAllowedRoutes();
    const payload = parsePayload(await request.json(), allowedRoles, allowedRoutes);

    const serviceAccount = getServiceAccount();
    const explicitProjectId = getEnv("FIREBASE_PROJECT_ID");
    const projectId = explicitProjectId || serviceAccount.project_id;
    if (!projectId) throw new Error("Missing Firebase project id.");

    const fcmToken = await getGoogleAccessToken(serviceAccount, [FCM_SCOPE]);
    await sendFcmTopicMessage(projectId, fcmToken, payload);

    return jsonResponse({ ok: true, topic: roleToTopic(payload.targetRole), clientIp }, 200);
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown server error.";
    const lower = message.toLowerCase();
    const status = lower.includes("rate limit") || lower.includes("wait ")
      ? 429
      : lower.includes("api key")
      ? 401
      : 400;
    return jsonResponse({ error: message }, status);
  }
});
