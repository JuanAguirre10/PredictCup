-- ============================================================
-- V5__char_to_varchar.sql
-- Las columnas de codigo/grupo se declararon CHAR(n) (bpchar) pero las entidades
-- las mapean como String -> VARCHAR. Se convierten a VARCHAR para que la
-- validacion de Hibernate pase limpia. Los valores existentes no cambian.
-- ============================================================

ALTER TABLE paises           ALTER COLUMN grupo  TYPE VARCHAR(1);
ALTER TABLE partidos         ALTER COLUMN grupo  TYPE VARCHAR(1);
ALTER TABLE tabla_posiciones ALTER COLUMN grupo  TYPE VARCHAR(1);
ALTER TABLE salas            ALTER COLUMN codigo TYPE VARCHAR(6);
