// 02 — Pico de apuestas: simula la avalancha al abrir la ventana de un partido.
// Solo POST /api/apuestas. Lo crítico no es la latencia sino NO caer (sin 500):
// el sistema debe aceptar (202) o limitar (429), nunca romperse.
import http from "k6/http";
import { check } from "k6";
import { BASE, tokenRandom, payloadApuesta } from "./helpers.js";

export const options = {
  stages: [
    { duration: "30s", target: 1000 }, // rampa rápida 0 -> 1000 VUs
    { duration: "1m", target: 1000 }, // sostiene 1000 VUs
    { duration: "15s", target: 0 }, // baja a 0
  ],
  thresholds: {
    // INVARIANTE: ni un solo 500. Si aparece uno, el escenario falla.
    "checks{check:no hay 500}": ["rate==1.0"],
    // Bajo sobrecarga toleramos latencia alta, pero acotada.
    http_req_duration: ["p(95)<2000"],
  },
};

export default function () {
  const res = http.post(`${BASE}/api/apuestas`, payloadApuesta(), {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${tokenRandom()}`,
    },
  });

  check(res, {
    "no hay 500": (r) => r.status !== 500,
    "acepta o limita": (r) => r.status === 202 || r.status === 429,
  });
}
