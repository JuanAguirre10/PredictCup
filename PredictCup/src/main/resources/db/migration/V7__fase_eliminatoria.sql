-- Soporte para el bracket de eliminatorias: los slots pueden crearse vacíos
-- (sin países ni fecha) y el admin los va completando.

ALTER TABLE partidos ALTER COLUMN id_pais_local     DROP NOT NULL;
ALTER TABLE partidos ALTER COLUMN id_pais_visitante DROP NOT NULL;
ALTER TABLE partidos ALTER COLUMN fecha_hora        DROP NOT NULL;
ALTER TABLE partidos ALTER COLUMN cierre_apuestas   DROP NOT NULL;

-- Orden del partido dentro de su ronda (para posicionarlo en el bracket).
ALTER TABLE partidos ADD COLUMN orden INTEGER;
