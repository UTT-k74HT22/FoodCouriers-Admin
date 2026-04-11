import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient, type SupabaseClient } from "@supabase/supabase-js";

type CreateUserAccountRequest = {
  email?: string;
  password?: string;
  full_name?: string;
  phone?: string | null;
  avatar_url?: string | null;
  role?: string;
  is_active?: boolean;
  email_confirm?: boolean;
};

type UserRow = {
  id: string;
  auth_id: string;
  full_name: string;
  phone: string | null;
  email: string;
  avatar_url: string | null;
  role: string;
  is_active: boolean;
  created_at?: string;
  updated_at?: string;
};

const JSON_HEADERS = {
  "Content-Type": "application/json",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders(req) });
  }

  if (req.method !== "POST") {
    return jsonResponse(
      { error: "Method not allowed", message: "Use POST /admin-user-accounts" },
      405,
      req,
    );
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  if (!supabaseUrl || !anonKey || !serviceRoleKey) {
    return jsonResponse(
      {
        error: "Server configuration error",
        message:
          "SUPABASE_URL, SUPABASE_ANON_KEY, and SUPABASE_SERVICE_ROLE_KEY must be configured",
      },
      500,
      req,
    );
  }

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) {
    return jsonResponse(
      { error: "Unauthorized", message: "Missing Authorization header" },
      401,
      req,
    );
  }

  let payload: CreateUserAccountRequest;
  try {
    payload = await req.json();
  } catch {
    return jsonResponse(
      { error: "Invalid request", message: "Request body must be valid JSON" },
      400,
      req,
    );
  }

  const validationError = validatePayload(payload);
  if (validationError) {
    return jsonResponse(
      { error: "Validation failed", message: validationError },
      400,
      req,
    );
  }

  const normalizedRole = normalizeRole(payload.role!);
  if (!normalizedRole) {
    return jsonResponse(
      {
        error: "Validation failed",
        message: "Role must be one of customer, staff, admin, or shipper",
      },
      400,
      req,
    );
  }

  const userClient = createClient(supabaseUrl, anonKey, {
    auth: { autoRefreshToken: false, persistSession: false },
    global: { headers: { Authorization: authHeader } },
  });
  const serviceClient = createClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });

  const caller = await resolveCaller(userClient);
  if ("errorResponse" in caller) {
    return addCorsHeaders(caller.errorResponse, req);
  }

  const adminCheck = await ensureAdminCaller(serviceClient, caller.user.id);
  if (adminCheck) {
    return jsonResponse(adminCheck.body, adminCheck.status, req);
  }

  const duplicateProfile = await findExistingProfileByEmail(
    serviceClient,
    payload.email!,
  );
  if ("errorResponse" in duplicateProfile) {
    return addCorsHeaders(duplicateProfile.errorResponse, req);
  }
  if (duplicateProfile.profile) {
    return jsonResponse(
      {
        error: "Conflict",
        message: `Email ${payload.email} already exists in public.users`,
      },
      409,
      req,
    );
  }

  const createAuthResult = await serviceClient.auth.admin.createUser({
    email: payload.email!,
    password: payload.password!,
    email_confirm: payload.email_confirm ?? true,
    user_metadata: {
      full_name: payload.full_name!,
      phone: payload.phone?.trim() || null,
      role: normalizedRole,
    },
  });

  if (createAuthResult.error || !createAuthResult.data.user?.id) {
    return jsonResponse(
      {
        error: "Auth user creation failed",
        message: createAuthResult.error?.message ?? "Unable to create auth user",
      },
      mapAuthFailureStatus(createAuthResult.error?.message),
      req,
    );
  }

  const authUserId = createAuthResult.data.user.id;

  const insertProfileResult = await serviceClient
    .from("users")
    .insert({
      auth_id: authUserId,
      full_name: payload.full_name!.trim(),
      phone: normalizeOptional(payload.phone),
      email: payload.email!.trim().toLowerCase(),
      avatar_url: normalizeOptional(payload.avatar_url),
      role: normalizedRole,
      is_active: payload.is_active ?? true,
    })
    .select("*")
    .single<UserRow>();

  if (insertProfileResult.error || !insertProfileResult.data) {
    await serviceClient.auth.admin.deleteUser(authUserId);
    return jsonResponse(
      {
        error: "Profile creation failed",
        message: insertProfileResult.error?.message ??
          "Unable to create public.users row",
      },
      500,
      req,
    );
  }

  return jsonResponse(
    {
      message: "User account created successfully",
      data: insertProfileResult.data,
    },
    201,
    req,
  );
});

function validatePayload(payload: CreateUserAccountRequest): string | null {
  if (!payload.email?.trim()) {
    return "Email is required";
  }
  if (!isEmail(payload.email)) {
    return "Email format is invalid";
  }
  if (!payload.password) {
    return "Password is required";
  }
  if (payload.password.length < 6) {
    return "Password must contain at least 6 characters";
  }
  if (!payload.full_name?.trim()) {
    return "Full name is required";
  }
  if (!payload.role?.trim()) {
    return "Role is required";
  }
  return null;
}

function normalizeRole(role: string): "customer" | "staff" | "admin" | "shipper" | null {
  const normalized = role.trim().toLowerCase();
  if (normalized === "client" || normalized === "customer") {
    return "customer";
  }
  if (normalized === "staff") {
    return "staff";
  }
  if (normalized === "admin") {
    return "admin";
  }
  if (normalized === "shipper") {
    return "shipper";
  }
  return null;
}

function normalizeOptional(value?: string | null): string | null {
  if (!value) {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length == 0 ? null : trimmed;
}

function isEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

async function resolveCaller(userClient: SupabaseClient) {
  const userResult = await userClient.auth.getUser();
  if (userResult.error || !userResult.data.user) {
    return {
      errorResponse: new Response(
        JSON.stringify({
          error: "Unauthorized",
          message: userResult.error?.message ?? "Invalid or expired session",
        }),
        { status: 401, headers: JSON_HEADERS },
      ),
    };
  }
  return { user: userResult.data.user };
}

async function ensureAdminCaller(
  serviceClient: SupabaseClient,
  authUserId: string,
) {
  const profileResult = await serviceClient
    .from("users")
    .select("id, role, is_active")
    .eq("auth_id", authUserId)
    .maybeSingle<{ id: string; role: string; is_active: boolean }>();

  if (profileResult.error) {
    return {
      status: 500,
      body: {
        error: "Caller lookup failed",
        message: profileResult.error.message,
      },
    };
  }

  if (!profileResult.data) {
    return {
      status: 403,
      body: {
        error: "Forbidden",
        message: "Admin profile was not found",
      },
    };
  }

  if (!profileResult.data.is_active) {
    return {
      status: 403,
      body: {
        error: "Forbidden",
        message: "Inactive admin accounts cannot create users",
      },
    };
  }

  if (profileResult.data.role !== "admin") {
    return {
      status: 403,
      body: {
        error: "Forbidden",
        message: "Only admin accounts can create users",
      },
    };
  }

  return null;
}

async function findExistingProfileByEmail(
  serviceClient: SupabaseClient,
  email: string,
) {
  const result = await serviceClient
    .from("users")
    .select("id, email")
    .eq("email", email.trim().toLowerCase())
    .maybeSingle<{ id: string; email: string }>();

  if (result.error) {
    return {
      errorResponse: new Response(
        JSON.stringify({
          error: "Profile lookup failed",
          message: result.error.message,
        }),
        { status: 500, headers: JSON_HEADERS },
      ),
    };
  }

  return { profile: result.data };
}

function mapAuthFailureStatus(message?: string): number {
  if (!message) {
    return 500;
  }
  const normalized = message.toLowerCase();
  if (normalized.includes("already been registered") ||
    normalized.includes("already registered") ||
    normalized.includes("duplicate")) {
    return 409;
  }
  if (normalized.includes("password")) {
    return 400;
  }
  return 500;
}

function jsonResponse(body: unknown, status: number, req: Request): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...JSON_HEADERS,
      ...corsHeaders(req),
    },
  });
}

function addCorsHeaders(response: Response, req: Request): Response {
  const headers = new Headers(response.headers);
  const cors = corsHeaders(req);
  for (const [key, value] of Object.entries(cors)) {
    headers.set(key, value);
  }
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers,
  });
}

function corsHeaders(req: Request): Record<string, string> {
  return {
    "Access-Control-Allow-Origin": req.headers.get("Origin") ?? "*",
    "Access-Control-Allow-Headers":
      "authorization, x-client-info, apikey, content-type",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
  };
}
