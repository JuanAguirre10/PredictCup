"use client";

import { useEffect, useState } from "react";
import { Moon, Sun } from "lucide-react";

/** Alterna entre tema claro y nocturno (clase .dark en <html>, persistido). */
export default function ThemeToggle() {
  const [dark, setDark] = useState(false);

  // Sincroniza el icono con la clase .dark del <html> (puesta por el script anti-flash)
  // tras montar; en el servidor no existe el DOM.
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setDark(document.documentElement.classList.contains("dark"));
  }, []);

  const toggle = () => {
    const next = !dark;
    setDark(next);
    document.documentElement.classList.toggle("dark", next);
    try {
      localStorage.setItem("tema", next ? "dark" : "light");
    } catch {
      /* sin localStorage, no persiste */
    }
  };

  return (
    <button
      onClick={toggle}
      aria-label="Cambiar tema"
      className="rounded-full p-2 text-muted-foreground transition hover:bg-accent hover:text-foreground"
    >
      {dark ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
    </button>
  );
}
