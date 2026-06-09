import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// En Next.js 16 "middleware" se renombro a "proxy"; la funcionalidad es la misma.
// Protege rutas privadas: sin cookie auth_token -> redirige a /login.

const RUTAS_PROTEGIDAS = ["/admin", "/salas", "/perfil", "/predicciones"];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const protegida = RUTAS_PROTEGIDAS.some(
    (r) => pathname === r || pathname.startsWith(`${r}/`),
  );
  if (!protegida) return NextResponse.next();

  const token = request.cookies.get("auth_token")?.value;
  if (!token) {
    const url = request.nextUrl.clone();
    url.pathname = "/login";
    url.searchParams.set("from", pathname);
    return NextResponse.redirect(url);
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*", "/salas/:path*", "/perfil", "/predicciones"],
};
