"use client";

import useSWR from "swr";
import { Radio } from "lucide-react";
import { apiFetch } from "@/lib/api";
import { useRanking } from "@/hooks/useRanking";
import { useUsuario } from "@/hooks/useUsuario";
import type { RankingEntry } from "@/lib/types";
import RankingTable from "@/components/RankingTable";

export default function RankingPage() {
  const { ranking, isLoading, conectado } = useRanking("global");
  const { usuario } = useUsuario();
  const miId = usuario?.sub ?? null;

  // Mi posición, aunque no esté en el top 50.
  const { data: miFila } = useSWR<RankingEntry>(
    usuario ? "/api/usuarios/ranking/yo" : null,
    (e: string) => apiFetch<RankingEntry>(e),
  );

  const enTop = miId ? ranking.some((r) => r.idUsuario === miId) : true;
  const entries =
    !enTop && miFila && miFila.posicion > 0 ? [...ranking, miFila] : ranking;

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Ranking global</h1>
          <p className="mt-1 text-sm text-muted-foreground">Top 50 de PredictCup.</p>
        </div>
        <span
          className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ring-1 ring-inset ${
            conectado
              ? "bg-emerald-500/15 text-emerald-400 ring-emerald-500/30"
              : "bg-slate-500/15 text-muted-foreground ring-slate-500/30"
          }`}
        >
          <Radio className="h-3.5 w-3.5" />
          {conectado ? "En vivo" : "Conectando…"}
        </span>
      </div>

      {isLoading ? (
        <div className="h-80 animate-pulse rounded-2xl border border-border bg-card" />
      ) : (
        <RankingTable entries={entries} miId={miId} />
      )}
    </div>
  );
}
