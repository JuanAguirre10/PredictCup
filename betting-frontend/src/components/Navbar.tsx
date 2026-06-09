"use client";

import { useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { Menu, X } from "lucide-react";
import { BASE } from "@/lib/api";
import { useUsuario } from "@/hooks/useUsuario";
import CampanillaNotificaciones from "./CampanillaNotificaciones";
import ThemeToggle from "./ThemeToggle";
import UserMenu from "./UserMenu";

// Navegación pública (siempre visible).
const LINKS = [
  { href: "/partidos", label: "Partidos" },
  { href: "/grupos", label: "Grupos" },
  { href: "/eliminatorias", label: "Eliminatorias" },
  { href: "/ranking", label: "Ranking" },
  { href: "/reglas", label: "Reglas" },
];

// Enlaces personales (solo con sesión) — para el menú móvil.
const PERSONALES = [
  { href: "/predicciones", label: "Mis predicciones" },
  { href: "/salas", label: "Mis salas" },
  { href: "/perfil", label: "Mi perfil" },
];

export default function Navbar() {
  const { usuario, montado } = useUsuario();
  const [menu, setMenu] = useState(false);

  const personales =
    usuario?.rol === "admin" ? [...PERSONALES, { href: "/admin", label: "Admin" }] : PERSONALES;

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-background/80 backdrop-blur">
      <nav className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <Link href="/" className="flex items-center">
          <Image
            src="/brand/logo-horizontal.png"
            alt="PredictCup"
            width={158}
            height={40}
            priority
            className="h-9 w-auto dark:brightness-0 dark:invert"
          />
        </Link>

        <div className="hidden items-center gap-1 md:flex">
          {LINKS.map((l) => (
            <Link
              key={l.href}
              href={l.href}
              className="rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition hover:bg-accent hover:text-foreground"
            >
              {l.label}
            </Link>
          ))}
        </div>

        <div className="flex items-center gap-2">
          <ThemeToggle />
          {montado && usuario ? (
            <>
              <CampanillaNotificaciones />
              <UserMenu usuario={usuario} />
            </>
          ) : (
            <a
              href={`${BASE}/auth/callback`}
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition hover:opacity-90"
            >
              Entrar
            </a>
          )}
          <button
            onClick={() => setMenu((v) => !v)}
            aria-label="Menú"
            className="rounded-md p-2 text-muted-foreground transition hover:bg-accent md:hidden"
          >
            {menu ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </nav>

      {/* Menú móvil */}
      {menu && (
        <div className="border-t border-border bg-background/95 px-4 py-3 md:hidden">
          {LINKS.map((l) => (
            <Link
              key={l.href}
              href={l.href}
              onClick={() => setMenu(false)}
              className="block rounded-md px-3 py-2.5 text-sm font-medium text-muted-foreground transition hover:bg-accent hover:text-foreground"
            >
              {l.label}
            </Link>
          ))}
          {montado &&
            usuario &&
            personales.map((l) => (
              <Link
                key={l.href}
                href={l.href}
                onClick={() => setMenu(false)}
                className="block rounded-md px-3 py-2.5 text-sm font-medium text-muted-foreground transition hover:bg-accent hover:text-foreground"
              >
                {l.label}
              </Link>
            ))}
        </div>
      )}
    </header>
  );
}
