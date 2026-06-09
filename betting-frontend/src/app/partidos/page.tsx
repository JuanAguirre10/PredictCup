"use client";

import { useState } from "react";
import useSWR from "swr";
import { ChevronLeft, ChevronRight, CalendarDays } from "lucide-react";
import { apiFetch } from "@/lib/api";
import type { PartidoResponse } from "@/lib/types";
import { hoyISO, sumarDias, fechaLarga } from "@/lib/fecha";
import TarjetaPartido from "@/components/TarjetaPartido";

function TarjetaSkeleton() {
  return (
    <div className="h-44 animate-pulse rounded-2xl border border-border bg-card" />
  );
}

export default function PartidosPage() {
  const [fecha, setFecha] = useState<string>(hoyISO());

  const { data, isLoading, error } = useSWR<PartidoResponse[]>(
    `/api/partidos?fecha=${fecha}`,
    (e: string) => apiFetch<PartidoResponse[]>(e),
  );

  const esHoy = fecha === hoyISO();

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Partidos</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Predice los marcadores antes del cierre de apuestas.
          </p>
        </div>

        <div className="flex items-center gap-2 rounded-xl border border-border bg-card p-1.5">
          <button
            onClick={() => setFecha(sumarDias(fecha, -1))}
            aria-label="Día anterior"
            className="rounded-lg p-2 text-muted-foreground transition hover:bg-accent hover:text-foreground"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
          <div className="flex min-w-52 items-center justify-center gap-2 text-sm font-medium capitalize">
            <CalendarDays className="h-4 w-4 text-primary" />
            <span>{esHoy ? "Hoy" : fechaLarga(fecha)}</span>
          </div>
          <button
            onClick={() => setFecha(sumarDias(fecha, 1))}
            aria-label="Día siguiente"
            className="rounded-lg p-2 text-muted-foreground transition hover:bg-accent hover:text-foreground"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>

      {error ? (
        <p className="rounded-xl border border-red-500/20 bg-red-500/5 p-6 text-center text-sm text-red-400">
          No se pudieron cargar los partidos. ¿Está el backend en marcha?
        </p>
      ) : isLoading ? (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <TarjetaSkeleton key={i} />
          ))}
        </div>
      ) : data && data.length > 0 ? (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {data.map((p) => (
            <TarjetaPartido key={p.id} partido={p} />
          ))}
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <p className="text-muted-foreground">No hay partidos para este día.</p>
          <p className="mt-1 text-sm text-muted-foreground">
            Prueba con otra fecha usando las flechas.
          </p>
        </div>
      )}
    </div>
  );
}
