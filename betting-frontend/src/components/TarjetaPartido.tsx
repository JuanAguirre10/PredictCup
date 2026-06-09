import Link from "next/link";
import { CalendarClock } from "lucide-react";
import type { PartidoResponse } from "@/lib/types";
import { horaUTC } from "@/lib/fecha";
import EstadoBadge from "./EstadoBadge";
import Bandera from "./Bandera";

const tieneMarcador = (p: PartidoResponse) =>
  p.golesLocal !== null && p.golesVisitante !== null;

export default function TarjetaPartido({ partido }: { partido: PartidoResponse }) {
  const p = partido;

  return (
    <Link
      href={`/partidos/${p.id}`}
      className="group flex flex-col gap-4 rounded-2xl border border-border bg-card p-5 transition hover:border-primary/40 hover:bg-card"
    >
      <div className="flex items-center justify-between">
        <EstadoBadge estado={p.estado} />
        {p.apuestasAbiertas && (
          <span className="rounded-full bg-emerald-500/15 px-2.5 py-0.5 text-xs font-medium text-emerald-400 ring-1 ring-inset ring-emerald-500/30">
            Apuestas abiertas
          </span>
        )}
      </div>

      <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3">
        {/* Local */}
        <div className="flex items-center justify-end gap-2 text-right">
          <span className="truncate text-sm font-medium">{p.paisLocal.nombreEs}</span>
          <Bandera codigo={p.paisLocal.codigoFifa} emoji={p.paisLocal.banderaEmoji} size={22} />
        </div>

        {/* Centro: marcador o vs */}
        <div className="min-w-16 text-center">
          {tieneMarcador(p) ? (
            <span className="text-2xl font-bold tabular-nums">
              {p.golesLocal}
              <span className="mx-1.5 text-muted-foreground">-</span>
              {p.golesVisitante}
            </span>
          ) : (
            <span className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
              vs
            </span>
          )}
        </div>

        {/* Visitante */}
        <div className="flex items-center gap-2">
          <Bandera codigo={p.paisVisitante.codigoFifa} emoji={p.paisVisitante.banderaEmoji} size={22} />
          <span className="truncate text-sm font-medium">{p.paisVisitante.nombreEs}</span>
        </div>
      </div>

      <div className="flex items-center justify-between border-t border-border pt-3 text-xs text-muted-foreground">
        <span className="inline-flex items-center gap-1.5">
          <CalendarClock className="h-3.5 w-3.5" />
          {horaUTC(p.fechaHora)} UTC
          {p.grupo && <span className="text-muted-foreground">· Grupo {p.grupo}</span>}
        </span>
        {p.miApuesta && (
          <span className="font-medium text-primary">
            Tu predicción: {p.miApuesta.golesLocal}-{p.miApuesta.golesVisitante}
            {p.miApuesta.puntosTotal !== null && (
              <span className="ml-1 text-emerald-400">(+{p.miApuesta.puntosTotal})</span>
            )}
          </span>
        )}
      </div>
    </Link>
  );
}
