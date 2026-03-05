const DEFAULT_PAYSTACK_BASE_URL = "https://api.paystack.co";
const DEFAULT_WINDOW_MS = 60_000;
const DEFAULT_MAX_REQ_PER_MIN = 30;
const DEFAULT_MIN_SEND_INTERVAL_MS = 1_000;

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
  const maxReqPerMinute = Number(getEnv("PAYSTACK_VERIFY_MAX_REQ_PER_MINUTE")) || DEFAULT_MAX_REQ_PER_MIN;
  const minSendIntervalMs =
    Number(getEnv("PAYSTACK_VERIFY_MIN_REQ_INTERVAL_MS")) || DEFAULT_MIN_SEND_INTERVAL_MS;
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

function parsePayload(data: unknown): { reference: string } {
  if (!data || typeof data !== "object") throw new Error("Invalid JSON body.");
  const body = data as Record<string, unknown>;
  const reference = String(body.reference ?? "").trim();
  if (!reference) throw new Error("reference is required.");
  return { reference };
}

async function verifyPaystack(reference: string): Promise<unknown> {
  const secretKey = getRequiredEnv("PAYSTACK_SECRET_KEY");
  const baseUrl = getEnv("PAYSTACK_BASE_URL") || DEFAULT_PAYSTACK_BASE_URL;
  const response = await fetch(`${baseUrl}/transaction/verify/${encodeURIComponent(reference)}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${secretKey}`,
    },
  });

  const text = await response.text();
  if (!response.ok) {
    throw new Error(`Paystack verify failed (${response.status}): ${text}`);
  }
  return JSON.parse(text);
}

Deno.serve(async (request) => {
  if (request.method !== "POST") {
    return jsonResponse({ error: "Method not allowed." }, 405);
  }

  try {
    enforceApiKeyAllowlist(request);
    enforceIpRateLimit(getClientIp(request));
    const payload = parsePayload(await request.json());
    const result = await verifyPaystack(payload.reference);
    return jsonResponse(result, 200);
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
