"use client";

import { useRef, useState } from "react";
import useSWR from "swr";
import Link from "next/link";
import { Camera, Loader2, Pencil } from "lucide-react";
import { toast } from "sonner";
import { apiFetch, ApiError, BASE } from "@/lib/api";
import { getToken } from "@/lib/auth";
import type {
  UsuarioPerfil,
  RankingEntry,
  EstadisticasApuestas,
  HistorialApuesta,
} from "@/lib/types";

function StatCard({ label, valor }: { label: string; valor: number | string }) {
  return (
    <div className="rounded-xl border border-border bg-card p-4 text-center">
      <p className="text-2xl font-bold tabular-nums">{valor}</p>
      <p className="mt-0.5 text-xs uppercase tracking-wide text-muted-foreground">{label}</p>
    </div>
  );
}

export default function PerfilPage() {
  const { data: perfil, mutate: mutarPerfil } = useSWR<UsuarioPerfil>(
    "/api/usuarios/yo",
    (e: string) => apiFetch<UsuarioPerfil>(e),
  );
  const [editando, setEditando] = useState(false);
  const [nombre, setNombre] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [subiendo, setSubiendo] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const abrirEdicion = () => {
    setNombre(perfil?.nombreDisplay ?? "");
    setEditando(true);
  };

  const guardarNombre = async () => {
    if (nombre.trim().length < 1) {
      toast.error("El nombre no puede estar vacío");
      return;
    }
    setGuardando(true);
    try {
      await apiFetch("/api/usuarios/yo", {
        method: "PATCH",
        body: JSON.stringify({ nombreDisplay: nombre.trim() }),
      });
      toast.success("Perfil actualizado");
      setEditando(false);
      await mutarPerfil();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "No se pudo guardar");
    } finally {
      setGuardando(false);
    }
  };

  const subirFoto = async (file: File) => {
    if (file.size > 2 * 1024 * 1024) {
      toast.error("La imagen supera 2 MB");
      return;
    }
    setSubiendo(true);
    try {
      const fd = new FormData();
      fd.append("file", file);
      const res = await fetch(`${BASE}/api/usuarios/yo/avatar`, {
        method: "POST",
        headers: { Authorization: `Bearer ${getToken() ?? ""}` }, // sin Content-Type: lo pone el navegador
        body: fd,
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.detail ?? body.title ?? "No se pudo subir la foto");
      }
      toast.success("Foto actualizada");
      await mutarPerfil();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Error al subir la foto");
    } finally {
      setSubiendo(false);
    }
  };
  const { data: miRank } = useSWR<RankingEntry>("/api/usuarios/ranking/yo", (e: string) =>
    apiFetch<RankingEntry>(e),
  );
  const { data: stats } = useSWR<EstadisticasApuestas>("/api/apuestas/estadisticas", (e: string) =>
    apiFetch<EstadisticasApuestas>(e),
  );
  const { data: historial } = useSWR<HistorialApuesta[]>("/api/apuestas/historial", (e: string) =>
    apiFetch<HistorialApuesta[]>(e),
  );

  if (!perfil) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <div className="h-40 animate-pulse rounded-2xl border border-border bg-card" />
      </div>
    );
  }

  const racha = perfil.rachaActual;

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      {/* Cabecera */}
      <div className="flex flex-col items-center gap-4 rounded-2xl border border-border bg-card p-6 sm:flex-row sm:items-center">
        {/* Avatar (editable) */}
        <div className="relative h-16 w-16 shrink-0">
          {perfil.urlAvatar ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={perfil.urlAvatar}
              alt=""
              className="h-16 w-16 rounded-full object-cover"
            />
          ) : (
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/15 text-2xl font-bold text-primary">
              {perfil.nombreDisplay.charAt(0).toUpperCase()}
            </div>
          )}
          {editando && (
            <button
              type="button"
              onClick={() => fileRef.current?.click()}
              disabled={subiendo}
              aria-label="Cambiar foto"
              className="absolute inset-0 flex items-center justify-center rounded-full bg-black/55 text-foreground transition hover:bg-black/65"
            >
              {subiendo ? (
                <Loader2 className="h-5 w-5 animate-spin" />
              ) : (
                <Camera className="h-5 w-5" />
              )}
            </button>
          )}
          <input
            ref={fileRef}
            type="file"
            accept="image/png,image/jpeg,image/webp"
            className="hidden"
            onChange={(e) => {
              const f = e.target.files?.[0];
              if (f) void subirFoto(f);
              e.target.value = "";
            }}
          />
        </div>

        {/* Datos */}
        <div className="flex-1 text-center sm:text-left">
          {editando ? (
            <div className="flex flex-col items-center gap-2 sm:flex-row sm:items-center">
              <input
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                maxLength={100}
                className="rounded-md border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary/50"
              />
              <div className="flex gap-2">
                <button
                  onClick={guardarNombre}
                  disabled={guardando}
                  className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-2 text-sm font-semibold text-primary-foreground transition hover:opacity-90 disabled:opacity-60"
                >
                  {guardando && <Loader2 className="h-4 w-4 animate-spin" />} Guardar
                </button>
                <button
                  onClick={() => setEditando(false)}
                  className="rounded-md border border-border px-3 py-2 text-sm transition hover:bg-accent"
                >
                  Cancelar
                </button>
              </div>
            </div>
          ) : (
            <div className="flex items-center justify-center gap-2 sm:justify-start">
              <h1 className="text-xl font-bold">{perfil.nombreDisplay}</h1>
              <button
                onClick={abrirEdicion}
                aria-label="Editar perfil"
                className="rounded p-1 text-muted-foreground transition hover:bg-accent hover:text-foreground"
              >
                <Pencil className="h-4 w-4" />
              </button>
            </div>
          )}
          <p className="mt-1 text-sm text-muted-foreground">{perfil.correo}</p>
          {perfil.rol === "admin" && (
            <span className="mt-1 inline-block rounded-full bg-primary/15 px-2 py-0.5 text-xs text-primary">
              Administrador
            </span>
          )}
        </div>

        {/* Puntos */}
        <div className="sm:text-right">
          <p className="text-4xl font-extrabold tabular-nums text-primary">{perfil.puntosTotales}</p>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">puntos totales</p>
        </div>
      </div>

      {/* Posición y racha */}
      <div className="mt-4 grid grid-cols-2 gap-4">
        <div className="rounded-2xl border border-border bg-card p-5 text-center">
          <p className="text-2xl font-bold tabular-nums">
            {miRank && miRank.posicion > 0 ? `#${miRank.posicion}` : "—"}
          </p>
          <p className="mt-0.5 text-xs uppercase tracking-wide text-muted-foreground">Ranking global</p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 text-center">
          <p className="text-2xl font-bold tabular-nums">
            {racha >= 3 ? `🔥 ${racha}` : racha}
          </p>
          <p className="mt-0.5 text-xs uppercase tracking-wide text-muted-foreground">Racha actual</p>
        </div>
      </div>

      {/* Stats */}
      <h2 className="mb-3 mt-8 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
        Estadísticas
      </h2>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard label="Apuestas" valor={stats?.total ?? 0} />
        <StatCard label="Exactas" valor={stats?.exactas ?? 0} />
        <StatCard label="Ganadores" valor={stats?.ganadores ?? 0} />
        <StatCard label="Diferencias" valor={stats?.diferencias ?? 0} />
      </div>

      {/* Historial */}
      <h2 className="mb-3 mt-8 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
        Últimas apuestas
      </h2>
      <div className="overflow-hidden rounded-2xl border border-border bg-card">
        {historial && historial.length > 0 ? (
          historial.map((h) => (
            <Link
              key={h.id}
              href={`/partidos/${h.idPartido}`}
              className="flex items-center gap-3 border-b border-border px-4 py-3 text-sm transition last:border-0 hover:bg-accent"
            >
              <span className="flex-1 truncate">
                {h.localEmoji} {h.localNombre}{" "}
                <span className="text-muted-foreground">vs</span> {h.visitanteEmoji}{" "}
                {h.visitanteNombre}
              </span>
              <span className="tabular-nums text-muted-foreground">
                {h.golesLocal}-{h.golesVisitante}
                {h.golesRealLocal !== null && h.golesRealVisitante !== null && (
                  <span className="ml-1 text-xs text-muted-foreground">
                    (real {h.golesRealLocal}-{h.golesRealVisitante})
                  </span>
                )}
              </span>
              <span
                className={`w-12 text-right font-bold tabular-nums ${
                  h.puntos === null
                    ? "text-muted-foreground"
                    : h.puntos > 0
                      ? "text-emerald-400"
                      : "text-muted-foreground"
                }`}
              >
                {h.puntos === null ? "—" : `+${h.puntos}`}
              </span>
            </Link>
          ))
        ) : (
          <p className="p-6 text-center text-sm text-muted-foreground">Aún no has apostado.</p>
        )}
      </div>
    </div>
  );
}
