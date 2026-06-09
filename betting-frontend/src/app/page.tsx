import Image from "next/image";
import Link from "next/link";
import { Target, Users, TrendingUp, ArrowRight, Radio } from "lucide-react";

const features = [
  {
    icon: Target,
    titulo: "Predice",
    texto: "Acierta el marcador exacto y suma hasta 5 puntos por partido.",
  },
  {
    icon: Users,
    titulo: "Compite",
    texto: "Crea salas privadas con un código e invita a tus amigos.",
  },
  {
    icon: TrendingUp,
    titulo: "Gana",
    texto: "Sube en el ranking en tiempo real con cada gol.",
  },
];

export default function Home() {
  return (
    <div className="relative overflow-hidden">
      {/* Atmósfera: blobs suaves verde/azul */}
      <div className="pointer-events-none absolute -left-32 -top-24 -z-10 h-96 w-96 rounded-full bg-primary/25 blur-3xl" />
      <div className="pointer-events-none absolute -right-24 top-32 -z-10 h-80 w-80 rounded-full bg-accent/40 blur-3xl" />

      <section className="mx-auto flex max-w-5xl flex-col items-center px-4 pb-12 pt-16 text-center sm:pt-24">
        <span className="mb-8 inline-flex items-center gap-2 rounded-full border border-border bg-card/70 px-4 py-1.5 text-xs font-medium text-muted-foreground backdrop-blur">
          <Radio className="h-3.5 w-3.5 text-primary" />
          Mundial 2026 · en vivo
        </span>

        <Image
          src="/brand/logo-full.png"
          alt="PredictCup"
          width={1012}
          height={264}
          priority
          className="mb-10 h-auto w-[20rem] sm:w-[42rem] dark:brightness-0 dark:invert"
        />

        <h1 className="max-w-3xl text-balance text-4xl font-extrabold leading-[1.05] tracking-tight sm:text-6xl">
          Predice los marcadores del{" "}
          <span className="bg-gradient-to-r from-primary to-sky-500 bg-clip-text text-transparent">
            Mundial 2026
          </span>
        </h1>
        <p className="mt-5 max-w-xl text-pretty text-base text-muted-foreground sm:text-lg">
          Predice, compite en salas y sube en el ranking en vivo. Gana puntos con
          cada acierto y corónate como el mejor pronosticador del Mundial.
        </p>

        <div className="mt-9 flex flex-wrap items-center justify-center gap-3">
          <Link
            href="/partidos"
            className="group inline-flex items-center gap-2 rounded-full bg-primary px-7 py-3.5 font-semibold text-primary-foreground shadow-lg shadow-primary/25 transition hover:shadow-primary/40 hover:brightness-105"
          >
            Ver partidos de hoy
            <ArrowRight className="h-4 w-4 transition group-hover:translate-x-0.5" />
          </Link>
          <Link
            href="/ranking"
            className="rounded-full border border-border bg-card/60 px-7 py-3.5 font-semibold backdrop-blur transition hover:bg-accent"
          >
            Ranking global
          </Link>
        </div>
      </section>

      <section className="mx-auto grid max-w-5xl grid-cols-1 gap-5 px-4 pb-24 sm:grid-cols-3">
        {features.map((f) => (
          <div
            key={f.titulo}
            className="group rounded-3xl border border-border bg-card p-6 shadow-sm transition hover:-translate-y-1 hover:shadow-xl hover:shadow-primary/10"
          >
            <div className="mb-4 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/12 text-primary transition group-hover:scale-110">
              <f.icon className="h-6 w-6" />
            </div>
            <h3 className="text-lg font-bold">{f.titulo}</h3>
            <p className="mt-1.5 text-sm text-muted-foreground">{f.texto}</p>
          </div>
        ))}
      </section>
    </div>
  );
}
