-- ============================================================
-- V3__seed_data.sql
-- Datos iniciales: 32 paises (grupos A-H), estadios sede y
-- tabla de posiciones en cero para cada pais.
-- Los UUID se generan por defecto (gen_random_uuid()).
-- ============================================================

-- ------------------------------------------------------------
-- 32 paises del Mundial 2026 (4 por grupo, A-H)
-- ------------------------------------------------------------
INSERT INTO paises (codigo_fifa, nombre, nombre_es, bandera_emoji, confederacion, grupo) VALUES
-- Grupo A
('MEX','Mexico',        'Mexico',         '🇲🇽','CONCACAF','A'),
('CRO','Croatia',       'Croacia',        '🇭🇷','UEFA',    'A'),
('ECU','Ecuador',       'Ecuador',        '🇪🇨','CONMEBOL','A'),
('MAR','Morocco',       'Marruecos',      '🇲🇦','CAF',     'A'),
-- Grupo B
('CAN','Canada',        'Canada',         '🇨🇦','CONCACAF','B'),
('BEL','Belgium',       'Belgica',        '🇧🇪','UEFA',    'B'),
('KOR','South Korea',   'Corea del Sur',  '🇰🇷','AFC',     'B'),
('SEN','Senegal',       'Senegal',        '🇸🇳','CAF',     'B'),
-- Grupo C
('USA','United States', 'Estados Unidos', '🇺🇸','CONCACAF','C'),
('ENG','England',       'Inglaterra',     '🏴','UEFA',    'C'),
('IRN','Iran',          'Iran',           '🇮🇷','AFC',     'C'),
('GHA','Ghana',         'Ghana',          '🇬🇭','CAF',     'C'),
-- Grupo D
('ARG','Argentina',     'Argentina',      '🇦🇷','CONMEBOL','D'),
('NED','Netherlands',   'Holanda',        '🇳🇱','UEFA',    'D'),
('JPN','Japan',         'Japon',          '🇯🇵','AFC',     'D'),
('TUN','Tunisia',       'Tunez',          '🇹🇳','CAF',     'D'),
-- Grupo E
('BRA','Brazil',        'Brasil',         '🇧🇷','CONMEBOL','E'),
('FRA','France',        'Francia',        '🇫🇷','UEFA',    'E'),
('AUS','Australia',     'Australia',      '🇦🇺','AFC',     'E'),
('NGA','Nigeria',       'Nigeria',        '🇳🇬','CAF',     'E'),
-- Grupo F
('ESP','Spain',         'Espana',         '🇪🇸','UEFA',    'F'),
('URU','Uruguay',       'Uruguay',        '🇺🇾','CONMEBOL','F'),
('KSA','Saudi Arabia',  'Arabia Saudita', '🇸🇦','AFC',     'F'),
('CMR','Cameroon',      'Camerun',        '🇨🇲','CAF',     'F'),
-- Grupo G
('GER','Germany',       'Alemania',       '🇩🇪','UEFA',    'G'),
('POR','Portugal',      'Portugal',       '🇵🇹','UEFA',    'G'),
('COL','Colombia',      'Colombia',       '🇨🇴','CONMEBOL','G'),
('QAT','Qatar',         'Catar',          '🇶🇦','AFC',     'G'),
-- Grupo H
('ITA','Italy',         'Italia',         '🇮🇹','UEFA',    'H'),
('CRC','Costa Rica',    'Costa Rica',     '🇨🇷','CONCACAF','H'),
('PAR','Paraguay',      'Paraguay',       '🇵🇾','CONMEBOL','H'),
('EGY','Egypt',         'Egipto',         '🇪🇬','CAF',     'H');

-- ------------------------------------------------------------
-- Estadios sede del Mundial 2026
-- ------------------------------------------------------------
INSERT INTO estadios (nombre, ciudad, pais_sede, capacidad) VALUES
('Estadio Azteca',         'Ciudad de Mexico', 'Mexico',         87523),
('MetLife Stadium',        'Nueva York',       'Estados Unidos', 82500),
('AT&T Stadium',           'Dallas',           'Estados Unidos', 80000),
('SoFi Stadium',           'Los Angeles',      'Estados Unidos', 70240),
('BC Place',               'Vancouver',        'Canada',         54500),
('Estadio Tecnologico',    'Monterrey',        'Mexico',         42000),
('Levi''s Stadium',        'San Francisco',    'Estados Unidos', 68500),
('Lincoln Financial Field','Filadelfia',       'Estados Unidos', 69596),
('Arrowhead Stadium',      'Kansas City',      'Estados Unidos', 76416),
('Mercedes-Benz Stadium',  'Atlanta',          'Estados Unidos', 71000),
('NRG Stadium',            'Houston',          'Estados Unidos', 72220),
('Lumen Field',            'Seattle',          'Estados Unidos', 68740);

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
