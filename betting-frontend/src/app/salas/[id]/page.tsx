"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import useSWR from "swr";
import { ArrowLeft, Check, Copy, Radio } from "lucide-react";
import { apiFetch } from "@/lib/api";
import { useRanking } from "@/hooks/useRanking";
import { useUsuario } from "@/hooks/useUsuario";
import type { Sala } from "@/lib/types";
import RankingTable from "@/components/RankingTable";

export default function SalaDetallePage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const { usuario } = useUsuario();

  const { data: sala } = useSWR<Sala>(
    id ? `/api/salas/${id}` : null,
    (e: string) => apiFetch<Sala>(e),
  );
  const { ranking, isLoading, conectado } = useRanking(`sala:${id}`);

  const [copiado, setCopiado] = useState(false);
  const copiar = async () => {
    if (!sala) return;
    await navigator.clipboard.writeText(sala.codigo);
    setCopiado(true);
    setTimeout(() => setCopiado(false), 1500);
  };

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <Link
        href="/salas"
        className="mb-6 inline-flex items-center gap-1.5 text-sm text-muted-foreground transition hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" /> Mis salas
      </Link>

      <div className="mb-6 flex flex-col gap-4 rounded-2xl border border-border bg-card p-5 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl font-bold tracking-tight">{sala?.nombre ?? "Sala"}</h1>
          {sala?.descripcion && <p className="mt-1 text-sm text-muted-foreground">{sala.descripcion}</p>}
        </div>
        {sala && (
          <div className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground">Código:</span>
            <span className="rounded bg-background px-3 py-1.5 font-mono text-sm font-bold tracking-[0.25em] text-primary">
              {sala.codigo}
            </span>
            <button
              onClick={copiar}
              aria-label="Copiar código"
              className="rounded-md border border-border p-2 transition hover:bg-accent"
            >
              {copiado ? <Check className="h-4 w-4 text-emerald-400" /> : <Copy className="h-4 w-4" />}
            </button>
          </div>
        )}
      </div>

      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
          Ranking de la sala
        </h2>
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
        <div className="h-64 animate-pulse rounded-2xl border border-border bg-card" />
      ) : (
        <RankingTable entries={ranking} miId={usuario?.sub ?? null} />
      )}
    </div>
  );
}
