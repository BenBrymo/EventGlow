type InitPayload = {
  email: string;
  amount: string;
  currency?: string;
};

const DEFAULT_PAYSTACK_BASE_URL = "https://api.paystack.co";
const DEFAULT_CURRENCY = "GHS";

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
    const payload = parsePayload(await request.json());
    const result = await initializePaystack(payload);
    return jsonResponse({ ok: true, ...result }, 200);
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown server error.";
    const status = message.toLowerCase().includes("api key") ? 401 : 400;
    return jsonResponse({ error: message }, status);
  }
});
