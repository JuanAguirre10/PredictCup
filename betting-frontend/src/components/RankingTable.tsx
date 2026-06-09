import type { RankingEntry } from "@/lib/types";

function medalla(posicion: number): string {
  if (posicion === 1) return "🥇";
  if (posicion === 2) return "🥈";
  if (posicion === 3) return "🥉";
  return String(posicion);
}

function Fila({ e, esYo }: { e: RankingEntry; esYo: boolean }) {
  return (
    <tr
      className={`border-t border-border transition ${
        esYo ? "bg-primary/15 ring-1 ring-inset ring-primary/30" : "hover:bg-accent"
      }`}
    >
      <td className="w-12 px-3 py-3 text-center text-lg font-semibold tabular-nums">
        {medalla(e.posicion)}
      </td>
      <td className="px-2 py-3">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/15 text-sm font-bold text-primary">
            {(e.nombre || "U").charAt(0).toUpperCase()}
          </div>
          <span className="truncate font-medium">
            {e.nombre}
            {esYo && <span className="ml-2 text-xs text-primary">(tú)</span>}
          </span>
        </div>
      </td>
      <td className="px-3 py-3 text-right font-bold tabular-nums">{e.puntos}</td>
      <td className="hidden px-3 py-3 text-center tabular-nums text-muted-foreground sm:table-cell">
        {e.racha >= 3 ? `🔥 ${e.racha}` : e.racha}
      </td>
      <td className="hidden px-3 py-3 text-center tabular-nums text-muted-foreground sm:table-cell">
        {e.apuestas}
      </td>
    </tr>
  );
}

export default function RankingTable({
  entries,
  miId,
}: {
  entries: RankingEntry[];
  miId?: string | null;
}) {
  return (
    <div className="overflow-hidden rounded-2xl border border-border bg-card">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-xs uppercase tracking-wide text-muted-foreground">
            <th className="px-3 py-2.5 text-center font-medium">#</th>
            <th className="px-2 py-2.5 text-left font-medium">Jugador</th>
            <th className="px-3 py-2.5 text-right font-medium">Puntos</th>
            <th className="hidden px-3 py-2.5 text-center font-medium sm:table-cell">Racha</th>
            <th className="hidden px-3 py-2.5 text-center font-medium sm:table-cell">Apuestas</th>
          </tr>
        </thead>
        <tbody>
          {entries.length === 0 ? (
            <tr>
              <td colSpan={5} className="px-3 py-8 text-center text-sm text-muted-foreground">
                Aún no hay puntuaciones.
              </td>
            </tr>
          ) : (
            entries.map((e) => (
              <Fila key={e.idUsuario} e={e} esYo={e.idUsuario === miId} />
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
