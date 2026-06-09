"use client";

import { useEffect, useState } from "react";
import { getUsuario, type UsuarioToken } from "@/lib/auth";

/** Lee el usuario del JWT tras montar (la cookie solo existe en el cliente). */
export function useUsuario(): { usuario: UsuarioToken | null; montado: boolean } {
  const [usuario, setUsuario] = useState<UsuarioToken | null>(null);
  const [montado, setMontado] = useState(false);
  // Lectura única tras montar de estado solo-cliente (cookie); imprescindible para evitar
  // un mismatch de hidratación (en el servidor no hay cookie).
  useEffect(() => {
    /* eslint-disable react-hooks/set-state-in-effect */
    setUsuario(getUsuario());
    setMontado(true);
    /* eslint-enable react-hooks/set-state-in-effect */
  }, []);
  return { usuario, montado };
}
