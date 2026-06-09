"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Check, Copy, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { apiFetch, ApiError } from "@/lib/api";
import type { Sala } from "@/lib/types";

export default function CrearSalaPage() {
  const router = useRouter();
  const [nombre, setNombre] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [esPublica, setEsPublica] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [creada, setCreada] = useState<Sala | null>(null);
  const [copiado, setCopiado] = useState(false);

  const crear = async (ev: React.FormEvent) => {
    ev.preventDefault();
    if (nombre.trim().length < 2) {
      toast.error("Ponle un nombre a la sala");
      return;
    }
    setEnviando(true);
    try {
      const sala = await apiFetch<Sala>("/api/salas", {
        method: "POST",
        body: JSON.stringify({
          nombre: nombre.trim(),
          descripcion: descripcion.trim() || null,
          esPublica,
          maxMiembros: 100,
        }),
      });
      setCreada(sala);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "No se pudo crear la sala");
    } finally {
      setEnviando(false);
    }
  };

  const copiar = async () => {
    if (!creada) return;
    await navigator.clipboard.writeText(creada.codigo);
    setCopiado(true);
    setTimeout(() => setCopiado(false), 1500);
  };

  return (
    <div className="mx-auto max-w-md px-4 py-10">
      <Link
        href="/salas"
        className="mb-6 inline-flex items-center gap-1.5 text-sm text-muted-foreground transition hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" /> Mis salas
      </Link>

      <h1 className="mb-6 text-2xl font-bold tracking-tight">Crear sala</h1>

      <form onSubmit={crear} className="space-y-5 rounded-2xl border border-border bg-card p-6">
        <div>
          <label className="mb-1.5 block text-sm font-medium text-muted-foreground">Nombre</label>
          <input
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            maxLength={100}
            placeholder="Los cracks del mundial"
            className="w-full rounded-md border border-border bg-card px-3 py-2.5 text-sm outline-none focus:border-primary/50"
          />
        </div>
        <div>
          <label className="mb-1.5 block text-sm font-medium text-muted-foreground">Descripción</label>
          <textarea
            value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
            maxLength={255}
            rows={3}
            placeholder="Opcional"
            className="w-full resize-none rounded-md border border-border bg-card px-3 py-2.5 text-sm outline-none focus:border-primary/50"
          />
        </div>
        <label className="flex cursor-pointer items-center justify-between rounded-md border border-border bg-background px-3 py-2.5">
          <span className="text-sm text-muted-foreground">
            {esPublica ? "Pública" : "Privada"}
            <span className="ml-2 text-xs text-muted-foreground">
              {esPublica ? "cualquiera con el código entra" : "solo por invitación"}
            </span>
          </span>
          <button
            type="button"
            role="switch"
            aria-checked={esPublica}
            onClick={() => setEsPublica((v) => !v)}
            className={`relative h-6 w-11 rounded-full transition ${esPublica ? "bg-primary" : "bg-slate-600"}`}
          >
            <span
              className={`absolute top-0.5 h-5 w-5 rounded-full bg-white transition ${esPublica ? "left-[22px]" : "left-0.5"}`}
            />
          </button>
        </label>

        <button
          type="submit"
          disabled={enviando}
          className="flex w-full items-center justify-center gap-2 rounded-md bg-primary px-6 py-3 font-semibold text-primary-foreground transition hover:opacity-90 disabled:opacity-60"
        >
          {enviando && <Loader2 className="h-4 w-4 animate-spin" />}
          Crear sala
        </button>
      </form>

      {creada && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-sm rounded-2xl border border-border bg-card p-6 text-center">
            <h2 className="text-lg font-bold">¡Sala creada!</h2>
            <p className="mt-1 text-sm text-muted-foreground">Comparte este código para invitar:</p>
            <div className="my-5 flex items-center justify-center gap-2">
              <span className="rounded-lg bg-background px-5 py-3 font-mono text-2xl font-bold tracking-[0.3em] text-primary">
                {creada.codigo}
              </span>
              <button
                onClick={copiar}
                aria-label="Copiar código"
                className="rounded-md border border-border p-3 transition hover:bg-accent"
              >
                {copiado ? <Check className="h-5 w-5 text-emerald-400" /> : <Copy className="h-5 w-5" />}
              </button>
            </div>
            <button
              onClick={() => router.push(`/salas/${creada.id}`)}
              className="w-full rounded-md bg-primary px-6 py-2.5 font-semibold text-primary-foreground transition hover:opacity-90"
            >
              Ir a la sala
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
