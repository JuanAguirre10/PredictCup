"use client";

import { useState, type ReactNode } from "react";
import { Minus, Plus, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { apiFetch, ApiError } from "@/lib/api";
import { isLoggedIn } from "@/lib/auth";
import type { ApuestaResponse, PaisMini } from "@/lib/types";
import Bandera from "./Bandera";

interface Props {
  idPartido: string;
  local: PaisMini;
  visitante: PaisMini;
  onExito?: () => void;
}

type Estado = "idle" | "enviando" | "confirmando";

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

/** Sondea la apuesta del usuario para este partido hasta confirmarla o agotar el tiempo. */
async function esperarConfirmacion(idPartido: string, maxMs = 30_000): Promise<boolean> {
  const limite = Date.now() + maxMs;
  while (Date.now() < limite) {
    try {
      await apiFetch<ApuestaResponse>(`/api/apuestas/partido/${idPartido}`);
      return true;
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        await sleep(2000); // aún en la cola: reintenta
        continue;
      }
      throw e;
    }
  }
  return false;
}

function Stepper({
  valor,
  setValor,
  bandera,
}: {
  valor: number;
  setValor: (n: number) => void;
  bandera: ReactNode;
}) {
  const clamp = (n: number) => Math.max(0, Math.min(9, n));
  return (
    <div className="flex flex-col items-center gap-3">
      {bandera}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={() => setValor(clamp(valor - 1))}
          aria-label="Restar"
          className="flex h-9 w-9 items-center justify-center rounded-lg border border-border text-muted-foreground transition hover:bg-accent disabled:opacity-40"
          disabled={valor <= 0}
        >
          <Minus className="h-4 w-4" />
        </button>
        <span className="w-10 text-center text-3xl font-bold tabular-nums">{valor}</span>
        <button
          type="button"
          onClick={() => setValor(clamp(valor + 1))}
          aria-label="Sumar"
          className="flex h-9 w-9 items-center justify-center rounded-lg border border-border text-muted-foreground transition hover:bg-accent disabled:opacity-40"
          disabled={valor >= 9}
        >
          <Plus className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

export default function FormularioApuesta({ idPartido, local, visitante, onExito }: Props) {
  const [golesLocal, setGolesLocal] = useState(0);
  const [golesVisitante, setGolesVisitante] = useState(0);
  const [estado, setEstado] = useState<Estado>("idle");

  if (!isLoggedIn()) {
    return (
      <div className="rounded-2xl border border-border bg-card p-6 text-center">
        <p className="text-sm text-muted-foreground">Inicia sesión para predecir este partido.</p>
        <a
          href="/login"
          className="mt-4 inline-block rounded-md bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground transition hover:opacity-90"
        >
          Entrar con Google
        </a>
      </div>
    );
  }

  const apostar = async () => {
    setEstado("enviando");
    const claveIdempotencia = crypto.randomUUID();
    try {
      await apiFetch("/api/apuestas", {
        method: "POST",
        body: JSON.stringify({ idPartido, golesLocal, golesVisitante, claveIdempotencia }),
      });
      toast.info("Apuesta recibida, confirmando…");
      setEstado("confirmando");

      const ok = await esperarConfirmacion(idPartido);
      if (ok) {
        toast.success("✓ Apuesta confirmada");
        onExito?.();
      } else {
        toast.warning("Tu apuesta sigue procesándose. Refresca en unos segundos.");
      }
    } catch (e) {
      const msg =
        e instanceof ApiError ? e.message : "No se pudo registrar la apuesta";
      toast.error(msg);
    } finally {
      setEstado("idle");
    }
  };

  const ocupado = estado !== "idle";

  return (
    <div className="rounded-2xl border border-border bg-card p-6">
      <h3 className="mb-5 text-center text-sm font-semibold uppercase tracking-wide text-muted-foreground">
        Tu predicción
      </h3>
      <div className="flex items-center justify-center gap-8">
        <Stepper
          valor={golesLocal}
          setValor={setGolesLocal}
          bandera={<Bandera codigo={local.codigoFifa} emoji={local.banderaEmoji} size={30} />}
        />
        <span className="text-2xl font-bold text-muted-foreground">:</span>
        <Stepper
          valor={golesVisitante}
          setValor={setGolesVisitante}
          bandera={<Bandera codigo={visitante.codigoFifa} emoji={visitante.banderaEmoji} size={30} />}
        />
      </div>

      <button
        onClick={apostar}
        disabled={ocupado}
        className="mt-6 flex w-full items-center justify-center gap-2 rounded-md bg-primary px-6 py-3 font-semibold text-primary-foreground transition hover:opacity-90 disabled:opacity-60"
      >
        {ocupado && <Loader2 className="h-4 w-4 animate-spin" />}
        {estado === "idle" && "Apostar"}
        {estado === "enviando" && "Enviando…"}
        {estado === "confirmando" && "Confirmando…"}
      </button>
    </div>
  );
}
