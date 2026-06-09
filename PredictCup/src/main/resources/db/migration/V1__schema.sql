-- ============================================================
-- V1__schema.sql
-- Esquema base del Mundial Betting System (PostgreSQL 16)
-- Adaptado desde basededatosprototipo.sql (MySQL):
--   CHAR(36)                      -> UUID DEFAULT gen_random_uuid()
--   DATETIME                      -> TIMESTAMPTZ
--   TINYINT(1)                    -> BOOLEAN
--   TINYINT                       -> SMALLINT
--   ON UPDATE CURRENT_TIMESTAMP   -> trigger set_actualizado_en()
-- ============================================================

-- Trigger reutilizable para columnas actualizado_en
CREATE OR REPLACE FUNCTION set_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- a) paises
-- ============================================================
CREATE TABLE paises (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    codigo_fifa     VARCHAR(3)   NOT NULL,
    nombre          VARCHAR(100) NOT NULL,
    nombre_es       VARCHAR(100) NOT NULL,
    bandera_emoji   VARCHAR(10),
    bandera_url     VARCHAR(500),
    confederacion   VARCHAR(15)  NOT NULL,
    grupo           CHAR(1),
    eliminado       BOOLEAN      NOT NULL DEFAULT false,
    CONSTRAINT pk_paises PRIMARY KEY (id),
    CONSTRAINT uq_codigo_fifa UNIQUE (codigo_fifa)
);

-- ============================================================
-- b) estadios
-- ============================================================
CREATE TABLE estadios (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    nombre      VARCHAR(150) NOT NULL,
    ciudad      VARCHAR(100) NOT NULL,
    pais_sede   VARCHAR(100) NOT NULL,
    capacidad   INTEGER,
    imagen_url  VARCHAR(500),
    CONSTRAINT pk_estadios PRIMARY KEY (id)
);

-- ============================================================
-- c) partidos
-- ============================================================
CREATE TABLE partidos (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    id_externo          VARCHAR(50),
    id_pais_local       UUID         NOT NULL,
    id_pais_visitante   UUID         NOT NULL,
    id_estadio          UUID,
    fecha_hora          TIMESTAMPTZ  NOT NULL,
    cierre_apuestas     TIMESTAMPTZ  NOT NULL,
    fase                VARCHAR(20)  NOT NULL DEFAULT 'GRUPO',
    grupo               CHAR(1),
    jornada             SMALLINT,
    goles_local         SMALLINT,
    goles_visitante     SMALLINT,
    goles_local_et      SMALLINT,
    goles_visitante_et  SMALLINT,
    goles_local_pen     SMALLINT,
    goles_visitante_pen SMALLINT,
    estado              VARCHAR(15)  NOT NULL DEFAULT 'PROGRAMADO',
    creado_en           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_partidos PRIMARY KEY (id),
    CONSTRAINT uq_id_externo UNIQUE (id_externo),
    CONSTRAINT fk_partido_local
        FOREIGN KEY (id_pais_local)     REFERENCES paises(id)   ON UPDATE CASCADE,
    CONSTRAINT fk_partido_visitante
        FOREIGN KEY (id_pais_visitante) REFERENCES paises(id)   ON UPDATE CASCADE,
    CONSTRAINT fk_partido_estadio
        FOREIGN KEY (id_estadio)        REFERENCES estadios(id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TRIGGER trg_partidos_actualizado
    BEFORE UPDATE ON partidos
    FOR EACH ROW EXECUTE FUNCTION set_actualizado_en();

-- ============================================================
-- d) tabla_posiciones
-- ============================================================
CREATE TABLE tabla_posiciones (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    id_pais             UUID        NOT NULL,
    grupo               CHAR(1)     NOT NULL,
    posicion            SMALLINT    NOT NULL DEFAULT 0,
    partidos_jugados    SMALLINT    NOT NULL DEFAULT 0,
    partidos_ganados    SMALLINT    NOT NULL DEFAULT 0,
    partidos_empatados  SMALLINT    NOT NULL DEFAULT 0,
    partidos_perdidos   SMALLINT    NOT NULL DEFAULT 0,
    goles_favor         SMALLINT    NOT NULL DEFAULT 0,
    goles_contra        SMALLINT    NOT NULL DEFAULT 0,
    diferencia_goles    SMALLINT    NOT NULL DEFAULT 0,
    puntos              SMALLINT    NOT NULL DEFAULT 0,
    actualizado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_tabla_posiciones PRIMARY KEY (id),
    CONSTRAINT uq_pais_grupo UNIQUE (id_pais, grupo),
    CONSTRAINT fk_tp_pais
        FOREIGN KEY (id_pais) REFERENCES paises(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TRIGGER trg_tabla_posiciones_actualizado
    BEFORE UPDATE ON tabla_posiciones
    FOR EACH ROW EXECUTE FUNCTION set_actualizado_en();

-- ============================================================
-- e) usuarios
-- ============================================================
CREATE TABLE usuarios (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    google_sub      VARCHAR(255) NOT NULL,
    correo          VARCHAR(255) NOT NULL,
    nombre_display  VARCHAR(100) NOT NULL,
    url_avatar      VARCHAR(500),
    rol             VARCHAR(10)  NOT NULL DEFAULT 'usuario',
    puntos_totales  INTEGER      NOT NULL DEFAULT 0,
    racha_actual    INTEGER      NOT NULL DEFAULT 0,
    activo          BOOLEAN      NOT NULL DEFAULT true,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uq_google_sub UNIQUE (google_sub),
    CONSTRAINT uq_correo     UNIQUE (correo)
);

CREATE TRIGGER trg_usuarios_actualizado
    BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION set_actualizado_en();

-- ============================================================
-- f) apuestas
-- ============================================================
CREATE TABLE apuestas (
    id                      UUID         NOT NULL DEFAULT gen_random_uuid(),
    id_usuario              UUID         NOT NULL,
    id_partido              UUID         NOT NULL,
    goles_predichos_local   SMALLINT     NOT NULL,
    goles_predichos_visit   SMALLINT     NOT NULL,
    registrado_en           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    clave_idempotencia      VARCHAR(100) NOT NULL,
    es_anticipada           BOOLEAN      NOT NULL DEFAULT false,
    puntos_exacto           SMALLINT,
    puntos_ganador          SMALLINT,
    puntos_diferencia       SMALLINT,
    bonus_anticipada        SMALLINT,
    bonus_racha             SMALLINT,
    puntos_ganados_total    SMALLINT     NOT NULL DEFAULT 0,
    puntuado_en             TIMESTAMPTZ,
    creado_en               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_apuestas PRIMARY KEY (id),
    CONSTRAINT uq_usuario_partido UNIQUE (id_usuario, id_partido),
    CONSTRAINT uq_idempotencia    UNIQUE (clave_idempotencia),
    CONSTRAINT fk_apuesta_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_apuesta_partido
        FOREIGN KEY (id_partido) REFERENCES partidos(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- ============================================================
-- g) salas
-- ============================================================
CREATE TABLE salas (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    id_dueno        UUID         NOT NULL,
    nombre          VARCHAR(100) NOT NULL,
    codigo          CHAR(6)      NOT NULL,
    descripcion     VARCHAR(255),
    es_publica      BOOLEAN      NOT NULL DEFAULT false,
    max_miembros    SMALLINT     NOT NULL DEFAULT 100,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_salas PRIMARY KEY (id),
    CONSTRAINT uq_codigo UNIQUE (codigo),
    CONSTRAINT fk_sala_dueno
        FOREIGN KEY (id_dueno) REFERENCES usuarios(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TRIGGER trg_salas_actualizado
    BEFORE UPDATE ON salas
    FOR EACH ROW EXECUTE FUNCTION set_actualizado_en();

-- ============================================================
-- h) miembros_sala  (N:M usuarios <-> salas)
-- ============================================================
CREATE TABLE miembros_sala (
    id_sala     UUID        NOT NULL,
    id_usuario  UUID        NOT NULL,
    unido_en    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_miembros_sala PRIMARY KEY (id_sala, id_usuario),
    CONSTRAINT fk_ms_sala
        FOREIGN KEY (id_sala)    REFERENCES salas(id)    ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ms_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- ============================================================
-- i) cola_apuestas_log  (auditoria de la cola Redis)
-- ============================================================
CREATE TABLE cola_apuestas_log (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    clave_idempotencia  VARCHAR(100) NOT NULL,
    id_usuario          UUID         NOT NULL,
    id_partido          UUID         NOT NULL,
    goles_local         SMALLINT     NOT NULL,
    goles_visitante     SMALLINT     NOT NULL,
    encolado_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    procesado_en        TIMESTAMPTZ,
    tiempo_proceso_ms   INTEGER,
    estado              VARCHAR(15)  NOT NULL DEFAULT 'EN_COLA',
    mensaje_error       TEXT,
    CONSTRAINT pk_cola_apuestas_log PRIMARY KEY (id),
    CONSTRAINT uq_cola_idempotencia UNIQUE (clave_idempotencia),
    CONSTRAINT fk_cola_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cola_partido
        FOREIGN KEY (id_partido) REFERENCES partidos(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- ============================================================
-- j) historial_puntuacion
-- ============================================================
CREATE TABLE historial_puntuacion (
    id                    UUID        NOT NULL DEFAULT gen_random_uuid(),
    id_partido            UUID        NOT NULL,
    id_usuario            UUID        NOT NULL,
    id_apuesta            UUID        NOT NULL,
    puntos_base           SMALLINT    NOT NULL DEFAULT 0,
    puntos_bonus          SMALLINT    NOT NULL DEFAULT 0,
    puntos_total          SMALLINT    NOT NULL DEFAULT 0,
    tipo_resultado        VARCHAR(15) NOT NULL DEFAULT 'ninguno',
    tuvo_bonus_anticipada BOOLEAN     NOT NULL DEFAULT false,
    tuvo_bonus_racha      BOOLEAN     NOT NULL DEFAULT false,
    puntuado_en           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_historial_puntuacion PRIMARY KEY (id),
    CONSTRAINT uq_hp_apuesta UNIQUE (id_apuesta),
    CONSTRAINT fk_hp_partido
        FOREIGN KEY (id_partido) REFERENCES partidos(id) ON DELETE CASCADE,
    CONSTRAINT fk_hp_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_hp_apuesta
        FOREIGN KEY (id_apuesta) REFERENCES apuestas(id) ON DELETE CASCADE
);

-- ============================================================
-- k) notificaciones
-- ============================================================
CREATE TABLE notificaciones (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    id_usuario  UUID         NOT NULL,
    tipo        VARCHAR(30)  NOT NULL,
    titulo      VARCHAR(150) NOT NULL,
    cuerpo      VARCHAR(500) NOT NULL,
    id_partido  UUID,
    id_sala     UUID,
    leida       BOOLEAN      NOT NULL DEFAULT false,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_notificaciones PRIMARY KEY (id),
    CONSTRAINT fk_notif_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_partido
        FOREIGN KEY (id_partido) REFERENCES partidos(id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_sala
        FOREIGN KEY (id_sala)    REFERENCES salas(id)    ON DELETE CASCADE
);
