import { Target, Trophy, Scale, Flame, Clock } from "lucide-react";
import type { LucideIcon } from "lucide-react";

interface Regla {
  icon: LucideIcon;
  titulo: string;
  puntos: string;
  desc: string;
  ejemplo?: string;
}

const BASE: Regla[] = [
  {
    icon: Target,
    titulo: "Resultado exacto",
    puntos: "5",
    desc: "Aciertas el marcador exacto del partido.",
    ejemplo: "Predices 2-1 y termina 2-1.",
  },
  {
    icon: Trophy,
    titulo: "Ganador correcto",
    puntos: "3",
    desc: "Aciertas qué equipo gana (o el empate), aunque no el marcador exacto.",
    ejemplo: "Predices 1-0 y termina 3-1: gana el local en ambos.",
  },
  {
    icon: Scale,
    titulo: "Diferencia de goles",
    puntos: "2",
    desc: "Aciertas el margen de victoria aunque no el marcador exacto.",
    ejemplo: "Predices 3-1 y termina 2-0: ambos con diferencia de 2 a favor del mismo equipo.",
  },
];

const BONOS: Regla[] = [
  {
    icon: Flame,
    titulo: "Bonus por racha",
    puntos: "+2",
    desc: "Por cada 3 partidos consecutivos acertados (al menos el ganador), sumas 2 puntos extra como recompensa a la constancia.",
  },
  {
    icon: Clock,
    titulo: "Predicción anticipada",
    puntos: "+1",
    desc: "Si registras tu predicción con más de 24 horas de anticipación, ganas 1 punto extra. Las predicciones de último minuto solo reciben los puntos base.",
  },
];

function Tarjeta({ r, acento }: { r: Regla; acento: "base" | "bono" }) {
  return (
    <div className="rounded-3xl border border-border bg-card p-6 shadow-sm transition hover:shadow-lg">
      <div className="flex items-start justify-between gap-4">
        <div
          className={`inline-flex h-12 w-12 items-center justify-center rounded-2xl ${
            acento === "base" ? "bg-primary/12 text-primary" : "bg-amber-500/15 text-amber-500"
          }`}
        >
          <r.icon className="h-6 w-6" />
        </div>
        <span
          className={`text-3xl font-extrabold tabular-nums ${
            acento === "base" ? "text-primary" : "text-amber-500"
          }`}
        >
          {r.puntos}
          <span className="ml-1 text-sm font-semibold text-muted-foreground">pts</span>
        </span>
      </div>
      <h3 className="mt-4 text-lg font-bold">{r.titulo}</h3>
      <p className="mt-1 text-sm text-muted-foreground">{r.desc}</p>
      {r.ejemplo && (
        <p className="mt-3 rounded-xl bg-muted px-3 py-2 text-xs text-muted-foreground">
          <span className="font-semibold text-foreground">Ejemplo: </span>
          {r.ejemplo}
        </p>
      )}
    </div>
  );
}

export default function ReglasPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      <h1 className="text-3xl font-extrabold tracking-tight sm:text-4xl">Reglas de puntuación</h1>
      <p className="mt-3 max-w-2xl text-muted-foreground">
        Por cada partido se aplica <strong className="text-foreground">una sola</strong> categoría de
        acierto (la que corresponda según tu predicción). Encima, puedes sumar los bonos.
      </p>

      <h2 className="mb-4 mt-10 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
        Puntos por acierto
      </h2>
      <div className="grid gap-5 sm:grid-cols-3">
        {BASE.map((r) => (
          <Tarjeta key={r.titulo} r={r} acento="base" />
        ))}
      </div>

      <h2 className="mb-4 mt-10 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
        Bonos extra
      </h2>
      <div className="grid gap-5 sm:grid-cols-2">
        {BONOS.map((r) => (
          <Tarjeta key={r.titulo} r={r} acento="bono" />
        ))}
      </div>

      <div className="mt-10 rounded-2xl border border-primary/30 bg-primary/5 p-5 text-sm text-muted-foreground">
        <span className="font-semibold text-foreground">¿Cómo se suma?</span> Tus puntos de cada
        predicción = puntos por acierto (5, 3, 2 o 0) + bonos aplicables (racha y/o anticipada).
        Tu total es la suma de todas tus predicciones, y es lo que te posiciona en el ranking.
      </div>
    </div>
  );
}
