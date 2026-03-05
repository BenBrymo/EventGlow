type InitPayload = {
  email: string;
  amount: string;
  currency?: string;
};

const DEFAULT_PAYSTACK_BASE_URL = "https://api.paystack.co";
const DEFAULT_CURRENCY = "GHS";
const DEFAULT_WINDOW_MS = 60_000;
const DEFAULT_MAX_REQ_PER_MIN = 20;
const DEFAULT_MIN_SEND_INTERVAL_MS = 2_000;

type IpRateState = {
  windowStartMs: number;
  count: number;
  lastSendAtMs: number;
};

const ipRateStates = new Map<string, IpRateState>();

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

function getClientIp(request: Request): string {
  const forwardedFor = request.headers.get("x-forwarded-for")?.split(",")[0]?.trim();
  const realIp = request.headers.get("x-real-ip")?.trim();
  const cfIp = request.headers.get("cf-connecting-ip")?.trim();
  return forwardedFor || realIp || cfIp || "unknown";
}

function enforceIpRateLimit(clientIp: string): void {
  const maxReqPerMinute = Number(getEnv("PAYSTACK_MAX_REQ_PER_MINUTE")) || DEFAULT_MAX_REQ_PER_MIN;
  const minSendIntervalMs =
    Number(getEnv("PAYSTACK_MIN_REQ_INTERVAL_MS")) || DEFAULT_MIN_SEND_INTERVAL_MS;
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
    throw new Error(`Please wait ${secondsLeft}s before retrying.`);
  }

  state.lastSendAtMs = nowMs;
  ipRateStates.set(clientIp, state);
}

function enforceApiKeyAllowlist(request: Request): void {
  const configuredKeys = parseCsvEnv("ALLOWED_SUPABASE_API_KEYS");
  if (configuredKeys.length === 0) return;
  const incomingApiKey = request.headers.get("apikey")?.trim() ?? "";
  if (!incomingApiKey || !configuredKeys.includes(incomingApiKey)) {
    throw new Error("API key not allowed.");
  }
}

function parsePayload(data: unknown): InitPayload {
  if (!data || typeof data !== "object") throw new Error("Invalid JSON body.");
  const body = data as Record<string, unknown>;
  const email = String(body.email ?? "").trim();
  const amount = String(body.amount ?? "").trim();
  const currency = String(body.currency ?? DEFAULT_CURRENCY).trim().toUpperCase();

  if (!email || !amount) throw new Error("email and amount are required.");
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) throw new Error("Invalid email.");
  if (!/^\d+$/.test(amount) || Number(amount) <= 0) {
    throw new Error("amount must be a positive integer string in kobo/pesewas.");
  }

  return { email, amount, currency };
}

async function initializePaystack(payload: InitPayload): Promise<{
  authorizationUrl: string;
  reference: string;
}> {
  const secretKey = getRequiredEnv("PAYSTACK_SECRET_KEY");
  const baseUrl = getEnv("PAYSTACK_BASE_URL") || DEFAULT_PAYSTACK_BASE_URL;
  const response = await fetch(`${baseUrl}/transaction/initialize`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${secretKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      email: payload.email,
      amount: payload.amount,
      currency: payload.currency || DEFAULT_CURRENCY,
    }),
  });

  const text = await response.text();
  if (!response.ok) {
    throw new Error(`Paystack initialize failed (${response.status}): ${text}`);
  }

  const json = JSON.parse(text) as {
    data?: { authorization_url?: string; reference?: string };
  };
  const authorizationUrl = json.data?.authorization_url?.trim();
  const reference = json.data?.reference?.trim();
  if (!authorizationUrl || !reference) {
    throw new Error("Paystack initialize response missing authorization_url/reference.");
  }
  return { authorizationUrl, reference };
}

Deno.serve(async (request) => {
  if (request.method !== "POST") {
    return jsonResponse({ error: "Method not allowed." }, 405);
  }

  try {
    enforceApiKeyAllowlist(request);
    enforceIpRateLimit(getClientIp(request));
    const payload = parsePayload(await request.json());
    const result = await initializePaystack(payload);
    return jsonResponse({ ok: true, ...result }, 200);
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
