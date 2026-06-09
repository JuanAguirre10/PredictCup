"use client";

import { Suspense, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { setToken } from "@/lib/auth";

/**
 * Destino del redirect de AuthResource del backend, que llega con ?token=JWT.
 * Guarda el token en cookie y manda a /partidos.
 */
function CapturaToken() {
  const router = useRouter();
  const params = useSearchParams();

  useEffect(() => {
    const token = params.get("token");
    if (token) {
      setToken(token);
      // Recarga COMPLETA (no router client-side): así la navbar y los hooks releen la
      // cookie recién guardada y la sesión aparece a la primera (sin loguear dos veces).
      window.location.replace("/partidos");
    } else {
      router.replace("/login");
    }
  }, [params, router]);

  return <p className="py-24 text-center text-muted-foreground">Iniciando sesión…</p>;
}

export default function LoginCallbackPage() {
  return (
    <Suspense
      fallback={<p className="py-24 text-center text-muted-foreground">Cargando…</p>}
    >
      <CapturaToken />
    </Suspense>
  );
}
