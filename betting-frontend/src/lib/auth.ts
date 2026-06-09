import Cookies from "js-cookie";

const COOKIE = "auth_token";

export interface UsuarioToken {
  sub: string;
  correo: string;
  rol: string;
  nombre: string;
}

export function getToken(): string | null {
  return Cookies.get(COOKIE) ?? null;
}

export function setToken(t: string): void {
  // ~12h, alineado con la expiracion del JWT que emite el backend.
  Cookies.set(COOKIE, t, { expires: 0.5, sameSite: "lax", path: "/" });
}

export function clearToken(): void {
  Cookies.remove(COOKIE, { path: "/" });
}

/** Decodifica el payload de un JWT sin verificar la firma (solo para leer claims en el cliente). */
function decodificar(token: string): Record<string, unknown> | null {
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
        .join(""),
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

export function getUsuario(): UsuarioToken | null {
  const token = getToken();
  if (!token) return null;
  const claims = decodificar(token);
  if (!claims) return null;

  // Expirado -> sesion invalida.
  if (typeof claims.exp === "number" && claims.exp * 1000 < Date.now()) {
    clearToken();
    return null;
  }

  const groups = claims.groups as string[] | string | undefined;
  const rol = Array.isArray(groups) ? groups[0] ?? "usuario" : groups ?? "usuario";

  return {
    sub: String(claims.sub ?? ""),
    correo: String(claims.upn ?? claims.email ?? ""),
    rol: String(rol),
    nombre: String(claims.nombre ?? claims.upn ?? "Usuario"),
  };
}

export function isAdmin(): boolean {
  return getUsuario()?.rol === "admin";
}

export function isLoggedIn(): boolean {
  return getUsuario() !== null;
}
