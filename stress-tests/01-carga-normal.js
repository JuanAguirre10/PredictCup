// 01 — Carga normal: 500 VUs durante 2 minutos.
// Mezcla realista de lecturas (90%) y escrituras (10%).
import http from "k6/http";
import { check, sleep } from "k6";
import { Rate } from "k6/metrics";
import { BASE, tokenRandom, payloadApuesta } from "./helpers.js";

// Tasa de errores "reales": un status inesperado. 429 (rate limit) NO es error.
const errores = new Rate("errores");

export const options = {
  vus: 500,
  duration: "2m",
  thresholds: {
    http_req_duration: ["p(95)<200"], // p95 de latencia < 200ms
    errores: ["rate<0.02"], // menos del 2% de respuestas inesperadas
  },
};

const hoy = new Date().toISOString().slice(0, 10);

export default function () {
  const r = Math.random();

  if (r < 0.5) {
    // 50% — partidos de hoy
    const res = http.get(`${BASE}/api/partidos?fecha=${hoy}`);
    errores.add(!check(res, { "partidos 200": (x) => x.status === 200 }));
  } else if (r < 0.7) {
    // 20% — posiciones del grupo C
    const res = http.get(`${BASE}/api/posiciones/C`);
    errores.add(!check(res, { "posiciones 200": (x) => x.status === 200 }));
  } else if (r < 0.9) {
    // 20% — ranking global
    const res = http.get(`${BASE}/api/usuarios/ranking`);
    errores.add(!check(res, { "ranking 200": (x) => x.status === 200 }));
  } else {
    // 10% — apuesta (autenticada). 202 = encolada, 429 = rate limit (ambos OK).
    const res = http.post(`${BASE}/api/apuestas`, payloadApuesta(), {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${tokenRandom()}`,
      },
    });
    errores.add(!check(res, { "apuesta 202/429": (x) => x.status === 202 || x.status === 429 }));
  }

  sleep(1);
}
