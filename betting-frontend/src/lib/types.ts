// Espejo de los DTOs del backend Quarkus (com.mundial.*.dto).

export interface PaisMini {
  id: string;
  codigoFifa: string;
  nombreEs: string;
  banderaEmoji: string;
}

export interface EstadioMini {
  id: string;
  nombre: string;
  ciudad: string;
  paisSede: string;
}

export interface ApuestaResponse {
  id: string;
  idPartido: string;
  golesLocal: number;
  golesVisitante: number;
  esAnticipada: boolean;
  puntosTotal: number | null;
  registradoEn: string;
  puntuadoEn: string | null;
}

export interface PartidoResponse {
  id: string;
  idExterno: string | null;
  paisLocal: PaisMini;
  paisVisitante: PaisMini;
  estadio: EstadioMini | null;
  fechaHora: string;
  cierreApuestas: string;
  fase: string;
  grupo: string | null;
  golesLocal: number | null;
  golesVisitante: number | null;
  estado: string;
  apuestasAbiertas: boolean;
  miApuesta: ApuestaResponse | null;
}

/** Partido del bracket: países y fecha pueden ser null (slot sin asignar todavía). */
export interface PartidoBracket {
  id: string;
  fase: string;
  paisLocal: PaisMini | null;
  paisVisitante: PaisMini | null;
  fechaHora: string | null;
  golesLocal: number | null;
  golesVisitante: number | null;
  estado: string;
  apuestasAbiertas: boolean;
  miApuesta: ApuestaResponse | null;
}

/** País completo (de GET /api/paises), para selectores. */
export interface PaisCompleto {
  id: string;
  codigoFifa: string;
  nombreEs: string;
  banderaEmoji: string;
  grupo: string | null;
}

export interface TablaPosicion {
  posicion: number;
  pais: PaisMini;
  partidosJugados: number;
  partidosGanados: number;
  partidosEmpatados: number;
  partidosPerdidos: number;
  diferenciaGoles: number;
  puntos: number;
}

export interface RankingEntry {
  posicion: number;
  idUsuario: string;
  nombre: string;
  urlAvatar: string | null;
  puntos: number;
  racha: number;
  apuestas: number;
}

export interface UsuarioPerfil {
  id: string;
  nombreDisplay: string;
  correo: string;
  urlAvatar: string | null;
  rol: string;
  puntosTotales: number;
  rachaActual: number;
  activo: boolean;
}

export interface SalaResumen {
  id: string;
  nombre: string;
  codigo: string;
  descripcion: string | null;
  esPublica: boolean;
  miembros: number;
  esDueno: boolean;
}

export interface Sala {
  id: string;
  nombre: string;
  codigo: string;
  descripcion: string | null;
  esPublica: boolean;
  maxMiembros: number;
  idDueno: string;
}

export interface EstadisticasApuestas {
  total: number;
  puntuadas: number;
  exactas: number;
  ganadores: number;
  diferencias: number;
  fallos: number;
  anticipadas: number;
}

export interface HistorialApuesta {
  id: string;
  idPartido: string;
  localNombre: string;
  localEmoji: string;
  localCodigoFifa: string;
  visitanteNombre: string;
  visitanteEmoji: string;
  visitanteCodigoFifa: string;
  golesLocal: number;
  golesVisitante: number;
  golesRealLocal: number | null;
  golesRealVisitante: number | null;
  estado: string;
  tipoResultado: "EXACTO" | "GANADOR" | "DIFERENCIA" | "NINGUNO" | null;
  bonusAnticipada: number | null;
  bonusRacha: number | null;
  puntos: number | null;
  puntuadoEn: string | null;
}

export interface Notificacion {
  id: string;
  tipo: string;
  titulo: string;
  cuerpo: string;
  idPartido: string | null;
  idSala: string | null;
  leida: boolean;
  creadoEn: string;
}

export interface ColaStats {
  pendientes: number;
  procesadosHoy: number;
  fallidos: number;
}

export interface TopApostador {
  idUsuario: string;
  nombre: string;
  urlAvatar: string | null;
  puntos: number;
  aciertos: number;
}

export interface PartidoVs {
  id: string;
  paisLocal: PaisMini;
  paisVisitante: PaisMini;
  estadio: EstadioMini | null;
  fechaHora: string;
  golesLocal: number | null;
  golesVisitante: number | null;
  estado: string;
  grupo: string | null;
  tablaPosicionesGrupo: TablaPosicion[];
}
