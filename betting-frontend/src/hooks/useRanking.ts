"use client";

import { useEffect } from "react";
import useSWR from "swr";
import { apiFetch } from "@/lib/api";
import type { RankingEntry } from "@/lib/types";
import { useWebSocket } from "./useWebSocket";

function endpointDeScope(scope: string): string {
  if (scope.startsWith("sala:")) return `/api/salas/${scope.slice(5)}/ranking`;
  return "/api/usuarios/ranking"; // "global"
}

/**
 * Ranking en vivo: carga inicial con SWR y revalida (mutate) cada vez que el
 * WebSocket del mismo scope avisa que el ranking cambió.
 */
export function useRanking(scope: string) {
  const endpoint = endpointDeScope(scope);
  const { data, error, isLoading, mutate } = useSWR<RankingEntry[]>(
    endpoint,
    (e: string) => apiFetch<RankingEntry[]>(e),
  );
  const { conectado, ultimoMensaje } = useWebSocket(scope);

  useEffect(() => {
    if (ultimoMensaje !== null) {
      void mutate();
    }
  }, [ultimoMensaje, mutate]);

  return { ranking: data ?? [], error, isLoading, conectado };
}
