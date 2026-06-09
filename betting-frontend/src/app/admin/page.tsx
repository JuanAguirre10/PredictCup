"use client";

import { useState } from "react";
import useSWR from "swr";
import { toast } from "sonner";
import { apiFetch, ApiError } from "@/lib/api";
import { hoyISO } from "@/lib/fecha";
import type {
  PartidoResponse,
  UsuarioPerfil,
  ColaStats,
  PartidoBracket,
  PaisCompleto,
} from "@/lib/types";
import { FASES } from "@/lib/fases";
import EstadoBadge from "@/components/EstadoBadge";
import ModalMarcador from "@/components/ModalMarcador";
import Bandera from "@/components/Bandera";

type Tab = "partidos" | "eliminatorias" | "usuarios" | "cola";

export default function AdminPage() {
  const [tab, setTab] = useState<Tab>("partidos");

  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <h1 className="mb-6 text-2xl font-bold tracking-tight">Administración</h1>

      <div className="mb-6 flex gap-1 rounded-xl border border-border bg-card p-1">
        {(
          [
            ["partidos", "Partidos"],
            ["eliminatorias", "Eliminatorias"],
            ["usuarios", "Usuarios"],
            ["cola", "Cola Redis"],
          ] as [Tab, string][]
        ).map(([k, label]) => (
          <button
            key={k}
            onClick={() => setTab(k)}
            className={`flex-1 rounded-lg px-3 py-2 text-sm font-medium transition ${
              tab === k ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-accent"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === "partidos" && <TabPartidos />}
      {tab === "eliminatorias" && <TabEliminatorias />}
      {tab === "usuarios" && <TabUsuarios />}
      {tab === "cola" && <TabCola />}
    </div>
  );
}

function TabPartidos() {
  const [fecha, setFecha] = useState(hoyISO());
  const { data, mutate } = useSWR<PartidoResponse[]>(
    `/api/partidos?fecha=${fecha}`,
    (e: string) => apiFetch<PartidoResponse[]>(e),
  );
  const [sel, setSel] = useState<PartidoResponse | null>(null);

  return (
    <div>
      <input
        type="date"
        value={fecha}
        onChange={(e) => setFecha(e.target.value)}
        className="mb-4 rounded-md border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary/50"
      />
      <div className="overflow-hidden rounded-2xl border border-border bg-card">
        {data && data.length > 0 ? (
          data.map((p) => (
            <div
              key={p.id}
              className="flex items-center gap-3 border-b border-border px-4 py-3 text-sm last:border-0"
            >
              <span className="flex flex-1 items-center gap-1.5 truncate">
                <Bandera codigo={p.paisLocal.codigoFifa} emoji={p.paisLocal.banderaEmoji} size={16} />
                {p.paisLocal.nombreEs}
                <span className="tabular-nums text-muted-foreground">
                  {p.golesLocal ?? "-"}:{p.golesVisitante ?? "-"}
                </span>
                {p.paisVisitante.nombreEs}
                <Bandera codigo={p.paisVisitante.codigoFifa} emoji={p.paisVisitante.banderaEmoji} size={16} />
              </span>
              <EstadoBadge estado={p.estado} />
              <button
                onClick={() => setSel(p)}
                className="rounded-md border border-border px-3 py-1.5 text-xs font-medium transition hover:bg-accent"
              >
                Marcador
              </button>
            </div>
          ))
        ) : (
          <p className="p-6 text-center text-sm text-muted-foreground">No hay partidos en esta fecha.</p>
        )}
      </div>
      {sel && (
        <ModalMarcador partido={sel} onGuardar={() => mutate()} onClose={() => setSel(null)} />
      )}
    </div>
  );
}

function TabEliminatorias() {
  const { data: bracket, mutate } = useSWR<PartidoBracket[]>(
    "/api/partidos/bracket",
    (e: string) => apiFetch<PartidoBracket[]>(e),
  );
  const { data: paises } = useSWR<PaisCompleto[]>(
    "/api/paises",
    (e: string) => apiFetch<PaisCompleto[]>(e),
  );
  const [generando, setGenerando] = useState(false);

  const generar = async () => {
    setGenerando(true);
    try {
      await apiFetch("/api/partidos/bracket/generar", { method: "POST" });
      toast.success("Bracket generado");
      void mutate();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Error");
    } finally {
      setGenerando(false);
    }
  };

  if (!bracket) {
    return <div className="h-40 animate-pulse rounded-2xl border border-border bg-card" />;
  }

  if (bracket.length === 0) {
    return (
      <div className="rounded-2xl border border-border bg-card p-8 text-center">
        <p className="text-sm text-muted-foreground">
          Aún no hay bracket. Genera los slots vacíos (16avos → final) y luego asigna países y fechas.
        </p>
        <button
          onClick={generar}
          disabled={generando}
          className="mt-4 rounded-md bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground transition hover:opacity-90 disabled:opacity-60"
        >
          {generando ? "Generando…" : "Generar bracket vacío"}
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <p className="text-xs text-muted-foreground">
        Asigna países y fecha (en UTC) a cada cruce. El resultado se edita desde la pestaña
        Partidos cuando llegue el día.
      </p>
      {FASES.map(({ key, label }) => {
        const slots = bracket.filter((p) => p.fase === key);
        if (slots.length === 0) return null;
        return (
          <section key={key}>
            <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
              {label}
            </h2>
            <div className="space-y-2">
              {slots.map((s) => (
                <FilaSlot key={s.id} slot={s} paises={paises ?? []} onSaved={() => mutate()} />
              ))}
            </div>
          </section>
        );
      })}
    </div>
  );
}

function FilaSlot({
  slot,
  paises,
  onSaved,
}: {
  slot: PartidoBracket;
  paises: PaisCompleto[];
  onSaved: () => void;
}) {
  const [local, setLocal] = useState(slot.paisLocal?.id ?? "");
  const [visit, setVisit] = useState(slot.paisVisitante?.id ?? "");
  const [fecha, setFecha] = useState(slot.fechaHora ? slot.fechaHora.slice(0, 16) : "");
  const [guardando, setGuardando] = useState(false);

  const guardar = async () => {
    setGuardando(true);
    try {
      await apiFetch(`/api/partidos/${slot.id}/asignar`, {
        method: "PATCH",
        body: JSON.stringify({
          idPaisLocal: local || null,
          idPaisVisitante: visit || null,
          fechaHora: fecha ? `${fecha}:00Z` : null,
        }),
      });
      toast.success("Slot actualizado");
      onSaved();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Error");
    } finally {
      setGuardando(false);
    }
  };

  const opciones = (
    <>
      <option value="">— Por definir —</option>
      {paises.map((p) => (
        <option key={p.id} value={p.id}>
          {p.nombreEs}
        </option>
      ))}
    </>
  );

  return (
    <div className="flex flex-col gap-2 rounded-xl border border-border bg-card p-3 sm:flex-row sm:items-center">
      <select
        value={local}
        onChange={(e) => setLocal(e.target.value)}
        className="min-w-0 flex-1 rounded-md border border-border bg-card px-2 py-1.5 text-sm outline-none focus:border-primary/50"
      >
        {opciones}
      </select>
      <span className="text-center text-xs text-muted-foreground">vs</span>
      <select
        value={visit}
        onChange={(e) => setVisit(e.target.value)}
        className="min-w-0 flex-1 rounded-md border border-border bg-card px-2 py-1.5 text-sm outline-none focus:border-primary/50"
      >
        {opciones}
      </select>
      <input
        type="datetime-local"
        value={fecha}
        onChange={(e) => setFecha(e.target.value)}
        className="rounded-md border border-border bg-card px-2 py-1.5 text-sm outline-none focus:border-primary/50"
      />
      <button
        onClick={guardar}
        disabled={guardando}
        className="rounded-md bg-primary px-4 py-1.5 text-sm font-semibold text-primary-foreground transition hover:opacity-90 disabled:opacity-60"
      >
        Guardar
      </button>
    </div>
  );
}

function TabUsuarios() {
  const [q, setQ] = useState("");
  const { data, mutate } = useSWR<UsuarioPerfil[]>(
    `/api/usuarios?buscar=${encodeURIComponent(q)}`,
    (e: string) => apiFetch<UsuarioPerfil[]>(e),
  );

  const cambiarRol = async (u: UsuarioPerfil) => {
    const nuevo = u.rol === "admin" ? "usuario" : "admin";
    try {
      await apiFetch(`/api/usuarios/${u.id}/rol`, {
        method: "PATCH",
        body: JSON.stringify({ rol: nuevo }),
      });
      toast.success(`Rol → ${nuevo}`);
      void mutate();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Error");
    }
  };

  const toggleActivo = async (u: UsuarioPerfil) => {
    try {
      await apiFetch(`/api/usuarios/${u.id}/activo`, {
        method: "PATCH",
        body: JSON.stringify({ activo: !u.activo }),
      });
      toast.success(u.activo ? "Usuario desactivado" : "Usuario activado");
      void mutate();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Error");
    }
  };

  return (
    <div>
      <input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Buscar por nombre o correo…"
        className="mb-4 w-full rounded-md border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary/50"
      />
      <div className="overflow-hidden rounded-2xl border border-border bg-card">
        {data && data.length > 0 ? (
          data.map((u) => (
            <div
              key={u.id}
              className="flex items-center gap-3 border-b border-border px-4 py-3 text-sm last:border-0"
            >
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium">
                  {u.nombreDisplay}
                  {!u.activo && <span className="ml-2 text-xs text-red-400">(inactivo)</span>}
                </p>
                <p className="truncate text-xs text-muted-foreground">{u.correo}</p>
              </div>
              <span
                className={`rounded-full px-2 py-0.5 text-xs ${
                  u.rol === "admin" ? "bg-primary/15 text-primary" : "bg-muted text-muted-foreground"
                }`}
              >
                {u.rol}
              </span>
              <button
                onClick={() => cambiarRol(u)}
                className="rounded-md border border-border px-2.5 py-1.5 text-xs transition hover:bg-accent"
              >
                {u.rol === "admin" ? "Quitar admin" : "Hacer admin"}
              </button>
              <button
                onClick={() => toggleActivo(u)}
                className={`rounded-md border px-2.5 py-1.5 text-xs transition hover:bg-accent ${
                  u.activo ? "border-red-500/30 text-red-400" : "border-emerald-500/30 text-emerald-400"
                }`}
              >
                {u.activo ? "Desactivar" : "Activar"}
              </button>
            </div>
          ))
        ) : (
          <p className="p-6 text-center text-sm text-muted-foreground">Sin usuarios.</p>
        )}
      </div>
    </div>
  );
}

function TabCola() {
  const { data } = useSWR<ColaStats>(
    "/api/admin/cola/stats",
    (e: string) => apiFetch<ColaStats>(e),
    { refreshInterval: 5000 },
  );

  const cards = [
    { label: "Pendientes en cola", valor: data?.pendientes ?? 0, color: "text-amber-400" },
    { label: "Procesados hoy", valor: data?.procesadosHoy ?? 0, color: "text-emerald-400" },
    { label: "Fallidos", valor: data?.fallidos ?? 0, color: "text-red-400" },
  ];

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      {cards.map((c) => (
        <div key={c.label} className="rounded-2xl border border-border bg-card p-6 text-center">
          <p className={`text-4xl font-extrabold tabular-nums ${c.color}`}>{c.valor}</p>
          <p className="mt-1 text-xs uppercase tracking-wide text-muted-foreground">{c.label}</p>
        </div>
      ))}
    </div>
  );
}
