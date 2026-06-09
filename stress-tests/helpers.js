// Utilidades compartidas por los escenarios de k6.
// Configurable por variables de entorno (--env o $ENV):
//   BASE_URL    URL del backend (default http://localhost:8080)
//   TOKENS      JSON array de JWT validos, p.ej. '["eyJ...","eyJ..."]'
//   ID_PARTIDO  UUID de un partido con apuestas abiertas (para POST /api/apuestas)

export const BASE = __ENV.BASE_URL || "http://localhost:8080";
export const TOKENS = JSON.parse(__ENV.TOKENS || '["demo"]');
export const ID_PARTIDO = __ENV.ID_PARTIDO || "test-id";

export function tokenRandom() {
  return TOKENS[Math.floor(Math.random() * TOKENS.length)];
}

export function payloadApuesta() {
  return JSON.stringify({
    idPartido: ID_PARTIDO,
    golesLocal: Math.floor(Math.random() * 4),
    golesVisitante: Math.floor(Math.random() * 4),
    claveIdempotencia: `${__VU}-${__ITER}-${Date.now()}`,
  });
}
