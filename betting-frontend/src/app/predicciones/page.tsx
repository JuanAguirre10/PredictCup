"use client";

import useSWR from "swr";
import Link from "next/link";
import { apiFetch } from "@/lib/api";
import type { HistorialApuesta } from "@/lib/types";
import Bandera from "@/components/Bandera";
import EstadoBadge from "@/components/EstadoBadge";

const CATEGORIA: Record<string, { label: string; clase: string }> = {
  EXACTO: { label: "Exacto", clase: "bg-emerald-500/15 text-emerald-500 ring-emerald-500/30" },
  GANADOR: { label: "Ganador", clase: "bg-sky-500/15 text-sky-500 ring-sky-500/30" },
  DIFERENCIA: { label: "Diferencia", clase: "bg-amber-500/15 text-amber-500 ring-amber-500/30" },
  NINGUNO: { label: "Sin acierto", clase: "bg-muted text-muted-foreground ring-border" },
};

function Resumen({ label, valor }: { label: string; valor: number | string }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-4 text-center">
      <p className="text-2xl font-extrabold tabular-nums">{valor}</p>
      <p className="mt-0.5 text-xs uppercase tracking-wide text-muted-foreground">{label}</p>
    </div>
  );
}

function Fila({ h }: { h: HistorialApuesta }) {
  const puntuada = h.puntos !== null;
  const cat = h.tipoResultado ? CATEGORIA[h.tipoResultado] : null;
  const tieneReal = h.golesRealLocal !== null && h.golesRealVisitante !== null;

  return (
    <Link
      href={`/partidos/${h.idPartido}`}
      className="flex flex-col gap-3 border-b border-border px-4 py-4 transition last:border-0 hover:bg-accent sm:flex-row sm:items-center"
    >
      {/* Enfrentamiento */}
      <div className="flex flex-1 items-center gap-2 text-sm">
        <Bandera codigo={h.localCodigoFifa} emoji={h.localEmoji} size={18} />
        <span className="font-medium">{h.localNombre}</span>
        <span className="text-muted-foreground">vs</span>
        <Bandera codigo={h.visitanteCodigoFifa} emoji={h.visitanteEmoji} size={18} />
        <span className="font-medium">{h.visitanteNombre}</span>
      </div>

      {/* Predicción vs resultado */}
      <div className="flex items-center gap-4 text-sm">
        <div className="text-center">
          <p className="text-[10px] uppercase tracking-wide text-muted-foreground">Predicción</p>
          <p className="font-bold tabular-nums">
            {h.golesLocal}-{h.golesVisitante}
          </p>
        </div>
        <div className="text-center">
          <p className="text-[10px] uppercase tracking-wide text-muted-foreground">Resultado</p>
          {tieneReal ? (
            <p className="font-bold tabular-nums">
              {h.golesRealLocal}-{h.golesRealVisitante}
            </p>
          ) : (
            <EstadoBadge estado={h.estado} />
          )}
        </div>
      </div>

      {/* Puntos */}
      <div className="flex items-center justify-end gap-2 sm:w-44">
        {puntuada ? (
          <>
            {cat && (
              <span
                className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ring-1 ring-inset ${cat.clase}`}
              >
                {cat.label}
              </span>
            )}
            {(h.bonusRacha ?? 0) > 0 && (
              <span className="text-[11px] text-amber-500" title="Bonus racha">
                🔥
              </span>
            )}
            {(h.bonusAnticipada ?? 0) > 0 && (
              <span className="text-[11px] text-amber-500" title="Bonus anticipada">
                ⏱
              </span>
            )}
            <span
              className={`w-12 text-right font-extrabold tabular-nums ${
                (h.puntos ?? 0) > 0 ? "text-primary" : "text-muted-foreground"
              }`}
            >
              {(h.puntos ?? 0) > 0 ? `+${h.puntos}` : "0"}
            </span>
          </>
        ) : (
          <span className="text-xs text-muted-foreground">Pendiente</span>
        )}
      </div>
    </Link>
  );
}

export default function PrediccionesPage() {
  const { data, isLoading } = useSWR<HistorialApuesta[]>(
    "/api/apuestas/historial?limite=100",
    (e: string) => apiFetch<HistorialApuesta[]>(e),
  );

  const total = data?.length ?? 0;
  const aciertos = data?.filter((h) => (h.puntos ?? 0) > 0).length ?? 0;
  const puntos = data?.reduce((s, h) => s + (h.puntos ?? 0), 0) ?? 0;

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="text-2xl font-bold tracking-tight">Mis predicciones</h1>
      <p className="mt-1 text-sm text-muted-foreground">
        Todas tus predicciones y los puntos que ganaste. Solo tú puedes ver esto.
      </p>

      <div className="mt-6 grid grid-cols-3 gap-4">
        <Resumen label="Predicciones" valor={total} />
        <Resumen label="Aciertos" valor={aciertos} />
        <Resumen label="Puntos" valor={puntos} />
      </div>

      <div className="mt-6 overflow-hidden rounded-2xl border border-border bg-card">
        {isLoading ? (
          <div className="h-40 animate-pulse bg-muted" />
        ) : data && data.length > 0 ? (
          data.map((h) => <Fila key={h.id} h={h} />)
        ) : (
          <p className="p-12 text-center text-sm text-muted-foreground">
            Aún no has hecho ninguna predicción.{" "}
            <Link href="/partidos" className="font-semibold text-primary hover:underline">
              Ve a los partidos
            </Link>{" "}
            y predice tu primer marcador.
          </p>
        )}
      </div>
    </div>
  );
}
