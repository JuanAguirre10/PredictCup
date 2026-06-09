"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Bell } from "lucide-react";
import useSWR from "swr";
import { apiFetch } from "@/lib/api";
import type { Notificacion } from "@/lib/types";

export default function CampanillaNotificaciones() {
  const router = useRouter();
  const [abierto, setAbierto] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const { data: notifs, mutate } = useSWR<Notificacion[]>(
    "/api/notificaciones",
    (e: string) => apiFetch<Notificacion[]>(e),
    { refreshInterval: 60_000 },
  );

  // Cerrar al hacer clic fuera.
  useEffect(() => {
    const onClick = (ev: MouseEvent) => {
      if (ref.current && !ref.current.contains(ev.target as Node)) setAbierto(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);

  const noLeidas = notifs?.length ?? 0;

  const abrir = async (n: Notificacion) => {
    setAbierto(false);
    try {
      await apiFetch(`/api/notificaciones/${n.id}/leer`, { method: "PATCH" });
      void mutate();
    } catch {
      /* si falla el marcado, igual navegamos */
    }
    if (n.idPartido) router.push(`/partidos/${n.idPartido}`);
    else if (n.idSala) router.push(`/salas/${n.idSala}`);
    else router.push("/notificaciones");
  };

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setAbierto((v) => !v)}
        aria-label="Notificaciones"
        className="relative rounded-full p-2 text-muted-foreground transition hover:bg-accent hover:text-foreground"
      >
        <Bell className="h-5 w-5" />
        {noLeidas > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-bold text-primary-foreground">
            {noLeidas > 9 ? "9+" : noLeidas}
          </span>
        )}
      </button>

      {abierto && (
        <div className="absolute right-0 mt-2 w-80 overflow-hidden rounded-xl border border-border bg-card shadow-xl">
          <div className="border-b border-border px-4 py-2.5 text-sm font-semibold">
            Notificaciones
          </div>
          {notifs && notifs.length > 0 ? (
            notifs.slice(0, 5).map((n) => (
              <button
                key={n.id}
                onClick={() => abrir(n)}
                className="block w-full border-b border-border px-4 py-3 text-left transition last:border-0 hover:bg-accent"
              >
                <p className="text-sm font-medium">{n.titulo}</p>
                <p className="mt-0.5 line-clamp-2 text-xs text-muted-foreground">{n.cuerpo}</p>
              </button>
            ))
          ) : (
            <p className="px-4 py-6 text-center text-sm text-muted-foreground">Sin notificaciones nuevas.</p>
          )}
          <Link
            href="/notificaciones"
            onClick={() => setAbierto(false)}
            className="block border-t border-border px-4 py-2.5 text-center text-sm font-medium text-primary transition hover:bg-accent"
          >
            Ver todas
          </Link>
        </div>
      )}
    </div>
  );
}
