-- ============================================================
-- V4__widen_smallint_to_integer.sql
-- Las entidades JPA mapean estos campos como int/Integer (INTEGER), pero el
-- esquema original los declaro SMALLINT. Hibernate (validate) fallaba en el
-- arranque por el desajuste de tipo. Se ensanchan a INTEGER para alinear el
-- esquema con el modelo. Los valores (goles, puntos, posiciones) caben de sobra.
-- ============================================================

-- partidos
ALTER TABLE partidos
    ALTER COLUMN jornada             TYPE INTEGER,
    ALTER COLUMN goles_local         TYPE INTEGER,
    ALTER COLUMN goles_visitante     TYPE INTEGER,
    ALTER COLUMN goles_local_et      TYPE INTEGER,
    ALTER COLUMN goles_visitante_et  TYPE INTEGER,
    ALTER COLUMN goles_local_pen     TYPE INTEGER,
    ALTER COLUMN goles_visitante_pen TYPE INTEGER;

-- tabla_posiciones
ALTER TABLE tabla_posiciones
    ALTER COLUMN posicion           TYPE INTEGER,
    ALTER COLUMN partidos_jugados   TYPE INTEGER,
    ALTER COLUMN partidos_ganados   TYPE INTEGER,
    ALTER COLUMN partidos_empatados TYPE INTEGER,
    ALTER COLUMN partidos_perdidos  TYPE INTEGER,
    ALTER COLUMN goles_favor        TYPE INTEGER,
    ALTER COLUMN goles_contra       TYPE INTEGER,
    ALTER COLUMN diferencia_goles   TYPE INTEGER,
    ALTER COLUMN puntos             TYPE INTEGER;

-- apuestas
ALTER TABLE apuestas
    ALTER COLUMN goles_predichos_local TYPE INTEGER,
    ALTER COLUMN goles_predichos_visit TYPE INTEGER,
    ALTER COLUMN puntos_exacto          TYPE INTEGER,
    ALTER COLUMN puntos_ganador         TYPE INTEGER,
    ALTER COLUMN puntos_diferencia      TYPE INTEGER,
    ALTER COLUMN bonus_anticipada       TYPE INTEGER,
    ALTER COLUMN bonus_racha            TYPE INTEGER,
    ALTER COLUMN puntos_ganados_total   TYPE INTEGER;

-- salas
ALTER TABLE salas
    ALTER COLUMN max_miembros TYPE INTEGER;

-- cola_apuestas_log
ALTER TABLE cola_apuestas_log
    ALTER COLUMN goles_local     TYPE INTEGER,
    ALTER COLUMN goles_visitante TYPE INTEGER;

-- historial_puntuacion
ALTER TABLE historial_puntuacion
    ALTER COLUMN puntos_base  TYPE INTEGER,
    ALTER COLUMN puntos_bonus TYPE INTEGER,
    ALTER COLUMN puntos_total TYPE INTEGER;
