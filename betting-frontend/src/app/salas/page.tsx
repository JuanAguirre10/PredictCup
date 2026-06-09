"use client";

import { useState } from "react";
import Link from "next/link";
import useSWR from "swr";
import { useRouter } from "next/navigation";
import { Plus, Users, Crown, LogIn } from "lucide-react";
import { toast } from "sonner";
import { apiFetch, ApiError } from "@/lib/api";
import type { SalaResumen, Sala } from "@/lib/types";

export default function SalasPage() {
  const router = useRouter();
  const { data: salas, isLoading, mutate } = useSWR<SalaResumen[]>(
    "/api/salas/mias",
    (e: string) => apiFetch<SalaResumen[]>(e),
  );
  const [codigo, setCodigo] = useState("");
  const [uniendo, setUniendo] = useState(false);

  const unirse = async (ev: React.FormEvent) => {
    ev.preventDefault();
    if (codigo.trim().length !== 6) {
      toast.error("El código tiene 6 caracteres");
      return;
    }
    setUniendo(true);
    try {
      const sala = await apiFetch<Sala>("/api/salas/unirse", {
        method: "POST",
        body: JSON.stringify({ codigo: codigo.trim().toUpperCase() }),
      });
      toast.success(`Te uniste a ${sala.nombre}`);
      setCodigo("");
      await mutate();
      router.push(`/salas/${sala.id}`);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "No se pudo unir");
    } finally {
      setUniendo(false);
    }
  };

  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Mis salas</h1>
          <p className="mt-1 text-sm text-muted-foreground">Compite con tus amigos en grupos privados.</p>
        </div>
        <Link
          href="/salas/crear"
          className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground transition hover:opacity-90"
        >
          <Plus className="h-4 w-4" /> Crear sala
        </Link>
      </div>

      <form
        onSubmit={unirse}
        className="mb-8 flex items-center gap-2 rounded-xl border border-border bg-card p-2"
      >
        <input
          value={codigo}
          onChange={(e) => setCodigo(e.target.value.toUpperCase().slice(0, 6))}
          placeholder="Unirse con código (6 caracteres)"
          className="flex-1 bg-transparent px-3 py-2 text-sm tracking-widest outline-none placeholder:tracking-normal placeholder:text-muted-foreground"
        />
        <button
          type="submit"
          disabled={uniendo}
          className="inline-flex items-center gap-1.5 rounded-md border border-border px-4 py-2 text-sm font-medium transition hover:bg-accent disabled:opacity-60"
        >
          <LogIn className="h-4 w-4" /> Unirse
        </button>
      </form>

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="h-28 animate-pulse rounded-2xl border border-border bg-card" />
          <div className="h-28 animate-pulse rounded-2xl border border-border bg-card" />
        </div>
      ) : salas && salas.length > 0 ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {salas.map((s) => (
            <Link
              key={s.id}
              href={`/salas/${s.id}`}
              className="group rounded-2xl border border-border bg-card p-5 transition hover:border-primary/40 hover:bg-card"
            >
              <div className="flex items-start justify-between">
                <h3 className="font-semibold">{s.nombre}</h3>
                {s.esDueno && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/15 px-2 py-0.5 text-xs text-amber-400">
                    <Crown className="h-3 w-3" /> Dueño
                  </span>
                )}
              </div>
              {s.descripcion && (
                <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">{s.descripcion}</p>
              )}
              <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
                <span className="inline-flex items-center gap-1.5">
                  <Users className="h-3.5 w-3.5" /> {s.miembros} miembros
                </span>
                <span className="rounded bg-muted px-2 py-1 font-mono tracking-widest text-muted-foreground">
                  {s.codigo}
                </span>
              </div>
            </Link>
          ))}
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <p className="text-muted-foreground">Todavía no estás en ninguna sala.</p>
          <p className="mt-1 text-sm text-muted-foreground">Crea una o únete con un código.</p>
        </div>
      )}
    </div>
  );
}
