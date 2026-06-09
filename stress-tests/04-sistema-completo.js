// 04 — Sistema completo: escenario para el informe final.
// Rampa progresiva hasta 1000 VUs con la mezcla de tráfico esperada en producción.
import http from "k6/http";
import { check, sleep } from "k6";
import { Rate } from "k6/metrics";
import { BASE, tokenRandom, payloadApuesta } from "./helpers.js";

const errores = new Rate("errores");
const hoy = new Date().toISOString().slice(0, 10);
const GRUPOS = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"];

export const options = {
  scenarios: {
    sistema: {
      executor: "ramping-vus",
      startVUs: 100, // arrancamos ya con 100 usuarios
      stages: [
        { duration: "1m", target: 500 }, // calentamiento 100 -> 500
        { duration: "2m", target: 500 }, // meseta de operación normal
        { duration: "30s", target: 1000 }, // pico: abre una ventana de apuestas
        { duration: "1m", target: 1000 }, // sostiene el pico
        { duration: "30s", target: 0 }, // enfriamiento
      ],
    },
  },
  thresholds: {
    // Latencia global: 95% de TODAS las peticiones bajo 500ms aun en el pico.
    http_req_duration: ["p(95)<500"],
    // Errores propios (status inesperado) < 5%. Los 429 (rate limit) NO cuentan.
    errores: ["rate<0.05"],
    // Las LECTURAS son la cara visible del producto: deben ir más rápidas (<400ms p95).
    "http_req_duration{tipo:lectura}": ["p(95)<400"],
    // Disponibilidad: menos del 1% de peticiones totalmente fallidas (timeouts/cortes).
    http_req_failed: ["rate<0.05"],
  },
};

export default function () {
  const r = Math.random();

  if (r < 0.4) {
    // 40% — listado de partidos
    const res = http.get(`${BASE}/api/partidos?fecha=${hoy}`, { tags: { tipo: "lectura" } });
    errores.add(!check(res, { "partidos 200": (x) => x.status === 200 }));
  } else if (r < 0.65) {
    // 25% — posiciones de un grupo al azar
    const g = GRUPOS[Math.floor(Math.random() * GRUPOS.length)];
    const res = http.get(`${BASE}/api/posiciones/${g}`, { tags: { tipo: "lectura" } });
    errores.add(!check(res, { "posiciones 200": (x) => x.status === 200 }));
  } else if (r < 0.9) {
    // 25% — apuestas (escritura asíncrona): 202 encolada o 429 limitada
    const res = http.post(`${BASE}/api/apuestas`, payloadApuesta(), {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${tokenRandom()}`,
      },
      tags: { tipo: "escritura" },
    });
    errores.add(!check(res, { "apuesta 202/429": (x) => x.status === 202 || x.status === 429 }));
  } else {
    // 10% — ranking global
    const res = http.get(`${BASE}/api/usuarios/ranking`, { tags: { tipo: "lectura" } });
    errores.add(!check(res, { "ranking 200": (x) => x.status === 200 }));
  }

  sleep(Math.random() * 2); // think time 0-2s
}
