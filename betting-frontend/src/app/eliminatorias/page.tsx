"use client";

import useSWR from "swr";
import Link from "next/link";
import { apiFetch } from "@/lib/api";
import type { PartidoBracket } from "@/lib/types";
import { FASES } from "@/lib/fases";
import { fechaHoraLarga } from "@/lib/fecha";
import Bandera from "@/components/Bandera";
import EstadoBadge from "@/components/EstadoBadge";

function Equipo({
  pais,
  alineado,
}: {
  pais: PartidoBracket["paisLocal"];
  alineado: "izq" | "der";
}) {
  if (!pais) {
    return <span className="flex-1 text-sm italic text-muted-foreground">Por definir</span>;
  }
  return (
    <span
      className={`flex flex-1 items-center gap-1.5 text-sm font-medium ${
        alineado === "der" ? "flex-row-reverse text-right" : ""
      }`}
    >
      <Bandera codigo={pais.codigoFifa} emoji={pais.banderaEmoji} size={18} />
      <span className="truncate">{pais.nombreEs}</span>
    </span>
  );
}

function Tarjeta({ p }: { p: PartidoBracket }) {
  const definido = p.paisLocal && p.paisVisitante;
  const tieneResultado = p.golesLocal !== null && p.golesVisitante !== null;

  const cuerpo = (
    <div className="rounded-2xl border border-border bg-card p-4 transition hover:border-primary/40 hover:shadow-md">
      <div className="flex items-center gap-2">
        <Equipo pais={p.paisLocal} alineado="izq" />
        <span className="shrink-0 px-2 text-base font-bold tabular-nums">
          {tieneResultado ? (
            `${p.golesLocal}-${p.golesVisitante}`
          ) : (
            <span className="text-xs uppercase text-muted-foreground">vs</span>
          )}
        </span>
        <Equipo pais={p.paisVisitante} alineado="der" />
      </div>

      {definido && (
        <div className="mt-3 flex items-center justify-between gap-2 text-xs text-muted-foreground">
          <EstadoBadge estado={p.estado} />
          {p.fechaHora ? <span>{fechaHoraLarga(p.fechaHora)} UTC</span> : <span>Sin fecha</span>}
        </div>
      )}

      {p.miApuesta && (
        <p className="mt-2 rounded-lg bg-primary/10 px-2 py-1 text-xs text-primary">
          Tu predicción: {p.miApuesta.golesLocal}-{p.miApuesta.golesVisitante}
        </p>
      )}
      {definido && p.apuestasAbiertas && !p.miApuesta && (
        <p className="mt-2 text-xs font-medium text-primary">Abierto para predecir →</p>
      )}
    </div>
  );

  return definido ? (
    <Link href={`/partidos/${p.id}`} className="block">
      {cuerpo}
    </Link>
  ) : (
    cuerpo
  );
}

export default function EliminatoriasPage() {
  const { data, isLoading } = useSWR<PartidoBracket[]>("/api/partidos/bracket", (e: string) =>
    apiFetch<PartidoBracket[]>(e),
  );

  if (isLoading) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-10">
        <div className="h-64 animate-pulse rounded-2xl border border-border bg-card" />
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-20 text-center">
        <h1 className="text-2xl font-bold">Eliminatorias</h1>
        <p className="mt-3 text-muted-foreground">
          El cuadro de eliminatorias aún no está disponible. Se habilitará al terminar la fase de grupos.
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-10">
      <h1 className="text-3xl font-extrabold tracking-tight">Eliminatorias</h1>
      <p className="mt-2 text-muted-foreground">
        Predice cada cruce: ganas puntos con las mismas reglas que en la fase de grupos.
      </p>

      <div className="mt-8 space-y-10">
        {FASES.map(({ key, label }) => {
          const partidos = data.filter((p) => p.fase === key);
          if (partidos.length === 0) return null;
          return (
            <section key={key}>
              <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                {label}
              </h2>
              <div className="grid gap-4 sm:grid-cols-2">
                {partidos.map((p) => (
                  <Tarjeta key={p.id} p={p} />
                ))}
              </div>
            </section>
          );
        })}
      </div>
    </div>
  );
}
