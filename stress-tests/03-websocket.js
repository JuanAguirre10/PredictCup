// 03 — WebSocket: 300 conexiones simultáneas a /ws/ranking/global durante 2 min.
// Verifica que el servidor sostiene las conexiones (no las cierra él solo) y que
// el handshake es rápido incluso con 300 sockets abiertos a la vez.
import ws from "k6/ws";
import { check } from "k6";
import { BASE } from "./helpers.js";

const WS_URL = BASE.replace(/^http/, "ws") + "/ws/ranking/global";
const DURACION_MS = 120000; // 2 minutos abierta

export const options = {
  vus: 300,
  // Margen extra sobre los 2 min para que cada VU complete su conexión.
  duration: "2m20s",
  thresholds: {
    ws_connecting: ["p(95)<1000"], // p95 del handshake < 1s
  },
};

export default function () {
  const res = ws.connect(WS_URL, {}, (socket) => {
    let cerramosNosotros = false;

    socket.on("open", () => {
      // Nosotros decidimos cuándo cerrar, tras mantenerla 2 min.
      socket.setTimeout(() => {
        cerramosNosotros = true;
        socket.close();
      }, DURACION_MS);
    });

    socket.on("message", () => {
      // Llega un push de ranking actualizado; no hace falta procesarlo aquí.
    });

    socket.on("close", () => {
      // Si se cerró sin que lo pidiéramos, el servidor la tiró: eso es un fallo.
      check(null, { "cerrada por timeout, no por el servidor": () => cerramosNosotros });
    });

    socket.on("error", (e) => {
      check(null, { "sin error de socket": () => e === undefined });
    });
  });

  check(res, { "handshake 101": (r) => r && r.status === 101 });
}
