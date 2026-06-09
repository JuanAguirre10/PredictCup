import { getToken, clearToken } from "@/lib/auth";

export const BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/** Error con el status HTTP para que la UI decida como reaccionar. */
export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

/**
 * Wrapper de fetch contra el backend Quarkus.
 * - Adjunta Authorization: Bearer {token} si hay cookie auth_token.
 * - 401 -> limpia sesion y redirige a /login.
 * - 429 -> Error("Demasiadas peticiones").
 * - !ok -> ApiError(status, mensaje del problem-details).
 */
export async function apiFetch<T>(endpoint: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const res = await fetch(`${BASE}${endpoint}`, { ...init, headers });

  if (res.status === 401) {
    clearToken();
    if (typeof window !== "undefined") window.location.href = "/login";
    throw new ApiError(401, "No autenticado");
  }
  if (res.status === 429) {
    throw new ApiError(429, "Demasiadas peticiones");
  }
  if (!res.ok) {
    let message = res.statusText;
    try {
      const body = await res.json();
      message = body.detail ?? body.title ?? message;
    } catch {
      /* la respuesta de error no era JSON; nos quedamos con statusText */
    }
    throw new ApiError(res.status, message);
  }

  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
