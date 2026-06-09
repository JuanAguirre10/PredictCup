"use client";

import { useState } from "react";

// Base de las imagenes de bandera. Por defecto una carpeta local servida en /flags;
// se puede apuntar a un CDN/R2 con NEXT_PUBLIC_FLAGS_BASE.
const FLAGS_BASE = process.env.NEXT_PUBLIC_FLAGS_BASE ?? "/flags";
// Extension de los archivos de bandera (jpg por defecto; cambiable con NEXT_PUBLIC_FLAGS_EXT).
const FLAGS_EXT = process.env.NEXT_PUBLIC_FLAGS_EXT ?? "jpg";

interface Props {
  /** Codigo FIFA (ej. "BRA"); la imagen se busca como {base}/{codigo en minuscula}.{ext} */
  codigo: string;
  /** Emoji de respaldo si la imagen no existe. */
  emoji?: string;
  /** URL explicita; si se pasa, tiene prioridad sobre la convencion. */
  url?: string | null;
  /** Alto en px (el ancho se calcula con relacion ~3:2). */
  size?: number;
}

/**
 * Muestra la bandera como imagen (por convencion de codigo FIFA o URL explicita) y,
 * si la imagen falla o falta, cae al emoji. Las banderas son fijas, asi que no hay subida.
 */
export default function Bandera({ codigo, emoji, url, size = 24 }: Props) {
  const [fallo, setFallo] = useState(false);

  if (fallo || !codigo) {
    return (
      <span style={{ fontSize: size, lineHeight: 1 }} aria-label={codigo}>
        {emoji ?? "🏳"}
      </span>
    );
  }

  const src = url || `${FLAGS_BASE}/${codigo.toLowerCase()}.${FLAGS_EXT}`;
  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={src}
      alt={codigo}
      onError={() => setFallo(true)}
      style={{ width: Math.round(size * 1.45), height: size, objectFit: "cover" }}
      className="inline-block shrink-0 rounded-sm"
    />
  );
}
