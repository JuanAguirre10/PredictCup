-- ============================================================
-- V6__grupos_mundial_2026.sql
-- Sorteo oficial del Mundial 2026: 48 selecciones en 12 grupos (A-L).
-- Reemplaza el seed previo (V3, 32 equipos A-H). Se limpian partidos y la
-- tabla de posiciones primero por las claves foraneas; los estadios se mantienen.
-- NOTA: borrar partidos arrastra (ON DELETE CASCADE) apuestas/historial/cola/
-- notificaciones. Es un reseteo de datos previo a produccion.
-- ============================================================

DELETE FROM partidos;
DELETE FROM tabla_posiciones;
DELETE FROM paises;

-- ------------------------------------------------------------
-- 48 paises del Mundial 2026 (4 por grupo, A-L)
-- ------------------------------------------------------------
INSERT INTO paises (codigo_fifa, nombre, nombre_es, bandera_emoji, confederacion, grupo) VALUES
-- Grupo A
('MEX','Mexico',                 'Mexico',               '🇲🇽','CONCACAF','A'),
('RSA','South Africa',           'Sudafrica',            '🇿🇦','CAF',     'A'),
('KOR','South Korea',            'Corea del Sur',        '🇰🇷','AFC',     'A'),
('CZE','Czechia',                'Chequia',              '🇨🇿','UEFA',    'A'),
-- Grupo B
('CAN','Canada',                 'Canada',               '🇨🇦','CONCACAF','B'),
('BIH','Bosnia and Herzegovina', 'Bosnia y Herzegovina', '🇧🇦','UEFA',    'B'),
('QAT','Qatar',                  'Catar',                '🇶🇦','AFC',     'B'),
('SUI','Switzerland',            'Suiza',                '🇨🇭','UEFA',    'B'),
-- Grupo C
('BRA','Brazil',                 'Brasil',               '🇧🇷','CONMEBOL','C'),
('MAR','Morocco',                'Marruecos',            '🇲🇦','CAF',     'C'),
('HAI','Haiti',                  'Haiti',                '🇭🇹','CONCACAF','C'),
('SCO','Scotland',               'Escocia',              '🏴','UEFA',    'C'),
-- Grupo D
('USA','United States',          'Estados Unidos',       '🇺🇸','CONCACAF','D'),
('PAR','Paraguay',               'Paraguay',             '🇵🇾','CONMEBOL','D'),
('AUS','Australia',              'Australia',            '🇦🇺','AFC',     'D'),
('TUR','Turkiye',                'Turquia',              '🇹🇷','UEFA',    'D'),
-- Grupo E
('GER','Germany',                'Alemania',             '🇩🇪','UEFA',    'E'),
('CUW','Curacao',                'Curazao',              '🇨🇼','CONCACAF','E'),
('CIV','Ivory Coast',            'Costa de Marfil',      '🇨🇮','CAF',     'E'),
('ECU','Ecuador',                'Ecuador',              '🇪🇨','CONMEBOL','E'),
-- Grupo F
('NED','Netherlands',            'Paises Bajos',         '🇳🇱','UEFA',    'F'),
('JPN','Japan',                  'Japon',                '🇯🇵','AFC',     'F'),
('SWE','Sweden',                 'Suecia',               '🇸🇪','UEFA',    'F'),
('TUN','Tunisia',                'Tunez',                '🇹🇳','CAF',     'F'),
-- Grupo G
('BEL','Belgium',                'Belgica',              '🇧🇪','UEFA',    'G'),
('EGY','Egypt',                  'Egipto',               '🇪🇬','CAF',     'G'),
('IRN','Iran',                   'Iran',                 '🇮🇷','AFC',     'G'),
('NZL','New Zealand',            'Nueva Zelanda',        '🇳🇿','OFC',     'G'),
-- Grupo H
('ESP','Spain',                  'Espana',               '🇪🇸','UEFA',    'H'),
('CPV','Cape Verde',             'Cabo Verde',           '🇨🇻','CAF',     'H'),
('KSA','Saudi Arabia',           'Arabia Saudita',       '🇸🇦','AFC',     'H'),
('URU','Uruguay',                'Uruguay',              '🇺🇾','CONMEBOL','H'),
-- Grupo I
('FRA','France',                 'Francia',              '🇫🇷','UEFA',    'I'),
('SEN','Senegal',                'Senegal',              '🇸🇳','CAF',     'I'),
('IRQ','Iraq',                   'Irak',                 '🇮🇶','AFC',     'I'),
('NOR','Norway',                 'Noruega',              '🇳🇴','UEFA',    'I'),
-- Grupo J
('ARG','Argentina',              'Argentina',            '🇦🇷','CONMEBOL','J'),
('ALG','Algeria',                'Argelia',              '🇩🇿','CAF',     'J'),
('AUT','Austria',                'Austria',              '🇦🇹','UEFA',    'J'),
('JOR','Jordan',                 'Jordania',             '🇯🇴','AFC',     'J'),
-- Grupo K
('POR','Portugal',               'Portugal',             '🇵🇹','UEFA',    'K'),
('COD','DR Congo',               'RD Congo',             '🇨🇩','CAF',     'K'),
('UZB','Uzbekistan',             'Uzbekistan',           '🇺🇿','AFC',     'K'),
('COL','Colombia',               'Colombia',             '🇨🇴','CONMEBOL','K'),
-- Grupo L
('ENG','England',                'Inglaterra',           '🏴','UEFA',    'L'),
('CRO','Croatia',                'Croacia',              '🇭🇷','UEFA',    'L'),
('GHA','Ghana',                  'Ghana',                '🇬🇭','CAF',     'L'),
('PAN','Panama',                 'Panama',               '🇵🇦','CONCACAF','L');

-- ------------------------------------------------------------
-- Tabla de posiciones: una fila en cero por cada pais.
-- ------------------------------------------------------------
INSERT INTO tabla_posiciones
    (id_pais, grupo, posicion, partidos_jugados, partidos_ganados,
     partidos_empatados, partidos_perdidos, goles_favor, goles_contra,
     diferencia_goles, puntos)
SELECT p.id, p.grupo, 0, 0, 0, 0, 0, 0, 0, 0, 0
FROM paises p
WHERE p.grupo IS NOT NULL;
