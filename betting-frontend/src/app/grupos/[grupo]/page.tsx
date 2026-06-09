"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import useSWR from "swr";
import { ArrowLeft, Trophy, ListChecks } from "lucide-react";
import { apiFetch } from "@/lib/api";
import type { PartidoResponse, TopApostador } from "@/lib/types";
import TablaGrupo from "@/components/TablaGrupo";
import TarjetaPartido from "@/components/TarjetaPartido";

export default function GrupoDetallePage() {
  const params = useParams<{ grupo: string }>();
  const grupo = (params.grupo ?? "").toUpperCase();

  const { data: partidos, isLoading: cargandoPartidos } = useSWR<PartidoResponse[]>(
    grupo ? `/api/partidos/grupo/${grupo}` : null,
    (e: string) => apiFetch<PartidoResponse[]>(e),
  );
  const { data: top, isLoading: cargandoTop } = useSWR<TopApostador[]>(
    grupo ? `/api/partidos/grupo/${grupo}/apostadores` : null,
    (e: string) => apiFetch<TopApostador[]>(e),
  );

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <Link
        href="/grupos"
        className="mb-6 inline-flex items-center gap-1.5 text-sm text-muted-foreground transition hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" /> Grupos
      </Link>

      <h1 className="mb-6 text-2xl font-bold tracking-tight">Grupo {grupo}</h1>

      <TablaGrupo grupo={grupo} />

      {/* Partidos del grupo */}
      <section className="mt-8">
        <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
          <ListChecks className="h-4 w-4 text-primary" />
          Partidos del grupo
        </h2>
        {cargandoPartidos ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="h-44 animate-pulse rounded-2xl border border-border bg-card" />
            <div className="h-44 animate-pulse rounded-2xl border border-border bg-card" />
          </div>
        ) : partidos && partidos.length > 0 ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {partidos.map((p) => (
              <TarjetaPartido key={p.id} partido={p} />
            ))}
          </div>
        ) : (
          <p className="rounded-2xl border border-border bg-card p-6 text-center text-sm text-muted-foreground">
            Aún no hay partidos para este grupo.
          </p>
        )}
      </section>

      {/* Top apostadores del grupo */}
      <section className="mt-8">
        <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
          <Trophy className="h-4 w-4 text-primary" />
          Top apostadores del grupo
        </h2>
        <div className="overflow-hidden rounded-2xl border border-border bg-card">
          {cargandoTop ? (
            <div className="space-y-px">
              {Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="h-14 animate-pulse bg-muted" />
              ))}
            </div>
          ) : top && top.length > 0 ? (
            top.map((a, i) => (
              <div
                key={a.idUsuario}
                className="flex items-center gap-3 border-b border-border px-4 py-3 last:border-0"
              >
                <span
                  className={`flex h-6 w-6 items-center justify-center rounded text-xs font-bold ${
                    i < 3 ? "bg-primary/20 text-primary" : "text-muted-foreground"
                  }`}
                >
                  {i + 1}
                </span>
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/15 text-sm font-bold text-primary">
                  {(a.nombre || "U").charAt(0).toUpperCase()}
                </div>
                <span className="flex-1 truncate text-sm font-medium">{a.nombre}</span>
                <span className="text-xs text-muted-foreground">{a.aciertos} aciertos</span>
                <span className="w-14 text-right font-bold tabular-nums">{a.puntos} pts</span>
              </div>
            ))
          ) : (
            <p className="p-6 text-center text-sm text-muted-foreground">
              Todavía no hay puntuaciones en este grupo.
            </p>
          )}
        </div>
      </section>
    </div>
  );
}
