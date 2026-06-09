"use client";

import useSWR from "swr";
import Link from "next/link";
import { CheckCheck } from "lucide-react";
import { toast } from "sonner";
import { apiFetch } from "@/lib/api";
import type { Notificacion } from "@/lib/types";
import { fechaHoraLarga } from "@/lib/fecha";

export default function NotificacionesPage() {
  const { data: notifs, isLoading, mutate } = useSWR<Notificacion[]>(
    "/api/notificaciones",
    (e: string) => apiFetch<Notificacion[]>(e),
  );

  const marcarTodas = async () => {
    try {
      await apiFetch("/api/notificaciones/leer-todas", { method: "PATCH" });
      toast.success("Todas marcadas como leídas");
      void mutate();
    } catch {
      toast.error("No se pudo actualizar");
    }
  };

  const destino = (n: Notificacion) =>
    n.idPartido ? `/partidos/${n.idPartido}` : n.idSala ? `/salas/${n.idSala}` : "/notificaciones";

  return (
    <div className="mx-auto max-w-2xl px-4 py-10">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Notificaciones</h1>
        {notifs && notifs.length > 0 && (
          <button
            onClick={marcarTodas}
            className="inline-flex items-center gap-1.5 rounded-md border border-border px-3 py-2 text-sm transition hover:bg-accent"
          >
            <CheckCheck className="h-4 w-4" /> Marcar todas como leídas
          </button>
        )}
      </div>

      {isLoading ? (
        <div className="h-40 animate-pulse rounded-2xl border border-border bg-card" />
      ) : notifs && notifs.length > 0 ? (
        <div className="overflow-hidden rounded-2xl border border-border bg-card">
          {notifs.map((n) => (
            <Link
              key={n.id}
              href={destino(n)}
              className="block border-b border-border px-4 py-3.5 transition last:border-0 hover:bg-accent"
            >
              <div className="flex items-start gap-3">
                <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium">{n.titulo}</p>
                  <p className="mt-0.5 text-sm text-muted-foreground">{n.cuerpo}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{fechaHoraLarga(n.creadoEn)}</p>
                </div>
              </div>
            </Link>
          ))}
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card p-12 text-center text-muted-foreground">
          No tienes notificaciones sin leer.
        </div>
      )}
    </div>
  );
}
