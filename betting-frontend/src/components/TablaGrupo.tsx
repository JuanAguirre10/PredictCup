"use client";

import useSWR from "swr";
import { apiFetch } from "@/lib/api";
import type { TablaPosicion } from "@/lib/types";
import Bandera from "./Bandera";

interface Props {
  grupo: string;
  /** Si se pasan, se usan tal cual (no hace fetch). */
  posiciones?: TablaPosicion[];
  /** IDs de país a resaltar (p. ej. los dos equipos del partido). */
  resaltar?: string[];
}

const COLS = [
  { k: "PJ", t: "Partidos jugados" },
  { k: "G", t: "Ganados" },
  { k: "E", t: "Empatados" },
  { k: "P", t: "Perdidos" },
  { k: "DG", t: "Diferencia de goles" },
  { k: "Pts", t: "Puntos" },
];

export default function TablaGrupo({ grupo, posiciones, resaltar = [] }: Props) {
  // Hook siempre invocado; key null desactiva el fetch cuando ya hay datos.
  const { data, isLoading } = useSWR<TablaPosicion[]>(
    posiciones ? null : `/api/posiciones/${grupo}`,
    (e: string) => apiFetch<TablaPosicion[]>(e),
  );
  const filas = posiciones ?? data ?? [];

  return (
    <div className="overflow-hidden rounded-2xl border border-border bg-card">
      <div className="flex items-center justify-between border-b border-border px-4 py-3">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
          Grupo {grupo}
        </h3>
        <span className="text-xs text-muted-foreground">Posiciones</span>
      </div>

      <table className="w-full text-sm">
        <thead>
          <tr className="text-xs text-muted-foreground">
            <th className="w-8 px-2 py-2 text-center font-medium">#</th>
            <th className="px-2 py-2 text-left font-medium">País</th>
            {COLS.map((c) => (
              <th key={c.k} title={c.t} className="w-9 px-1 py-2 text-center font-medium">
                {c.k}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {isLoading && filas.length === 0 ? (
            Array.from({ length: 4 }).map((_, i) => (
              <tr key={i} className="border-t border-border">
                <td colSpan={8} className="px-2 py-3">
                  <div className="h-4 w-full animate-pulse rounded bg-muted" />
                </td>
              </tr>
            ))
          ) : filas.length === 0 ? (
            <tr>
              <td colSpan={8} className="px-2 py-6 text-center text-xs text-muted-foreground">
                Sin posiciones todavía.
              </td>
            </tr>
          ) : (
            filas.map((f) => {
              const top2 = f.posicion >= 1 && f.posicion <= 2;
              const activo = resaltar.includes(f.pais.id);
              return (
                <tr
                  key={f.pais.id}
                  className={`border-t border-border transition ${
                    activo ? "bg-primary/10" : "hover:bg-accent"
                  }`}
                >
                  <td className="px-2 py-2.5 text-center">
                    <span
                      className={`inline-flex h-5 w-5 items-center justify-center rounded text-xs font-semibold ${
                        top2 ? "bg-primary/20 text-primary" : "text-muted-foreground"
                      }`}
                    >
                      {f.posicion}
                    </span>
                  </td>
                  <td className="px-2 py-2.5">
                    <div className="flex items-center gap-2">
                      <Bandera codigo={f.pais.codigoFifa} emoji={f.pais.banderaEmoji} size={18} />
                      <span className="truncate font-medium">{f.pais.nombreEs}</span>
                    </div>
                  </td>
                  <td className="px-1 py-2.5 text-center tabular-nums text-muted-foreground">
                    {f.partidosJugados}
                  </td>
                  <td className="px-1 py-2.5 text-center tabular-nums text-muted-foreground">
                    {f.partidosGanados}
                  </td>
                  <td className="px-1 py-2.5 text-center tabular-nums text-muted-foreground">
                    {f.partidosEmpatados}
                  </td>
                  <td className="px-1 py-2.5 text-center tabular-nums text-muted-foreground">
                    {f.partidosPerdidos}
                  </td>
                  <td className="px-1 py-2.5 text-center tabular-nums text-muted-foreground">
                    {f.diferenciaGoles > 0 ? `+${f.diferenciaGoles}` : f.diferenciaGoles}
                  </td>
                  <td className="px-1 py-2.5 text-center font-bold tabular-nums text-foreground">
                    {f.puntos}
                  </td>
                </tr>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );
}
