-- ============================================================
-- V2__indexes.sql
-- Indices sobre las columnas realmente consultadas.
-- ============================================================

-- partidos: listados por fecha, filtrado por estado, gate de cierre
CREATE INDEX idx_partidos_fecha_hora      ON partidos (fecha_hora);
CREATE INDEX idx_partidos_estado          ON partidos (estado);
CREATE INDEX idx_partidos_cierre_apuestas ON partidos (cierre_apuestas);

-- tabla_posiciones: orden de la tabla por grupo (PJ G E P DG Pts)
CREATE INDEX idx_tabla_posiciones_grupo
    ON tabla_posiciones (grupo, puntos DESC, diferencia_goles DESC);

-- apuestas: busquedas por usuario y por partido
CREATE INDEX idx_apuestas_usuario ON apuestas (id_usuario);
CREATE INDEX idx_apuestas_partido ON apuestas (id_partido);

-- usuarios: ranking global
CREATE INDEX idx_usuarios_puntos_totales ON usuarios (puntos_totales DESC);

-- notificaciones: bandeja del usuario / no leidas
CREATE INDEX idx_notificaciones_usuario_leida ON notificaciones (id_usuario, leida);
