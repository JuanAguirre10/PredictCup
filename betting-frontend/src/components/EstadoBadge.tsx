interface InfoEstado {
  label: string;
  className: string;
  animado?: boolean;
}

function info(estado: string): InfoEstado {
  switch (estado) {
    case "EN_CURSO":
    case "EN_JUEGO":
      return {
        label: "En vivo",
        className: "bg-emerald-500/15 text-emerald-400 ring-emerald-500/30",
        animado: true,
      };
    case "TERMINADO":
      return {
        label: "Terminado",
        className: "bg-sky-500/15 text-sky-400 ring-sky-500/30",
      };
    case "SUSPENDIDO":
      return {
        label: "Suspendido",
        className: "bg-amber-500/15 text-amber-400 ring-amber-500/30",
      };
    default:
      return {
        label: "Programado",
        className: "bg-slate-500/15 text-muted-foreground ring-slate-500/30",
      };
  }
}

export default function EstadoBadge({ estado }: { estado: string }) {
  const { label, className, animado } = info(estado);
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${className}`}
    >
      <span className="relative flex h-1.5 w-1.5">
        {animado && (
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-current opacity-75" />
        )}
        <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-current" />
      </span>
      {label}
    </span>
  );
}
