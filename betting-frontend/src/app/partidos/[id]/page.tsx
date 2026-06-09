"use client";

import { useParams } from "next/navigation";
import useSWR from "swr";
import { MapPin, CalendarClock, ArrowLeft } from "lucide-react";
import Link from "next/link";
import { apiFetch } from "@/lib/api";
import type { PartidoResponse, PartidoVs } from "@/lib/types";
import { fechaHoraLarga } from "@/lib/fecha";
import EstadoBadge from "@/components/EstadoBadge";
import FormularioApuesta from "@/components/FormularioApuesta";
import TablaGrupo from "@/components/TablaGrupo";
import Bandera from "@/components/Bandera";

export default function PartidoDetallePage() {
  const params = useParams<{ id: string }>();
  const id = params.id;

  // Detalle: incluye apuestasAbiertas y miApuesta (necesarios para el formulario).
  const { data: partido, isLoading, mutate } = useSWR<PartidoResponse>(
    id ? `/api/partidos/${id}` : null,
    (e: string) => apiFetch<PartidoResponse>(e),
  );
  // Vista vs: trae la tabla de posiciones del grupo.
  const { data: vs } = useSWR<PartidoVs>(
    id ? `/api/partidos/${id}/vs` : null,
    (e: string) => apiFetch<PartidoVs>(e),
  );

  if (isLoading) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <div className="h-52 animate-pulse rounded-2xl border border-border bg-card" />
      </div>
    );
  }

  if (!partido) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-20 text-center text-muted-foreground">
        Partido no encontrado.
      </div>
    );
  }

  const p = partido;
  const tieneMarcador = p.golesLocal !== null && p.golesVisitante !== null;

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <Link
        href="/partidos"
        className="mb-6 inline-flex items-center gap-1.5 text-sm text-muted-foreground transition hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" /> Partidos
      </Link>

      {/* Cabecera del enfrentamiento */}
      <div className="relative overflow-hidden rounded-2xl border border-border bg-card p-6 text-center">
        <div className="pointer-events-none absolute inset-x-0 top-0 h-24 bg-gradient-to-b from-primary/10 to-transparent" />
        <div className="mb-4 flex justify-center">
          <EstadoBadge estado={p.estado} />
        </div>

        <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-4">
          <div className="flex flex-col items-center gap-2">
            <Bandera codigo={p.paisLocal.codigoFifa} emoji={p.paisLocal.banderaEmoji} size={48} />
            <span className="text-sm font-medium">{p.paisLocal.nombreEs}</span>
          </div>
          <div className="text-4xl font-extrabold tabular-nums">
            {tieneMarcador ? (
              <>
                {p.golesLocal}
                <span className="mx-2 text-muted-foreground">-</span>
                {p.golesVisitante}
              </>
            ) : (
              <span className="text-xl uppercase tracking-wide text-muted-foreground">vs</span>
            )}
          </div>
          <div className="flex flex-col items-center gap-2">
            <Bandera codigo={p.paisVisitante.codigoFifa} emoji={p.paisVisitante.banderaEmoji} size={48} />
            <span className="text-sm font-medium">{p.paisVisitante.nombreEs}</span>
          </div>
        </div>

        <div className="mt-5 space-y-1 text-sm text-muted-foreground">
          {p.estadio && (
            <p className="inline-flex items-center gap-1.5">
              <MapPin className="h-3.5 w-3.5" />
              {p.estadio.nombre}, {p.estadio.ciudad}
            </p>
          )}
          <p className="flex items-center justify-center gap-1.5">
            <CalendarClock className="h-3.5 w-3.5" />
            {fechaHoraLarga(p.fechaHora)} UTC
          </p>
        </div>
      </div>

      {/* Apuesta: predicción guardada, formulario o cierre */}
      <div className="mt-6">
        {p.miApuesta ? (
          <div className="rounded-2xl border border-primary/30 bg-primary/5 p-6 text-center">
            <p className="text-sm text-muted-foreground">Tu predicción</p>
            <p className="mt-1 text-3xl font-bold tabular-nums">
              {p.miApuesta.golesLocal} - {p.miApuesta.golesVisitante}
            </p>
            {p.miApuesta.puntosTotal !== null ? (
              <p className="mt-2 text-sm font-medium text-emerald-400">
                Ganaste {p.miApuesta.puntosTotal} puntos
              </p>
            ) : (
              <p className="mt-2 text-xs text-muted-foreground">
                Se calcularán los puntos al terminar el partido.
              </p>
            )}
          </div>
        ) : p.apuestasAbiertas ? (
          <FormularioApuesta
            idPartido={p.id}
            local={p.paisLocal}
            visitante={p.paisVisitante}
            onExito={() => mutate()}
          />
        ) : (
          <div className="rounded-2xl border border-border bg-card p-6 text-center text-sm text-muted-foreground">
            Las apuestas para este partido están cerradas.
          </div>
        )}
      </div>

      {/* Tabla del grupo */}
      {p.grupo && (
        <div className="mt-8">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
            Posiciones · Grupo {p.grupo}
          </h2>
          <TablaGrupo
            grupo={p.grupo}
            posiciones={vs?.tablaPosicionesGrupo}
            resaltar={[p.paisLocal.id, p.paisVisitante.id]}
          />
        </div>
      )}
    </div>
  );
}
