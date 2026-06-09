"use client";

import { useEffect, useRef, useState } from "react";
import { BASE } from "@/lib/api";

function urlWs(scope: string): string {
  const base = BASE.replace(/^http/, "ws"); // http->ws, https->wss
  return `${base}/ws/ranking/${scope}`;
}

export interface EstadoWebSocket {
  conectado: boolean;
  ultimoMensaje: string | null;
}

/**
 * Conecta a /ws/ranking/{scope} con reconexion automatica (backoff exponencial,
 * tope 30s). Pasar scope = null desconecta. Retorna estado y ultimo mensaje recibido.
 */
export function useWebSocket(scope: string | null): EstadoWebSocket {
  const [conectado, setConectado] = useState(false);
  const [ultimoMensaje, setUltimoMensaje] = useState<string | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const intentosRef = useRef(0);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const desmontadoRef = useRef(false);

  useEffect(() => {
    if (!scope) return;
    desmontadoRef.current = false;

    const conectar = () => {
      const ws = new WebSocket(urlWs(scope));
      wsRef.current = ws;

      ws.onopen = () => {
        setConectado(true);
        intentosRef.current = 0;
      };
      ws.onmessage = (e) => {
        setUltimoMensaje(typeof e.data === "string" ? e.data : String(e.data));
      };
      ws.onclose = () => {
        setConectado(false);
        if (desmontadoRef.current) return;
        const intento = Math.min(intentosRef.current++, 6);
        const espera = Math.min(1000 * 2 ** intento, 30000); // 1s,2s,4s...30s
        timerRef.current = setTimeout(conectar, espera);
      };
      ws.onerror = () => ws.close();
    };

    conectar();

    return () => {
      desmontadoRef.current = true;
      if (timerRef.current) clearTimeout(timerRef.current);
      wsRef.current?.close();
    };
  }, [scope]);

  return { conectado, ultimoMensaje };
}
