"use client";

import { useState } from "react";
import useSWR from "swr";
import { Loader2, X } from "lucide-react";
import { toast } from "sonner";
import { apiFetch, ApiError } from "@/lib/api";
import type { PartidoResponse } from "@/lib/types";
import Bandera from "./Bandera";

interface Props {
  partido: PartidoResponse;
  onGuardar: () => void;
  onClose: () => void;
}

export default function ModalMarcador({ partido, onGuardar, onClose }: Props) {
  const [golesLocal, setGolesLocal] = useState(partido.golesLocal ?? 0);
  const [golesVisitante, setGolesVisitante] = useState(partido.golesVisitante ?? 0);
  const [estado, setEstado] = useState(
    partido.estado === "TERMINADO" ? "TERMINADO" : "EN_CURSO",
  );
  const [guardando, setGuardando] = useState(false);

  // Cuántas apuestas se puntuarán si se marca TERMINADO.
  const { data: conteo } = useSWR<{ conteo: number }>(
    estado === "TERMINADO" ? `/api/apuestas/partido/${partido.id}/conteo` : null,
    (e: string) => apiFetch<{ conteo: number }>(e),
  );

  const guardar = async () => {
    setGuardando(true);
    try {
      await apiFetch(`/api/partidos/${partido.id}/marcador`, {
        method: "PATCH",
        body: JSON.stringify({ golesLocal, golesVisitante, estado }),
      });
      toast.success("Marcador actualizado");
      onGuardar();
      onClose();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "No se pudo actualizar");
    } finally {
      setGuardando(false);
    }
  };

  const num = (v: number, set: (n: number) => void) => (
    <input
      type="number"
      min={0}
      max={99}
      value={v}
      onChange={(e) => set(Math.max(0, Math.min(99, Number(e.target.value) || 0)))}
      className="w-16 rounded-md border border-border bg-card px-2 py-2 text-center text-xl font-bold tabular-nums outline-none focus:border-primary/50"
    />
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="w-full max-w-sm rounded-2xl border border-border bg-card p-6">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-bold">Actualizar marcador</h2>
          <button onClick={onClose} aria-label="Cerrar" className="rounded p-1 hover:bg-accent">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex items-center justify-center gap-4">
          <div className="text-center">
            <div className="mb-2 flex justify-center">
              <Bandera codigo={partido.paisLocal.codigoFifa} emoji={partido.paisLocal.banderaEmoji} size={30} />
            </div>
            {num(golesLocal, setGolesLocal)}
          </div>
          <span className="text-2xl font-bold text-muted-foreground">:</span>
          <div className="text-center">
            <div className="mb-2 flex justify-center">
              <Bandera codigo={partido.paisVisitante.codigoFifa} emoji={partido.paisVisitante.banderaEmoji} size={30} />
            </div>
            {num(golesVisitante, setGolesVisitante)}
          </div>
        </div>

        <label className="mt-5 block text-sm font-medium text-muted-foreground">Estado</label>
        <select
          value={estado}
          onChange={(e) => setEstado(e.target.value)}
          className="mt-1.5 w-full rounded-md border border-border bg-card px-3 py-2.5 text-sm outline-none focus:border-primary/50"
        >
          <option value="EN_CURSO">En curso</option>
          <option value="TERMINADO">Terminado</option>
        </select>

        {estado === "TERMINADO" && (
          <p className="mt-3 rounded-md bg-amber-500/10 px-3 py-2 text-xs text-amber-400">
            Se calcularán puntos para {conteo?.conteo ?? "…"} apuestas.
          </p>
        )}

        <button
          onClick={guardar}
          disabled={guardando}
          className="mt-5 flex w-full items-center justify-center gap-2 rounded-md bg-primary px-6 py-3 font-semibold text-primary-foreground transition hover:opacity-90 disabled:opacity-60"
        >
          {guardando && <Loader2 className="h-4 w-4 animate-spin" />}
          Guardar
        </button>
      </div>
    </div>
  );
}
