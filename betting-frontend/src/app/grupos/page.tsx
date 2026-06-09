import type { Metadata } from "next";
import Link from "next/link";
import TablaGrupo from "@/components/TablaGrupo";

const GRUPOS = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"];

export const metadata: Metadata = { title: "Grupos · PredictCup" };

export default function GruposPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="text-2xl font-bold tracking-tight">Fase de grupos</h1>
      <p className="mt-1 text-sm text-muted-foreground">
        Posiciones de los ocho grupos del Mundial 2026.
      </p>

      <div className="mt-8 grid grid-cols-1 gap-5 lg:grid-cols-2">
        {GRUPOS.map((g) => (
          <Link
            key={g}
            href={`/grupos/${g}`}
            className="block transition hover:-translate-y-0.5 hover:opacity-95"
          >
            <TablaGrupo grupo={g} />
          </Link>
        ))}
      </div>
    </div>
  );
}
