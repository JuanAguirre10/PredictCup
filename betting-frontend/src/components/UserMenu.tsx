"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import Link from "next/link";
import useSWR from "swr";
import { ChevronDown, User, ListChecks, Users, Bell, Shield, LogOut } from "lucide-react";
import { apiFetch } from "@/lib/api";
import { clearToken, type UsuarioToken } from "@/lib/auth";

interface Perfil {
  nombreDisplay: string;
  correo: string;
  urlAvatar?: string | null;
  puntosTotales: number;
}

function ItemMenu({
  href,
  icon,
  onClick,
  children,
}: {
  href: string;
  icon: ReactNode;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <Link
      href={href}
      onClick={onClick}
      className="flex items-center gap-2.5 px-4 py-2.5 text-sm transition hover:bg-accent"
    >
      <span className="text-muted-foreground">{icon}</span>
      {children}
    </Link>
  );
}

export default function UserMenu({ usuario }: { usuario: UsuarioToken }) {
  const [abierto, setAbierto] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const { data: perfil } = useSWR<Perfil>("/api/usuarios/yo", (e: string) => apiFetch<Perfil>(e));

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setAbierto(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);

  const logout = () => {
    clearToken();
    window.location.href = "/";
  };

  const inicial = (usuario.nombre || "U").charAt(0).toUpperCase();
  const cerrar = () => setAbierto(false);

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setAbierto((v) => !v)}
        className="flex items-center gap-2 rounded-full p-1 pr-2 transition hover:bg-accent"
        aria-label="Menú de usuario"
      >
        {perfil?.urlAvatar ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={perfil.urlAvatar} alt="" className="h-8 w-8 rounded-full object-cover" />
        ) : (
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/20 text-sm font-bold text-primary">
            {inicial}
          </div>
        )}
        <div className="hidden text-left leading-tight sm:block">
          <p className="text-sm font-medium">{usuario.nombre}</p>
          <p className="text-xs text-primary">{perfil?.puntosTotales ?? 0} pts</p>
        </div>
        <ChevronDown className="h-4 w-4 text-muted-foreground" />
      </button>

      {abierto && (
        <div className="absolute right-0 mt-2 w-60 overflow-hidden rounded-xl border border-border bg-popover shadow-xl">
          <div className="border-b border-border px-4 py-3">
            <p className="truncate text-sm font-semibold">{usuario.nombre}</p>
            <p className="truncate text-xs text-muted-foreground">{usuario.correo}</p>
          </div>
          <ItemMenu href="/perfil" icon={<User className="h-4 w-4" />} onClick={cerrar}>
            Mi perfil
          </ItemMenu>
          <ItemMenu href="/predicciones" icon={<ListChecks className="h-4 w-4" />} onClick={cerrar}>
            Mis predicciones
          </ItemMenu>
          <ItemMenu href="/salas" icon={<Users className="h-4 w-4" />} onClick={cerrar}>
            Mis salas
          </ItemMenu>
          <ItemMenu href="/notificaciones" icon={<Bell className="h-4 w-4" />} onClick={cerrar}>
            Notificaciones
          </ItemMenu>
          {usuario.rol === "admin" && (
            <ItemMenu href="/admin" icon={<Shield className="h-4 w-4" />} onClick={cerrar}>
              Admin
            </ItemMenu>
          )}
          <button
            onClick={logout}
            className="flex w-full items-center gap-2.5 border-t border-border px-4 py-2.5 text-sm text-destructive transition hover:bg-accent"
          >
            <LogOut className="h-4 w-4" /> Cerrar sesión
          </button>
        </div>
      )}
    </div>
  );
}
