-- =====================================================================================
-- CARRERAS, PLANES DE ESTUDIO, MATERIAS Y CORRELATIVIDADES
-- =====================================================================================
-- Se corre después de atenea_db_script.sql. Reemplaza a sus secciones "INSERTS DE CARRERAS" y
-- "CORRELATIVIDADES": hay que sacarlas de ese script, porque si no este falla por ids repetidos.
--
-- Las carreras 1 a 3 son las que estaban en el script de la base, copiadas sin cambios. Las 4 a 7
-- son nuevas, de los PDF de UTN FRMDP de tp_final/recursos:
--
--   carrera  plan  materias   plan_materia
--   1        1     1-48       1-48     Ingeniería Naval
--   2        2     49-87      49-94    Ingeniería Pesquera (comparte 7 materias con Naval)
--   3        3     88-105     95-112   TUP - Plan 2024
--   3        4     106-126    113-133  TUP - Plan 2003
--   4        5     127-152    134-159  Tecnicatura Superior en Interiorismo
--   5        6     153-172    160-179  Tecnicatura Universitaria en Administración - Plan 2025
--   6        7     173-197    180-204  Tecnicatura Universitaria en Procedimientos y Tecnologías Ambientales
--   7        8     198-227    205-234  Tecnicatura Universitaria en Producción Textil
--
-- Notas sobre las carreras nuevas:
-- * Los PDF de Interiorismo, Ambientales y Textil no dicen el año del plan: anio_inicio queda NULL.
-- * Administración: el PDF de recursos (Ordenanza 2112) trae sólo las correlatividades. Las horas y
--   el régimen (todas anuales) salen del plan de estudios de la Ordenanza 2111 que publica UTN FRGP:
--   https://www.frgp.utn.edu.ar/public/files/Plan_de_Estudios_TUA_Plan_2025.pdf
--   carga_horaria_total es la de interacción pedagógica (la de cursada), como en las otras carreras.
-- * Las materias nuevas no se comparten con otras carreras aunque coincidan el nombre y las horas
--   (Legislación de Interiorismo y de la TUP 2024, Inglés I de Ambientales y de la TUP 2024): son
--   asignaturas distintas, y como el final es de la materia (examen_final.materia_id), compartirlas
--   juntaría en la misma mesa a estudiantes de carreras distintas.
-- =====================================================================================

SET NAMES utf8mb4;
USE atenea_db;

-- CARRERAS --

INSERT INTO carrera (id, nombre, duracion, titulo_otorgado, modalidad_carrera) VALUES
(1, 'Ingeniería Naval', '6 años', 'Ingeniero Naval', 'PRESENCIAL'),
(2, 'Ingeniería Pesquera (Orientación Procesamiento)', '5 años', 'Ingeniero Pesquero', 'PRESENCIAL'),
(3, 'Tecnicatura Universitaria en Programación', '2 años', 'Técnico Universitario en Programación', 'PRESENCIAL'),
(4, 'Tecnicatura Superior en Interiorismo', '3 años', 'Técnico Superior en Interiorismo', 'PRESENCIAL'),
(5, 'Tecnicatura Universitaria en Administración', '2 años', 'Técnico Universitario en Administración', 'PRESENCIAL'),
(6, 'Tecnicatura Universitaria en Procedimientos y Tecnologías Ambientales', '3 años', 'Técnico Universitario en Procedimientos y Tecnologías Ambientales', 'PRESENCIAL'),
(7, 'Tecnicatura Universitaria en Producción Textil', '2 años y medio', 'Técnico Universitario en Producción Textil', 'PRESENCIAL');

INSERT INTO plan_estudio (id, carrera_id, nombre, anio_inicio) VALUES
(1, 1, 'Plan de Estudios Ingeniería Naval', 2004),
(2, 2, 'Plan de Estudios Ingeniería Pesquera', 2004),
(3, 3, 'Plan 2024', 2024),
(4, 3, 'Plan 2003', 2003),
(5, 4, 'Plan de Estudios Interiorismo', NULL),
(6, 5, 'Plan 2025', 2025),
(7, 6, 'Plan de Estudios Tecnologías Ambientales', NULL),
(8, 7, 'Plan de Estudios Producción Textil', NULL);

INSERT INTO materia (id, nombre, carga_horaria_semanal, carga_horaria_total) VALUES
(1, 'Álgebra y Geometría Analítica', 5, 160),
(2, 'Análisis Matemático I', 5, 160),
(3, 'Física I', 5, 160),
(4, 'Química General', 5, 160),
(5, 'Introducción a la Ingeniería Naval (Integradora)', 3, 96),
(6, 'Sistemas de Representación', 3, 96),
(7, 'Inglés I', 2, 64),
(8, 'Fundamentos de Informática', 2, 64),
(9, 'Análisis Matemático II', 5, 160),
(10, 'Probabilidad y Estadística', 3, 96),
(11, 'Física II', 5, 160),
(12, 'Física III', 2, 64),
(13, 'Análisis Estructural I', 4, 128),
(14, 'Ingeniería y Sociedad', 2, 64),
(15, 'Dibujo Naval (Integradora)', 4, 128),
(16, 'Legislación', 2, 64),
(17, 'Análisis Estructural II', 4, 128),
(18, 'Termodinámica', 5, 160),
(19, 'Mecánica Racional', 5, 160),
(20, 'Teoría del Buque I (Integradora)', 4, 128),
(21, 'Matemática Aplicada a la Ingeniería', 5, 160),
(22, 'Electrotecnia y Máquinas Eléctricas', 4, 128),
(23, 'Mecánica de los Fluidos', 3, 60),
(24, 'Teoría del Buque II (Integradora)', 3, 48),
(25, 'Alistamiento de Buques', 5, 80),
(26, 'Análisis Estructural III', 4, 64),
(27, 'Máquinas Alternativas y Turbomáquinas', 4, 64),
(28, 'Materiales Navales', 3, 48),
(29, 'Construcción Naval', 3, 48),
(30, 'Actividad Naviera', 3, 48),
(31, 'Inglés II', 3, 48),
(32, 'Plantas Eléctricas Navales', 2, 64),
(33, 'Plantas Propulsoras Navales', 5, 160),
(34, 'Cálculo de Estructuras de Buques', 5, 160),
(35, 'Mecánica Aplicada a las Máquinas', 4, 128),
(36, 'Proyecto de Buques (Integradora)', 5, 160),
(37, 'Organización Industrial', 2, 64),
(38, 'Soldadura', 3, 96),
(39, 'Análisis por Elementos Finitos', 3, 96),
(40, 'Proyecto Final (Integradora)', 2, 64),
(41, 'Embarcaciones Veloces', 3, 96),
(42, 'Seguridad, Higiene e Ingeniería Ambiental', 2, 64),
(43, 'Procedimientos de Astilleros', 3, 96),
(44, 'Práctica Profesional Supervisada', 0, 267),
(45, 'Modelización de Procesos (Electiva 1)', 2, 64),
(46, 'Diseño Naval Asistido (Electiva 2)', 3, 48),
(47, 'Navegación (Electiva 3)', 2, 64),
(48, 'Buques Militares (Electiva 4)', 2, 64),
(49, 'Recursos Pesqueros I (Integradora)', 4, 128),
(50, 'Física I', 4, 128),
(51, 'Buques Pesqueros (Electiva I)', 6, 96),
(52, 'Gabinete Sistemas de Representación', 3, 48),
(53, 'Gabinete Informática I', 2, 64),
(54, 'Física II', 8, 128),
(55, 'Química Analítica e Instrumental', 6, 192),
(56, 'Recursos Pesqueros II (Integradora)', 4, 128),
(57, 'Termodinámica', 8, 128),
(58, 'Seminario I', 6, 192),
(59, 'Gabinete de Idiomas I', 2, 64),
(60, 'Estática y Resistencia de Materiales', 6, 96),
(61, 'Química Orgánica y Biológica', 8, 128),
(62, 'Tecnología del Producto Pesquero I', 6, 96),
(63, 'Higiene y Seguridad Industrial', 4, 64),
(64, 'Tecnología del Producto Pesquero II (Integradora)', 6, 96),
(65, 'Bioquímica de Alimentos', 8, 128),
(66, 'Equipos e Instalaciones Industriales', 8, 128),
(67, 'Acuicultura I', 6, 96),
(68, 'Gabinete de Idiomas II', 2, 64),
(69, 'Gabinete de Informática II', 2, 64),
(70, 'Tecnología del Producto Pesquero III (Integradora)', 6, 96),
(71, 'Operaciones Unitarias I', 10, 160),
(72, 'Materiales y Envases de la Industria Pesquera', 4, 64),
(73, 'Gestión de Calidad', 6, 96),
(74, 'Matemática Superior', 4, 64),
(75, 'Bromatología', 8, 128),
(76, 'Operaciones Unitarias II', 8, 128),
(77, 'Proyecto Integrador I (Integradora)', 6, 96),
(78, 'Seminario II', 4, 64),
(79, 'Economía', 6, 96),
(80, 'Programación y Control de la Producción', 6, 96),
(81, 'Proyecto Integrador II (Integradora)', 6, 192),
(82, 'Maquinas e Instalaciones Térmicas', 6, 96),
(83, 'Investigación Operativa', 6, 96),
(84, 'Acuicultura II', 6, 96),
(85, 'Economía Pesquera (Electiva II)', 6, 96),
(86, 'Tratamientos de Efluentes (Electiva III)', 2, 64),
(87, 'Práctica Profesional Supervisada', 0, 200),
(88, 'Programación I', 8, 128),
(89, 'Arquitectura y Sistemas Operativos', 4, 64),
(90, 'Matemática', 4, 64),
(91, 'Organización Empresarial', 4, 64),
(92, 'Programación II', 8, 128),
(93, 'Probabilidad y Estadística', 4, 64),
(94, 'Base de Datos I', 4, 64),
(95, 'Inglés I', 4, 64),
(96, 'Programación III', 8, 128),
(97, 'Base de Datos II', 4, 64),
(98, 'Metodología de Sistemas I', 4, 64),
(99, 'Inglés II', 4, 64),
(100, 'Programación IV', 8, 128),
(101, 'Metodología de Sistemas II', 4, 64),
(102, 'Introducción al análisis de datos', 2, 32),
(103, 'Legislación', 2, 32),
(104, 'Gestión de desarrollo de software', 4, 64),
(105, 'Trabajo Final Integrador', 0, 95),
(106, 'Programación I', 6, 96),
(107, 'Sistemas de Procesamiento de Datos', 6, 96),
(108, 'Matemática', 9, 144),
(109, 'Inglés I', 3, 48),
(110, 'Laboratorio de Computación I', 6, 96),
(111, 'Programación II', 6, 96),
(112, 'Arquitectura y Sistemas Operativos', 6, 96),
(113, 'Estadística', 6, 96),
(114, 'Inglés II', 3, 48),
(115, 'Laboratorio de Computación II', 6, 96),
(116, 'Metodología de la Investigación', 3, 48),
(117, 'Programación III', 6, 96),
(118, 'Organización Contable de la Empresa', 6, 96),
(119, 'Organización Empresarial', 6, 96),
(120, 'Elementos de la Investigación Operativa', 6, 96),
(121, 'Laboratorio de Computación III', 6, 96),
(122, 'Metodología de Sistemas I', 12, 192),
(123, 'Diseño y Administración de Base de Datos', 6, 96),
(124, 'Legislación', 6, 96),
(125, 'Laboratorio de Computación IV', 6, 96),
(126, 'Práctica Profesional', 0, 60),
-- Tecnicatura Superior en Interiorismo
(127, 'Introducción al Diseño', 8, 128),
(128, 'Nociones Básicas de Física y Matemática Aplicada', 4, 64),
(129, 'Dibujo Técnico I', 4, 64),
(130, 'Comunicación Visual I', 2, 32),
(131, 'Introducción a la Historia del Diseño', 2, 32),
(132, 'Diseño I "A"', 8, 128),
(133, 'Dibujo Técnico II', 4, 64),
(134, 'Historia del Diseño I', 2, 32),
(135, 'Técnicas Constructivas I', 4, 64),
(136, 'Seminario Integrador I', 3, 48),
(137, 'Diseño I "B"', 8, 128),
(138, 'Historia del Diseño II', 2, 32),
(139, 'Dibujo Técnico III', 4, 64),
(140, 'Comunicación Visual II', 2, 32),
(141, 'Técnicas Constructivas II', 4, 64),
(142, 'Diseño II "A"', 8, 128),
(143, 'Comunicación Visual III', 2, 32),
(144, 'Técnicas Constructivas III', 4, 64),
(145, 'Seminario Integrador II', 3, 48),
(146, 'Lenguajes Digitales', 2, 32),
(147, 'Diseño II "B"', 8, 128),
(148, 'Práctica Profesional', 8, 256),
(149, 'Proyecto Final', 8, 256),
(150, 'Seminario Integrador III', 3, 48),
(151, 'Legislación', 2, 32),
(152, 'Marketing', 2, 32),
-- TUA - Plan 2025
(153, 'Administración General I', 5, 120),
(154, 'Matemática General', 3, 72),
(155, 'Informática I', 3, 72),
(156, 'Problemática Social Contemporánea', 3, 72),
(157, 'Economía I', 3, 72),
(158, 'Psicología de las Organizaciones', 2, 48),
(159, 'Ambiente, Economía y Sociedad', 2, 48),
(160, 'Legislación I', 2, 48),
(161, 'Estado y Sociedad', 2, 48),
(162, 'Seminario', 2, 48),
(163, 'Administración General II', 5, 120),
(164, 'Informática II', 3, 72),
(165, 'Comportamiento Organizacional', 3, 72),
(166, 'Estadística', 2, 48),
(167, 'Economía II', 2, 48),
(168, 'Legislación II', 2, 48),
(169, 'Administración Estatal', 2, 48),
(170, 'Gestión de Recursos Humanos', 2, 48),
(171, 'Finanzas y Técnicas Contables', 2, 48),
(172, 'Proyecto Final', 4, 96),
-- Tecnicatura Universitaria en Procedimientos y Tecnologías Ambientales
(173, 'Biología General', 5, 80),
(174, 'Ciencias de la Tierra', 5, 80),
(175, 'Físico-Química General', 5, 80),
(176, 'Matemática', 5, 80),
(177, 'Ecología General', 5, 80),
(178, 'Química Ambiental', 5, 80),
(179, 'Física', 5, 80),
(180, 'Probabilidad y Estadística', 5, 80),
(181, 'Principios de Derecho y Normativa Ambiental', 4, 64),
(182, 'Calidad de Aguas Superficiales y Subterráneas', 5, 80),
(183, 'Sistema de Tratamiento de Residuos y Efluentes', 5, 80),
(184, 'Contaminación Atmosférica', 4, 64),
(185, 'Gabinete de Electrónica', 3, 48),
(186, 'Sistemas de Control y Gestión Ambiental', 4, 64),
(187, 'Tecnologías de Muestreo, Medición y Control Ambiental', 5, 80),
(188, 'Energía y Medio Ambiente', 4, 64),
(189, 'Gabinete de informática', 3, 48),
(190, 'Inglés I', 4, 64),
(191, 'Evaluación de Impacto y Auditorias Ambientales', 4, 64),
(192, 'Seguridad e Higiene Laboral', 4, 64),
(193, 'Taller de Elaboración de Informes Técnicos', 3, 48),
(194, 'Cartografía y GIS', 5, 80),
(195, 'Problemática Ambiental Regional y Nacional', 4, 64),
(196, 'Inglés II', 5, 80),
(197, 'Práctica Profesional Supervisada / Trabajo Final Integrador', 5, 80),
-- Tecnicatura Universitaria en Producción Textil
(198, 'Matemáticas', 4, 64),
(199, 'Química General', 4, 64),
(200, 'Medios expresivos I', 3, 48),
(201, 'Historia', 3, 48),
(202, 'Seminario Integrador I', 3, 48),
(203, 'Fundamentos Textiles', 5, 160),
(204, 'Sistemas Productivos I', 3, 96),
(205, 'Física I', 4, 64),
(206, 'Química Orgánica', 4, 64),
(207, 'Gestión de las Organizaciones', 3, 48),
(208, 'Dibujo Técnico', 3, 48),
(209, 'Seminario Integrador II', 3, 48),
(210, 'Física II', 4, 64),
(211, 'Química Textil I', 4, 64),
(212, 'Medios expresivos II', 3, 48),
(213, 'Seminario Integrador III', 3, 48),
(214, 'Procesos Productivos Industriales', 5, 160),
(215, 'Sistemas Productivos II', 3, 96),
(216, 'Matemática Aplicada', 4, 64),
(217, 'Química Textil II', 4, 64),
(218, 'Emprendimientos Comerciales', 3, 48),
(219, 'Historia de los Estilos', 3, 48),
(220, 'Seminario Integrador IV', 3, 48),
(221, 'Informática Aplicada', 3, 48),
(222, 'Tintorería y Estampería', 5, 80),
(223, 'Estrategias de Comercialización', 4, 64),
(224, 'Inglés Técnico', 4, 64),
(225, 'Gestión de Diseño', 3, 48),
(226, 'Legislación', 3, 48),
(227, 'Seminario Integrador V', 3, 48);

-- Se cargan los id de forma explicita para poder referenciarlos
-- directamente desde correlatividad_materias.
INSERT INTO plan_materia (id, plan_estudio_id, materia_id, codigo, anio_cursado, periodo_lectivo) VALUES
-- Ingenieria Naval
(  1, 1,   1, '01',      1, 'ANUAL'),                -- Álgebra y Geometría Analítica
(  2, 1,   2, '02',      1, 'ANUAL'),                -- Análisis Matemático I
(  3, 1,   3, '03',      1, 'ANUAL'),                -- Física I
(  4, 1,   4, '04',      1, 'ANUAL'),                -- Química General
(  5, 1,   5, '05',      1, 'ANUAL'),                -- Introducción a la Ingeniería Naval (Integradora)
(  6, 1,   6, '06',      1, 'ANUAL'),                -- Sistemas de Representación
(  7, 1,   7, '07',      1, 'ANUAL'),                -- Inglés I
(  8, 1,   8, '08',      1, 'ANUAL'),                -- Fundamentos de Informática
(  9, 1,   9, '09',      2, 'ANUAL'),                -- Análisis Matemático II
( 10, 1,  10, '10',      2, 'ANUAL'),                -- Probabilidad y Estadística
( 11, 1,  11, '11',      2, 'ANUAL'),                -- Física II
( 12, 1,  12, '12',      2, 'SEGUNDO_CUATRIMESTRE'), -- Física III
( 13, 1,  13, '13',      2, 'ANUAL'),                -- Análisis Estructural I
( 14, 1,  14, '14',      2, 'PRIMER_CUATRIMESTRE'),  -- Ingeniería y Sociedad
( 15, 1,  15, '15',      2, 'ANUAL'),                -- Dibujo Naval (Integradora)
( 16, 1,  16, '16',      2, 'ANUAL'),                -- Legislación
( 17, 1,  45, NULL,      2, 'SEGUNDO_CUATRIMESTRE'), -- Modelización de Procesos (Electiva 1)
( 18, 1,  17, '17',      3, 'ANUAL'),                -- Análisis Estructural II
( 19, 1,  18, '18',      3, 'ANUAL'),                -- Termodinámica
( 20, 1,  19, '19',      3, 'ANUAL'),                -- Mecánica Racional
( 21, 1,  20, '20',      3, 'ANUAL'),                -- Teoría del Buque I (Integradora)
( 22, 1,  21, '21',      3, 'ANUAL'),                -- Matemática Aplicada a la Ingeniería
( 23, 1,  22, '22',      3, 'ANUAL'),                -- Electrotecnia y Máquinas Eléctricas
( 24, 1,  23, '23',      3, 'ANUAL'),                -- Mecánica de los Fluidos
( 25, 1,  24, '24',      4, 'ANUAL'),                -- Teoría del Buque II (Integradora)
( 26, 1,  25, '25',      4, 'ANUAL'),                -- Alistamiento de Buques
( 27, 1,  26, '26',      4, 'SEGUNDO_CUATRIMESTRE'), -- Análisis Estructural III
( 28, 1,  27, '27',      4, 'ANUAL'),                -- Máquinas Alternativas y Turbomáquinas
( 29, 1,  28, '28',      4, 'ANUAL'),                -- Materiales Navales
( 30, 1,  29, '29',      4, 'ANUAL'),                -- Construcción Naval
( 31, 1,  30, '30',      4, 'PRIMER_CUATRIMESTRE'),  -- Actividad Naviera
( 32, 1,  31, '31',      4, 'ANUAL'),                -- Inglés II
( 33, 1,  46, NULL,      4, 'SEGUNDO_CUATRIMESTRE'), -- Diseño Naval Asistido (Electiva 2)
( 34, 1,  32, '32',      5, 'SEGUNDO_CUATRIMESTRE'), -- Plantas Eléctricas Navales
( 35, 1,  33, '33',      5, 'ANUAL'),                -- Plantas Propulsoras Navales
( 36, 1,  34, '34',      5, 'ANUAL'),                -- Cálculo de Estructuras de Buques
( 37, 1,  35, '35',      5, 'ANUAL'),                -- Mecánica Aplicada a las Máquinas
( 38, 1,  36, '36',      5, 'ANUAL'),                -- Proyecto de Buques (Integradora)
( 39, 1,  37, '37',      5, 'ANUAL'),                -- Organización Industrial
( 40, 1,  38, '38',      5, 'ANUAL'),                -- Soldadura
( 41, 1,  39, '39',      5, 'PRIMER_CUATRIMESTRE'),  -- Análisis por Elementos Finitos
( 42, 1,  40, '40',      6, 'ANUAL'),                -- Proyecto Final (Integradora)
( 43, 1,  41, '41',      6, 'SEGUNDO_CUATRIMESTRE'), -- Embarcaciones Veloces
( 44, 1,  42, '42',      6, 'ANUAL'),                -- Seguridad, Higiene e Ingeniería Ambiental
( 45, 1,  43, '43',      6, 'SEGUNDO_CUATRIMESTRE'), -- Procedimientos de Astilleros
( 46, 1,  47, NULL,      6, 'SEGUNDO_CUATRIMESTRE'), -- Navegación (Electiva 3)
( 47, 1,  48, NULL,      6, 'PRIMER_CUATRIMESTRE'),  -- Buques Militares (Electiva 4)
( 48, 1,  44, '44',      6, 'ANUAL'),                -- Práctica Profesional Supervisada

-- Ingenieria Pesquera
( 49, 2,   1, '01',      1, 'ANUAL'),                -- Álgebra y Geometría Analítica
( 50, 2,   2, '02',      1, 'ANUAL'),                -- Análisis Matemático I
( 51, 2,   4, '03',      1, 'ANUAL'),                -- Química General
( 52, 2,  49, '04',      1, 'ANUAL'),                -- Recursos Pesqueros I (Integradora)
( 53, 2,  14, '05',      1, 'PRIMER_CUATRIMESTRE'),  -- Ingeniería y Sociedad
( 54, 2,  50, '06',      1, 'ANUAL'),                -- Física I
( 55, 2,  51, NULL,      1, 'SEGUNDO_CUATRIMESTRE'), -- Buques Pesqueros (Electiva I)
( 56, 2,  52, NULL,      1, 'SEGUNDO_CUATRIMESTRE'), -- Gabinete Sistemas de Representación
( 57, 2,  53, NULL,      1, 'ANUAL'),                -- Gabinete Informática I
( 58, 2,  54, '07',      2, 'PRIMER_CUATRIMESTRE'),  -- Física II
( 59, 2,  55, '08',      2, 'ANUAL'),                -- Química Analítica e Instrumental
( 60, 2,  56, '09',      2, 'ANUAL'),                -- Recursos Pesqueros II (Integradora)
( 61, 2,   9, '10',      2, 'ANUAL'),                -- Análisis Matemático II
( 62, 2,  57, '11',      2, 'SEGUNDO_CUATRIMESTRE'), -- Termodinámica
( 63, 2,  58, NULL,      2, 'ANUAL'),                -- Seminario I
( 64, 2,  59, NULL,      2, 'ANUAL'),                -- Gabinete de Idiomas I
( 65, 2,  60, '12',      3, 'PRIMER_CUATRIMESTRE'),  -- Estática y Resistencia de Materiales
( 66, 2,  61, '13',      3, 'PRIMER_CUATRIMESTRE'),  -- Química Orgánica y Biológica
( 67, 2,  62, '14',      3, 'PRIMER_CUATRIMESTRE'),  -- Tecnología del Producto Pesquero I
( 68, 2,  63, '15',      3, 'PRIMER_CUATRIMESTRE'),  -- Higiene y Seguridad Industrial
( 69, 2,  10, '16',      3, 'ANUAL'),                -- Probabilidad y Estadística
( 70, 2,  64, '17',      3, 'SEGUNDO_CUATRIMESTRE'), -- Tecnología del Producto Pesquero II (Integradora)
( 71, 2,  65, '18',      3, 'SEGUNDO_CUATRIMESTRE'), -- Bioquímica de Alimentos
( 72, 2,  66, '19',      3, 'SEGUNDO_CUATRIMESTRE'), -- Equipos e Instalaciones Industriales
( 73, 2,  67, '32',      3, 'SEGUNDO_CUATRIMESTRE'), -- Acuicultura I
( 74, 2,  68, NULL,      3, 'ANUAL'),                -- Gabinete de Idiomas II
( 75, 2,  69, NULL,      3, 'ANUAL'),                -- Gabinete de Informática II
( 76, 2,  70, '21',      4, 'PRIMER_CUATRIMESTRE'),  -- Tecnología del Producto Pesquero III (Integradora)
( 77, 2,  71, '22',      4, 'PRIMER_CUATRIMESTRE'),  -- Operaciones Unitarias I
( 78, 2,  72, '23',      4, 'PRIMER_CUATRIMESTRE'),  -- Materiales y Envases de la Industria Pesquera
( 79, 2,  73, '24',      4, 'PRIMER_CUATRIMESTRE'),  -- Gestión de Calidad
( 80, 2,  74, '20',      4, 'SEGUNDO_CUATRIMESTRE'), -- Matemática Superior
( 81, 2,  75, '25',      4, 'SEGUNDO_CUATRIMESTRE'), -- Bromatología
( 82, 2,  76, '26',      4, 'SEGUNDO_CUATRIMESTRE'), -- Operaciones Unitarias II
( 83, 2,  77, '27',      4, 'SEGUNDO_CUATRIMESTRE'), -- Proyecto Integrador I (Integradora)
( 84, 2,  78, NULL,      4, 'SEGUNDO_CUATRIMESTRE'), -- Seminario II
( 85, 2,  79, '28',      5, 'PRIMER_CUATRIMESTRE'),  -- Economía
( 86, 2,  80, '29',      5, 'PRIMER_CUATRIMESTRE'),  -- Programación y Control de la Producción
( 87, 2,  16, '30',      5, 'ANUAL'),                -- Legislación
( 88, 2,  81, '31',      5, 'ANUAL'),                -- Proyecto Integrador II (Integradora)
( 89, 2,  82, '33',      5, 'SEGUNDO_CUATRIMESTRE'), -- Maquinas e Instalaciones Térmicas
( 90, 2,  83, '34',      5, 'SEGUNDO_CUATRIMESTRE'), -- Investigación Operativa
( 91, 2,  84, '50',      5, 'SEGUNDO_CUATRIMESTRE'), -- Acuicultura II
( 92, 2,  85, NULL,      5, 'PRIMER_CUATRIMESTRE'),  -- Economía Pesquera (Electiva II)
( 93, 2,  86, NULL,      5, 'ANUAL'),                -- Tratamientos de Efluentes (Electiva III)
( 94, 2,  87, NULL,      5, 'ANUAL'),                -- Práctica Profesional Supervisada

-- TUP - Plan 2024
( 95, 3,  88, '1',       1, 'PRIMER_CUATRIMESTRE'),  -- Programación I
( 96, 3,  89, '2',       1, 'PRIMER_CUATRIMESTRE'),  -- Arquitectura y Sistemas Operativos
( 97, 3,  90, '3',       1, 'PRIMER_CUATRIMESTRE'),  -- Matemática
( 98, 3,  91, '4',       1, 'PRIMER_CUATRIMESTRE'),  -- Organización Empresarial
( 99, 3,  92, '5',       1, 'SEGUNDO_CUATRIMESTRE'), -- Programación II
(100, 3,  93, '6',       1, 'SEGUNDO_CUATRIMESTRE'), -- Probabilidad y Estadística
(101, 3,  94, '7',       1, 'SEGUNDO_CUATRIMESTRE'), -- Base de Datos I
(102, 3,  95, '8',       1, 'SEGUNDO_CUATRIMESTRE'), -- Inglés I
(103, 3,  96, '9',       2, 'PRIMER_CUATRIMESTRE'),  -- Programación III
(104, 3,  97, '10',      2, 'PRIMER_CUATRIMESTRE'),  -- Base de Datos II
(105, 3,  98, '11',      2, 'PRIMER_CUATRIMESTRE'),  -- Metodología de Sistemas I
(106, 3,  99, '12',      2, 'PRIMER_CUATRIMESTRE'),  -- Inglés II
(107, 3, 100, '13',      2, 'SEGUNDO_CUATRIMESTRE'), -- Programación IV
(108, 3, 101, '14',      2, 'SEGUNDO_CUATRIMESTRE'), -- Metodología de Sistemas II
(109, 3, 102, '15',      2, 'SEGUNDO_CUATRIMESTRE'), -- Introducción al análisis de datos
(110, 3, 103, '16',      2, 'SEGUNDO_CUATRIMESTRE'), -- Legislación
(111, 3, 104, '17',      2, 'SEGUNDO_CUATRIMESTRE'), -- Gestión de desarrollo de software
(112, 3, 105, '18',      2, 'SEGUNDO_CUATRIMESTRE'), -- Trabajo Final Integrador

-- TUP - Plan 2003
(113, 4, 106, '95-1121', 1, 'PRIMER_CUATRIMESTRE'),  -- Programación I
(114, 4, 107, '95-1122', 1, 'PRIMER_CUATRIMESTRE'),  -- Sistemas de Procesamiento de Datos
(115, 4, 108, '95-1123', 1, 'PRIMER_CUATRIMESTRE'),  -- Matemática
(116, 4, 109, '95-1111', 1, 'PRIMER_CUATRIMESTRE'),  -- Inglés I
(117, 4, 110, '95-1124', 1, 'PRIMER_CUATRIMESTRE'),  -- Laboratorio de Computación I
(118, 4, 111, '95-1125', 1, 'SEGUNDO_CUATRIMESTRE'), -- Programación II
(119, 4, 112, '95-1126', 1, 'SEGUNDO_CUATRIMESTRE'), -- Arquitectura y Sistemas Operativos
(120, 4, 113, '95-1104', 1, 'SEGUNDO_CUATRIMESTRE'), -- Estadística
(121, 4, 114, '95-1112', 1, 'SEGUNDO_CUATRIMESTRE'), -- Inglés II
(122, 4, 115, '95-1127', 1, 'SEGUNDO_CUATRIMESTRE'), -- Laboratorio de Computación II
(123, 4, 116, '95-1128', 1, 'SEGUNDO_CUATRIMESTRE'), -- Metodología de la Investigación
(124, 4, 117, '95-1230', 2, 'PRIMER_CUATRIMESTRE'),  -- Programación III
(125, 4, 118, '95-1231', 2, 'PRIMER_CUATRIMESTRE'),  -- Organización Contable de la Empresa
(126, 4, 119, '95-1232', 2, 'PRIMER_CUATRIMESTRE'),  -- Organización Empresarial
(127, 4, 120, '95-1233', 2, 'PRIMER_CUATRIMESTRE'),  -- Elementos de la Investigación Operativa
(128, 4, 121, '95-1234', 2, 'PRIMER_CUATRIMESTRE'),  -- Laboratorio de Computación III
(129, 4, 122, '95-1235', 2, 'SEGUNDO_CUATRIMESTRE'), -- Metodología de Sistemas I
(130, 4, 123, '95-1236', 2, 'SEGUNDO_CUATRIMESTRE'), -- Diseño y Administración de Base de Datos
(131, 4, 124, '95-1209', 2, 'SEGUNDO_CUATRIMESTRE'), -- Legislación
(132, 4, 125, '95-1237', 2, 'SEGUNDO_CUATRIMESTRE'), -- Laboratorio de Computación IV
(133, 4, 126, NULL,      2, 'SEGUNDO_CUATRIMESTRE'), -- Práctica Profesional

-- Tecnicatura Superior en Interiorismo
(134, 5, 127, '77-10-101', 1, 'PRIMER_CUATRIMESTRE'),  -- Introducción al Diseño
(135, 5, 128, '77-10-102', 1, 'PRIMER_CUATRIMESTRE'),  -- Nociones Básicas de Física y Matemática Aplicada
(136, 5, 129, '77-10-103', 1, 'PRIMER_CUATRIMESTRE'),  -- Dibujo Técnico I
(137, 5, 130, '77-10-104', 1, 'PRIMER_CUATRIMESTRE'),  -- Comunicación Visual I
(138, 5, 131, '77-10-105', 1, 'PRIMER_CUATRIMESTRE'),  -- Introducción a la Historia del Diseño
(139, 5, 132, '77-10-201', 1, 'SEGUNDO_CUATRIMESTRE'), -- Diseño I "A"
(140, 5, 133, '77-10-203', 1, 'SEGUNDO_CUATRIMESTRE'), -- Dibujo Técnico II
(141, 5, 134, '77-10-205', 1, 'SEGUNDO_CUATRIMESTRE'), -- Historia del Diseño I
(142, 5, 135, '77-10-206', 1, 'SEGUNDO_CUATRIMESTRE'), -- Técnicas Constructivas I
(143, 5, 136, '77-10-207', 1, 'SEGUNDO_CUATRIMESTRE'), -- Seminario Integrador I
(144, 5, 137, '77-10-202', 2, 'PRIMER_CUATRIMESTRE'),  -- Diseño I "B"
(145, 5, 138, '77-10-305', 2, 'PRIMER_CUATRIMESTRE'),  -- Historia del Diseño II
(146, 5, 139, '77-10-303', 2, 'PRIMER_CUATRIMESTRE'),  -- Dibujo Técnico III
(147, 5, 140, '77-10-304', 2, 'PRIMER_CUATRIMESTRE'),  -- Comunicación Visual II
(148, 5, 141, '77-10-306', 2, 'PRIMER_CUATRIMESTRE'),  -- Técnicas Constructivas II
(149, 5, 142, '77-10-401', 2, 'SEGUNDO_CUATRIMESTRE'), -- Diseño II "A"
(150, 5, 143, '77-10-404', 2, 'SEGUNDO_CUATRIMESTRE'), -- Comunicación Visual III
(151, 5, 144, '77-10-406', 2, 'SEGUNDO_CUATRIMESTRE'), -- Técnicas Constructivas III
(152, 5, 145, '77-10-407', 2, 'SEGUNDO_CUATRIMESTRE'), -- Seminario Integrador II
(153, 5, 146, '77-10-408', 2, 'SEGUNDO_CUATRIMESTRE'), -- Lenguajes Digitales
(154, 5, 147, '77-10-402', 3, 'PRIMER_CUATRIMESTRE'),  -- Diseño II "B"
(155, 5, 148, '77-10-508', 3, 'ANUAL'),                -- Práctica Profesional
(156, 5, 149, '77-10-509', 3, 'ANUAL'),                -- Proyecto Final
(157, 5, 150, '77-10-607', 3, 'SEGUNDO_CUATRIMESTRE'), -- Seminario Integrador III
(158, 5, 151, '77-10-611', 3, 'SEGUNDO_CUATRIMESTRE'), -- Legislación
(159, 5, 152, '77-10-612', 3, 'SEGUNDO_CUATRIMESTRE'), -- Marketing

-- TUA - Plan 2025
(160, 6, 153, '1',       1, 'ANUAL'),                -- Administración General I
(161, 6, 154, '2',       1, 'ANUAL'),                -- Matemática General
(162, 6, 155, '3',       1, 'ANUAL'),                -- Informática I
(163, 6, 156, '4',       1, 'ANUAL'),                -- Problemática Social Contemporánea
(164, 6, 157, '5',       1, 'ANUAL'),                -- Economía I
(165, 6, 158, '6',       1, 'ANUAL'),                -- Psicología de las Organizaciones
(166, 6, 159, '7',       1, 'ANUAL'),                -- Ambiente, Economía y Sociedad
(167, 6, 160, '8',       1, 'ANUAL'),                -- Legislación I
(168, 6, 161, '9',       1, 'ANUAL'),                -- Estado y Sociedad
(169, 6, 162, '10',      1, 'ANUAL'),                -- Seminario
(170, 6, 163, '11',      2, 'ANUAL'),                -- Administración General II
(171, 6, 164, '12',      2, 'ANUAL'),                -- Informática II
(172, 6, 165, '13',      2, 'ANUAL'),                -- Comportamiento Organizacional
(173, 6, 166, '14',      2, 'ANUAL'),                -- Estadística
(174, 6, 167, '15',      2, 'ANUAL'),                -- Economía II
(175, 6, 168, '16',      2, 'ANUAL'),                -- Legislación II
(176, 6, 169, '17',      2, 'ANUAL'),                -- Administración Estatal
(177, 6, 170, '18',      2, 'ANUAL'),                -- Gestión de Recursos Humanos
(178, 6, 171, '19',      2, 'ANUAL'),                -- Finanzas y Técnicas Contables
(179, 6, 172, '20',      2, 'ANUAL'),                -- Proyecto Final

-- Tecnologías Ambientales
(180, 7, 173, '01',      1, 'PRIMER_CUATRIMESTRE'),  -- Biología General
(181, 7, 174, '02',      1, 'PRIMER_CUATRIMESTRE'),  -- Ciencias de la Tierra
(182, 7, 175, '03',      1, 'PRIMER_CUATRIMESTRE'),  -- Físico-Química General
(183, 7, 176, '04',      1, 'PRIMER_CUATRIMESTRE'),  -- Matemática
(184, 7, 177, '05',      1, 'SEGUNDO_CUATRIMESTRE'), -- Ecología General
(185, 7, 178, '06',      1, 'SEGUNDO_CUATRIMESTRE'), -- Química Ambiental
(186, 7, 179, '07',      1, 'SEGUNDO_CUATRIMESTRE'), -- Física
(187, 7, 180, '08',      1, 'SEGUNDO_CUATRIMESTRE'), -- Probabilidad y Estadística
(188, 7, 181, '09',      2, 'PRIMER_CUATRIMESTRE'),  -- Principios de Derecho y Normativa Ambiental
(189, 7, 182, '10',      2, 'PRIMER_CUATRIMESTRE'),  -- Calidad de Aguas Superficiales y Subterráneas
(190, 7, 183, '11',      2, 'PRIMER_CUATRIMESTRE'),  -- Sistema de Tratamiento de Residuos y Efluentes
(191, 7, 184, '12',      2, 'PRIMER_CUATRIMESTRE'),  -- Contaminación Atmosférica
(192, 7, 185, '13',      2, 'PRIMER_CUATRIMESTRE'),  -- Gabinete de Electrónica
(193, 7, 186, '14',      2, 'SEGUNDO_CUATRIMESTRE'), -- Sistemas de Control y Gestión Ambiental
(194, 7, 187, '15',      2, 'SEGUNDO_CUATRIMESTRE'), -- Tecnologías de Muestreo, Medición y Control Ambiental
(195, 7, 188, '16',      2, 'SEGUNDO_CUATRIMESTRE'), -- Energía y Medio Ambiente
(196, 7, 189, '17',      2, 'SEGUNDO_CUATRIMESTRE'), -- Gabinete de informática
(197, 7, 190, '18',      2, 'SEGUNDO_CUATRIMESTRE'), -- Inglés I
(198, 7, 191, '19',      3, 'PRIMER_CUATRIMESTRE'),  -- Evaluación de Impacto y Auditorias Ambientales
(199, 7, 192, '20',      3, 'PRIMER_CUATRIMESTRE'),  -- Seguridad e Higiene Laboral
(200, 7, 193, '21',      3, 'PRIMER_CUATRIMESTRE'),  -- Taller de Elaboración de Informes Técnicos
(201, 7, 194, '22',      3, 'PRIMER_CUATRIMESTRE'),  -- Cartografía y GIS
(202, 7, 195, '23',      3, 'SEGUNDO_CUATRIMESTRE'), -- Problemática Ambiental Regional y Nacional
(203, 7, 196, '24',      3, 'SEGUNDO_CUATRIMESTRE'), -- Inglés II
(204, 7, 197, '25',      3, 'SEGUNDO_CUATRIMESTRE'), -- Práctica Profesional Supervisada / Trabajo Final Integrador

-- Producción Textil
(205, 8, 198, '01',      1, 'PRIMER_CUATRIMESTRE'),  -- Matemáticas
(206, 8, 199, '02',      1, 'PRIMER_CUATRIMESTRE'),  -- Química General
(207, 8, 200, '03',      1, 'PRIMER_CUATRIMESTRE'),  -- Medios expresivos I
(208, 8, 201, '04',      1, 'PRIMER_CUATRIMESTRE'),  -- Historia
(209, 8, 202, '31',      1, 'PRIMER_CUATRIMESTRE'),  -- Seminario Integrador I
(210, 8, 203, '05',      1, 'ANUAL'),                -- Fundamentos Textiles
(211, 8, 204, '06',      1, 'ANUAL'),                -- Sistemas Productivos I
(212, 8, 205, '07',      1, 'SEGUNDO_CUATRIMESTRE'), -- Física I
(213, 8, 206, '08',      1, 'SEGUNDO_CUATRIMESTRE'), -- Química Orgánica
(214, 8, 207, '09',      1, 'SEGUNDO_CUATRIMESTRE'), -- Gestión de las Organizaciones
(215, 8, 208, '10',      1, 'SEGUNDO_CUATRIMESTRE'), -- Dibujo Técnico
(216, 8, 209, '32',      1, 'SEGUNDO_CUATRIMESTRE'), -- Seminario Integrador II
(217, 8, 210, '11',      2, 'PRIMER_CUATRIMESTRE'),  -- Física II
(218, 8, 211, '12',      2, 'PRIMER_CUATRIMESTRE'),  -- Química Textil I
(219, 8, 212, '13',      2, 'PRIMER_CUATRIMESTRE'),  -- Medios expresivos II
(220, 8, 213, '33',      2, 'PRIMER_CUATRIMESTRE'),  -- Seminario Integrador III
(221, 8, 214, '14',      2, 'ANUAL'),                -- Procesos Productivos Industriales
(222, 8, 215, '15',      2, 'ANUAL'),                -- Sistemas Productivos II
(223, 8, 216, '16',      2, 'SEGUNDO_CUATRIMESTRE'), -- Matemática Aplicada
(224, 8, 217, '17',      2, 'SEGUNDO_CUATRIMESTRE'), -- Química Textil II
(225, 8, 218, '18',      2, 'SEGUNDO_CUATRIMESTRE'), -- Emprendimientos Comerciales
(226, 8, 219, '19',      2, 'SEGUNDO_CUATRIMESTRE'), -- Historia de los Estilos
(227, 8, 220, '34',      2, 'SEGUNDO_CUATRIMESTRE'), -- Seminario Integrador IV
(228, 8, 221, '20',      3, 'PRIMER_CUATRIMESTRE'),  -- Informática Aplicada
(229, 8, 222, '21',      3, 'PRIMER_CUATRIMESTRE'),  -- Tintorería y Estampería
(230, 8, 223, '22',      3, 'PRIMER_CUATRIMESTRE'),  -- Estrategias de Comercialización
(231, 8, 224, '23',      3, 'PRIMER_CUATRIMESTRE'),  -- Inglés Técnico
(232, 8, 225, '24',      3, 'PRIMER_CUATRIMESTRE'),  -- Gestión de Diseño
(233, 8, 226, '25',      3, 'PRIMER_CUATRIMESTRE'),  -- Legislación
(234, 8, 227, '35',      3, 'PRIMER_CUATRIMESTRE');  -- Seminario Integrador V

-- CORRELATIVIDADES --
--
-- Cargadas directamente contra plan_materia: cada fila referencia el id
-- de la materia y el de su correlativa dentro del MISMO plan de estudio.
-- Fuente: PDFs de regimen de correlatividades de UTN FRMDP (tp_final/recursos).
-- El comentario de cada fila indica el nombre de la correlativa requerida.

-- ============================================================
-- Plan 1 - Ingenieria Naval
-- ============================================================
INSERT INTO correlatividad_materias
    (plan_estudio_id, plan_materia_id, plan_materia_correlativa_id, estado_para_cursar, estado_para_rendir) VALUES
-- Análisis Matemático II
(1,   9,   1, 'CURSADA',  'APROBADA'), -- Álgebra y Geometría Analítica
(1,   9,   2, 'CURSADA',  'APROBADA'), -- Análisis Matemático I
-- Probabilidad y Estadística
(1,  10,   1, 'CURSADA',  'APROBADA'), -- Álgebra y Geometría Analítica
(1,  10,   2, 'CURSADA',  'APROBADA'), -- Análisis Matemático I
-- Física II
(1,  11,   1, 'CURSADA',  'APROBADA'), -- Álgebra y Geometría Analítica
(1,  11,   2, 'CURSADA',  'APROBADA'), -- Análisis Matemático I
(1,  11,   3, 'CURSADA',  'APROBADA'), -- Física I
-- Física III
(1,  12,   2, 'CURSADA',  'APROBADA'), -- Análisis Matemático I
(1,  12,   3, 'CURSADA',  'APROBADA'), -- Física I
(1,  12,  11, NULL,       'APROBADA'), -- Física II
-- Análisis Estructural I
(1,  13,   1, 'CURSADA',  'APROBADA'), -- Álgebra y Geometría Analítica
(1,  13,   2, 'CURSADA',  'APROBADA'), -- Análisis Matemático I
(1,  13,   3, 'CURSADA',  'APROBADA'), -- Física I
-- Dibujo Naval (Integradora)
(1,  15,   5, 'CURSADA',  'APROBADA'), -- Introducción a la Ingeniería Naval (Integradora)
(1,  15,   6, 'CURSADA',  'APROBADA'), -- Sistemas de Representación
-- Análisis Estructural II
(1,  18,   9, 'CURSADA',  'APROBADA'), -- Análisis Matemático II
(1,  18,  13, 'CURSADA',  'APROBADA'), -- Análisis Estructural I
(1,  18,   1, 'APROBADA', NULL),       -- Álgebra y Geometría Analítica
(1,  18,   3, 'APROBADA', NULL),       -- Física I
-- Termodinámica
(1,  19,   9, 'CURSADA',  'APROBADA'), -- Análisis Matemático II
(1,  19,  11, 'CURSADA',  'APROBADA'), -- Física II
-- Mecánica Racional
(1,  20,   9, 'CURSADA',  'APROBADA'), -- Análisis Matemático II
(1,  20,   3, 'APROBADA', NULL),       -- Física I
-- Teoría del Buque I (Integradora)
(1,  21,   9, 'CURSADA',  'APROBADA'), -- Análisis Matemático II
(1,  21,  10, 'CURSADA',  'APROBADA'), -- Probabilidad y Estadística
(1,  21,  15, 'CURSADA',  'APROBADA'), -- Dibujo Naval (Integradora)
(1,  21,   2, 'APROBADA', NULL),       -- Análisis Matemático I
(1,  21,   5, 'APROBADA', NULL),       -- Introducción a la Ingeniería Naval (Integradora)
-- Matemática Aplicada a la Ingeniería
(1,  22,   9, 'CURSADA',  'APROBADA'), -- Análisis Matemático II
(1,  22,   1, 'APROBADA', NULL),       -- Álgebra y Geometría Analítica
(1,  22,   2, 'APROBADA', NULL),       -- Análisis Matemático I
-- Electrotecnia y Máquinas Eléctricas
(1,  23,   9, 'CURSADA',  'APROBADA'), -- Análisis Matemático II
(1,  23,  11, 'CURSADA',  'APROBADA'), -- Física II
-- Mecánica de los Fluidos
(1,  24,   3, 'CURSADA',  'APROBADA'), -- Física I
(1,  24,   9, 'CURSADA',  'APROBADA'), -- Análisis Matemático II
(1,  24,   2, 'APROBADA', NULL),       -- Análisis Matemático I
-- Teoría del Buque II (Integradora)
(1,  25,   7, 'CURSADA',  'APROBADA'), -- Inglés I
(1,  25,  21, 'CURSADA',  'APROBADA'), -- Teoría del Buque I (Integradora)
(1,  25,  22, 'CURSADA',  'APROBADA'), -- Matemática Aplicada a la Ingeniería
(1,  25,  24, 'CURSADA',  'APROBADA'), -- Mecánica de los Fluidos
(1,  25,   9, 'APROBADA', NULL),       -- Análisis Matemático II
(1,  25,  15, 'APROBADA', NULL),       -- Dibujo Naval (Integradora)
-- Alistamiento de Buques
(1,  26,  18, 'CURSADA',  'APROBADA'), -- Análisis Estructural II
(1,  26,  19, 'CURSADA',  'APROBADA'), -- Termodinámica
(1,  26,  24, 'CURSADA',  'APROBADA'), -- Mecánica de los Fluidos
(1,  26,  15, 'APROBADA', NULL),       -- Dibujo Naval (Integradora)
-- Análisis Estructural III
(1,  27,  18, 'CURSADA',  'APROBADA'), -- Análisis Estructural II
(1,  27,  22, 'CURSADA',  'APROBADA'), -- Matemática Aplicada a la Ingeniería
(1,  27,  13, 'APROBADA', NULL),       -- Análisis Estructural I
-- Máquinas Alternativas y Turbomáquinas
(1,  28,  19, 'CURSADA',  'APROBADA'), -- Termodinámica
(1,  28,  20, 'CURSADA',  'APROBADA'), -- Mecánica Racional
-- Materiales Navales
(1,  29,  18, 'CURSADA',  'APROBADA'), -- Análisis Estructural II
(1,  29,   4, 'APROBADA', NULL),       -- Química General
-- Construcción Naval
(1,  30,  18, 'CURSADA',  'APROBADA'), -- Análisis Estructural II
(1,  30,  21, 'CURSADA',  'APROBADA'), -- Teoría del Buque I (Integradora)
(1,  30,  15, 'APROBADA', NULL),       -- Dibujo Naval (Integradora)
-- Actividad Naviera
(1,  31,  16, 'CURSADA',  'APROBADA'), -- Legislación
-- Inglés II
(1,  32,   7, 'CURSADA',  'APROBADA'), -- Inglés I
-- Plantas Eléctricas Navales
(1,  34,  23, 'APROBADA', NULL),       -- Electrotecnia y Máquinas Eléctricas
-- Plantas Propulsoras Navales
(1,  35,  26, 'CURSADA',  'APROBADA'), -- Alistamiento de Buques
(1,  35,  28, 'CURSADA',  'APROBADA'), -- Máquinas Alternativas y Turbomáquinas
(1,  35,  19, 'APROBADA', NULL),       -- Termodinámica
-- Cálculo de Estructuras de Buques
(1,  36,  27, 'CURSADA',  'APROBADA'), -- Análisis Estructural III
(1,  36,  29, 'CURSADA',  'APROBADA'), -- Materiales Navales
(1,  36,  30, 'CURSADA',  'APROBADA'), -- Construcción Naval
(1,  36,  18, 'APROBADA', NULL),       -- Análisis Estructural II
(1,  36,  22, 'APROBADA', NULL),       -- Matemática Aplicada a la Ingeniería
-- Mecánica Aplicada a las Máquinas
(1,  37,  26, 'CURSADA',  'APROBADA'), -- Alistamiento de Buques
(1,  37,  27, 'CURSADA',  'APROBADA'), -- Análisis Estructural III
(1,  37,  29, 'CURSADA',  'APROBADA'), -- Materiales Navales
(1,  37,  20, 'APROBADA', NULL),       -- Mecánica Racional
-- Proyecto de Buques (Integradora)
(1,  38,  25, 'CURSADA',  'APROBADA'), -- Teoría del Buque II (Integradora)
(1,  38,  26, 'CURSADA',  'APROBADA'), -- Alistamiento de Buques
(1,  38,  30, 'CURSADA',  'APROBADA'), -- Construcción Naval
(1,  38,  32, 'CURSADA',  'APROBADA'), -- Inglés II
(1,  38,   7, 'APROBADA', NULL),       -- Inglés I
(1,  38,  21, 'APROBADA', NULL),       -- Teoría del Buque I (Integradora)
-- Organización Industrial
(1,  39,  30, 'CURSADA',  'APROBADA'), -- Construcción Naval
-- Soldadura
(1,  40,  29, 'CURSADA',  NULL),       -- Materiales Navales
(1,  40,  22, 'APROBADA', NULL),       -- Matemática Aplicada a la Ingeniería
(1,  40,  27, NULL,       'APROBADA'), -- Análisis Estructural III
-- Análisis por Elementos Finitos
(1,  41,  27, 'CURSADA',  NULL),       -- Análisis Estructural III
(1,  41,  28, NULL,       'APROBADA'), -- Máquinas Alternativas y Turbomáquinas
-- Embarcaciones Veloces
(1,  43,  38, 'CURSADA',  'APROBADA'), -- Proyecto de Buques (Integradora)
-- Seguridad, Higiene e Ingeniería Ambiental
(1,  44,  39, 'CURSADA',  'APROBADA'), -- Organización Industrial
-- Procedimientos de Astilleros
(1,  45,  30, 'CURSADA',  'APROBADA'), -- Construcción Naval
(1,  45,  38, 'CURSADA',  'APROBADA'), -- Proyecto de Buques (Integradora)
(1,  45,  39, 'CURSADA',  'APROBADA'), -- Organización Industrial
-- Práctica Profesional Supervisada
(1,  48,  38, 'CURSADA',  'APROBADA'), -- Proyecto de Buques (Integradora)
-- Proyecto Final (Integradora)
(1,  42,   1, 'APROBADA', 'APROBADA'), -- Álgebra y Geometría Analítica
(1,  42,   2, 'APROBADA', 'APROBADA'), -- Análisis Matemático I
(1,  42,   3, 'APROBADA', 'APROBADA'), -- Física I
(1,  42,   4, 'APROBADA', 'APROBADA'), -- Química General
(1,  42,   5, 'APROBADA', 'APROBADA'), -- Introducción a la Ingeniería Naval (Integradora)
(1,  42,   6, 'APROBADA', 'APROBADA'), -- Sistemas de Representación
(1,  42,   7, 'APROBADA', 'APROBADA'), -- Inglés I
(1,  42,   8, 'APROBADA', 'APROBADA'), -- Fundamentos de Informática
(1,  42,   9, 'APROBADA', 'APROBADA'), -- Análisis Matemático II
(1,  42,  10, 'APROBADA', 'APROBADA'), -- Probabilidad y Estadística
(1,  42,  11, 'APROBADA', 'APROBADA'), -- Física II
(1,  42,  12, 'APROBADA', 'APROBADA'), -- Física III
(1,  42,  13, 'APROBADA', 'APROBADA'), -- Análisis Estructural I
(1,  42,  14, 'APROBADA', 'APROBADA'), -- Ingeniería y Sociedad
(1,  42,  15, 'APROBADA', 'APROBADA'), -- Dibujo Naval (Integradora)
(1,  42,  16, 'APROBADA', 'APROBADA'), -- Legislación
(1,  42,  17, 'APROBADA', 'APROBADA'), -- Modelización de Procesos (Electiva 1)
(1,  42,  18, 'APROBADA', 'APROBADA'), -- Análisis Estructural II
(1,  42,  19, 'APROBADA', 'APROBADA'), -- Termodinámica
(1,  42,  20, 'APROBADA', 'APROBADA'), -- Mecánica Racional
(1,  42,  21, 'APROBADA', 'APROBADA'), -- Teoría del Buque I (Integradora)
(1,  42,  22, 'APROBADA', 'APROBADA'), -- Matemática Aplicada a la Ingeniería
(1,  42,  23, 'APROBADA', 'APROBADA'), -- Electrotecnia y Máquinas Eléctricas
(1,  42,  24, 'APROBADA', 'APROBADA'), -- Mecánica de los Fluidos
(1,  42,  25, 'APROBADA', 'APROBADA'), -- Teoría del Buque II (Integradora)
(1,  42,  26, 'APROBADA', 'APROBADA'), -- Alistamiento de Buques
(1,  42,  27, 'APROBADA', 'APROBADA'), -- Análisis Estructural III
(1,  42,  28, 'APROBADA', 'APROBADA'), -- Máquinas Alternativas y Turbomáquinas
(1,  42,  29, 'APROBADA', 'APROBADA'), -- Materiales Navales
(1,  42,  30, 'APROBADA', 'APROBADA'), -- Construcción Naval
(1,  42,  31, 'APROBADA', 'APROBADA'), -- Actividad Naviera
(1,  42,  32, 'APROBADA', 'APROBADA'), -- Inglés II
(1,  42,  33, 'APROBADA', 'APROBADA'), -- Diseño Naval Asistido (Electiva 2)
(1,  42,  34, 'APROBADA', 'APROBADA'), -- Plantas Eléctricas Navales
(1,  42,  35, 'APROBADA', 'APROBADA'), -- Plantas Propulsoras Navales
(1,  42,  36, 'APROBADA', 'APROBADA'), -- Cálculo de Estructuras de Buques
(1,  42,  37, 'APROBADA', 'APROBADA'), -- Mecánica Aplicada a las Máquinas
(1,  42,  38, 'APROBADA', 'APROBADA'), -- Proyecto de Buques (Integradora)
(1,  42,  39, 'APROBADA', 'APROBADA'), -- Organización Industrial
(1,  42,  40, 'APROBADA', 'APROBADA'), -- Soldadura
(1,  42,  41, 'APROBADA', 'APROBADA'), -- Análisis por Elementos Finitos
(1,  42,  43, NULL,       'APROBADA'), -- Embarcaciones Veloces
(1,  42,  44, NULL,       'APROBADA'), -- Seguridad, Higiene e Ingeniería Ambiental
(1,  42,  45, NULL,       'APROBADA'), -- Procedimientos de Astilleros
(1,  42,  46, NULL,       'APROBADA'), -- Navegación (Electiva 3)
(1,  42,  47, NULL,       'APROBADA'), -- Buques Militares (Electiva 4)
(1,  42,  48, NULL,       'APROBADA'); -- Práctica Profesional Supervisada

-- ============================================================
-- Plan 2 - Ingenieria Pesquera
-- ============================================================
INSERT INTO correlatividad_materias
    (plan_estudio_id, plan_materia_id, plan_materia_correlativa_id, estado_para_cursar, estado_para_rendir) VALUES
-- Física II
(2,  58,  50, 'CURSADA',  'APROBADA'), -- Análisis Matemático I
(2,  58,  54, 'CURSADA',  'APROBADA'), -- Física I
-- Química Analítica e Instrumental
(2,  59,  51, 'CURSADA',  'APROBADA'), -- Química General
-- Recursos Pesqueros II (Integradora)
(2,  60,  50, 'CURSADA',  'APROBADA'), -- Análisis Matemático I
(2,  60,  51, 'CURSADA',  'APROBADA'), -- Química General
(2,  60,  52, 'CURSADA',  'APROBADA'), -- Recursos Pesqueros I (Integradora)
(2,  60,  56, 'CURSADA',  NULL),       -- Gabinete Sistemas de Representación
(2,  60,  57, 'CURSADA',  'APROBADA'), -- Gabinete Informática I
-- Análisis Matemático II
(2,  61,  49, 'CURSADA',  'APROBADA'), -- Álgebra y Geometría Analítica
(2,  61,  50, 'CURSADA',  'APROBADA'), -- Análisis Matemático I
-- Termodinámica
(2,  62,  50, 'CURSADA',  'APROBADA'), -- Análisis Matemático I
(2,  62,  58, 'CURSADA',  'APROBADA'), -- Física II
-- Estática y Resistencia de Materiales
(2,  65,  49, 'CURSADA',  'APROBADA'), -- Álgebra y Geometría Analítica
(2,  65,  50, 'CURSADA',  'APROBADA'), -- Análisis Matemático I
(2,  65,  54, 'CURSADA',  'APROBADA'), -- Física I
(2,  65,  56, 'APROBADA', NULL),       -- Gabinete Sistemas de Representación
-- Química Orgánica y Biológica
(2,  66,  59, 'CURSADA',  'APROBADA'), -- Química Analítica e Instrumental
(2,  66,  51, 'APROBADA', NULL),       -- Química General
(2,  66,  56, 'APROBADA', NULL),       -- Gabinete Sistemas de Representación
-- Tecnología del Producto Pesquero I
(2,  67,  59, 'CURSADA',  'APROBADA'), -- Química Analítica e Instrumental
(2,  67,  60, 'CURSADA',  'APROBADA'), -- Recursos Pesqueros II (Integradora)
(2,  67,  50, 'APROBADA', NULL),       -- Análisis Matemático I
(2,  67,  51, 'APROBADA', NULL),       -- Química General
(2,  67,  52, 'APROBADA', NULL),       -- Recursos Pesqueros I (Integradora)
(2,  67,  54, 'APROBADA', NULL),       -- Física I
(2,  67,  64, 'APROBADA', NULL),       -- Gabinete de Idiomas I
(2,  67,  56, 'APROBADA', NULL),       -- Gabinete Sistemas de Representación
-- Higiene y Seguridad Industrial
(2,  68,  58, 'CURSADA',  'APROBADA'), -- Física II
(2,  68,  59, 'CURSADA',  'APROBADA'), -- Química Analítica e Instrumental
(2,  68,  50, 'APROBADA', NULL),       -- Análisis Matemático I
(2,  68,  51, 'APROBADA', NULL),       -- Química General
(2,  68,  54, 'APROBADA', NULL),       -- Física I
(2,  68,  56, 'APROBADA', NULL),       -- Gabinete Sistemas de Representación
-- Probabilidad y Estadística
(2,  69,  49, 'CURSADA',  'APROBADA'), -- Álgebra y Geometría Analítica
(2,  69,  50, 'CURSADA',  'APROBADA'), -- Análisis Matemático I
(2,  69,  56, 'APROBADA', NULL),       -- Gabinete Sistemas de Representación
-- Tecnología del Producto Pesquero II (Integradora)
(2,  70,  59, 'CURSADA',  'APROBADA'), -- Química Analítica e Instrumental
(2,  70,  60, 'CURSADA',  'APROBADA'), -- Recursos Pesqueros II (Integradora)
(2,  70,  67, 'CURSADA',  'APROBADA'), -- Tecnología del Producto Pesquero I
(2,  70,  68, 'CURSADA',  'APROBADA'), -- Higiene y Seguridad Industrial
(2,  70,  51, 'APROBADA', NULL),       -- Química General
(2,  70,  52, 'APROBADA', NULL),       -- Recursos Pesqueros I (Integradora)
(2,  70,  56, 'APROBADA', NULL),       -- Gabinete Sistemas de Representación
-- Bioquímica de Alimentos
(2,  71,  66, 'CURSADA',  'APROBADA'), -- Química Orgánica y Biológica
(2,  71,  59, 'APROBADA', NULL),       -- Química Analítica e Instrumental
(2,  71,  56, 'APROBADA', NULL),       -- Gabinete Sistemas de Representación
-- Equipos e Instalaciones Industriales
(2,  72,  65, 'CURSADA',  'APROBADA'), -- Estática y Resistencia de Materiales
(2,  72,  58, 'APROBADA', NULL),       -- Física II
(2,  72,  56, 'APROBADA', NULL),       -- Gabinete Sistemas de Representación
-- Acuicultura I
(2,  73,  60, 'CURSADA',  'APROBADA'), -- Recursos Pesqueros II (Integradora)
(2,  73,  52, 'APROBADA', NULL),       -- Recursos Pesqueros I (Integradora)
(2,  73,  56, 'APROBADA', NULL),       -- Gabinete Sistemas de Representación
-- Tecnología del Producto Pesquero III (Integradora)
(2,  76,  70, 'CURSADA',  'APROBADA'), -- Tecnología del Producto Pesquero II (Integradora)
(2,  76,  71, 'CURSADA',  'APROBADA'), -- Bioquímica de Alimentos
(2,  76,  59, 'APROBADA', NULL),       -- Química Analítica e Instrumental
(2,  76,  66, 'APROBADA', NULL),       -- Química Orgánica y Biológica
(2,  76,  67, 'APROBADA', NULL),       -- Tecnología del Producto Pesquero I
(2,  76,  74, 'APROBADA', NULL),       -- Gabinete de Idiomas II
-- Operaciones Unitarias I
(2,  77,  72, 'CURSADA',  'APROBADA'), -- Equipos e Instalaciones Industriales
(2,  77,  62, 'APROBADA', NULL),       -- Termodinámica
-- Materiales y Envases de la Industria Pesquera
(2,  78,  71, 'CURSADA',  'APROBADA'), -- Bioquímica de Alimentos
(2,  78,  60, 'APROBADA', NULL),       -- Recursos Pesqueros II (Integradora)
-- Gestión de Calidad
(2,  79,  69, 'CURSADA',  'APROBADA'), -- Probabilidad y Estadística
(2,  79,  70, 'CURSADA',  'APROBADA'), -- Tecnología del Producto Pesquero II (Integradora)
(2,  79,  71, 'CURSADA',  'APROBADA'), -- Bioquímica de Alimentos
(2,  79,  61, 'APROBADA', NULL),       -- Análisis Matemático II
(2,  79,  66, 'APROBADA', NULL),       -- Química Orgánica y Biológica
(2,  79,  67, 'APROBADA', NULL),       -- Tecnología del Producto Pesquero I
(2,  79,  68, 'APROBADA', NULL),       -- Higiene y Seguridad Industrial
-- Matemática Superior
(2,  80,  61, 'APROBADA', NULL),       -- Análisis Matemático II
-- Bromatología
(2,  81,  71, 'CURSADA',  'APROBADA'), -- Bioquímica de Alimentos
(2,  81,  66, 'APROBADA', NULL),       -- Química Orgánica y Biológica
(2,  81,  68, 'APROBADA', NULL),       -- Higiene y Seguridad Industrial
-- Operaciones Unitarias II
(2,  82,  77, 'CURSADA',  'APROBADA'), -- Operaciones Unitarias I
(2,  82,  72, 'APROBADA', NULL),       -- Equipos e Instalaciones Industriales
-- Proyecto Integrador I (Integradora)
(2,  83,  70, 'CURSADA',  'APROBADA'), -- Tecnología del Producto Pesquero II (Integradora)
(2,  83,  71, 'CURSADA',  'APROBADA'), -- Bioquímica de Alimentos
(2,  83,  72, 'CURSADA',  'APROBADA'), -- Equipos e Instalaciones Industriales
(2,  83,  65, 'APROBADA', NULL),       -- Estática y Resistencia de Materiales
(2,  83,  66, 'APROBADA', NULL),       -- Química Orgánica y Biológica
(2,  83,  67, 'APROBADA', NULL),       -- Tecnología del Producto Pesquero I
-- Economía
(2,  85,  60, 'CURSADA',  'APROBADA'), -- Recursos Pesqueros II (Integradora)
-- Programación y Control de la Producción
(2,  86,  76, 'CURSADA',  'APROBADA'), -- Tecnología del Producto Pesquero III (Integradora)
(2,  86,  66, 'APROBADA', NULL),       -- Química Orgánica y Biológica
(2,  86,  69, 'APROBADA', NULL),       -- Probabilidad y Estadística
-- Legislación
(2,  87,  60, 'CURSADA',  'APROBADA'), -- Recursos Pesqueros II (Integradora)
-- Proyecto Integrador II (Integradora)
(2,  88,  76, 'CURSADA',  'APROBADA'), -- Tecnología del Producto Pesquero III (Integradora)
(2,  88,  79, 'CURSADA',  'APROBADA'), -- Gestión de Calidad
(2,  88,  82, 'CURSADA',  'APROBADA'), -- Operaciones Unitarias II
(2,  88,  83, 'CURSADA',  'APROBADA'), -- Proyecto Integrador I (Integradora)
(2,  88,  70, 'APROBADA', NULL),       -- Tecnología del Producto Pesquero II (Integradora)
(2,  88,  71, 'APROBADA', NULL),       -- Bioquímica de Alimentos
(2,  88,  72, 'APROBADA', NULL),       -- Equipos e Instalaciones Industriales
(2,  88,  77, 'APROBADA', NULL),       -- Operaciones Unitarias I
-- Maquinas e Instalaciones Térmicas
(2,  89,  62, 'CURSADA',  'APROBADA'), -- Termodinámica
(2,  89,  65, 'CURSADA',  'APROBADA'), -- Estática y Resistencia de Materiales
(2,  89,  49, 'APROBADA', NULL),       -- Álgebra y Geometría Analítica
(2,  89,  50, 'APROBADA', NULL),       -- Análisis Matemático I
(2,  89,  54, 'APROBADA', NULL),       -- Física I
(2,  89,  58, 'APROBADA', NULL),       -- Física II
-- Investigación Operativa
(2,  90,  80, 'CURSADA',  'APROBADA'), -- Matemática Superior
(2,  90,  61, 'APROBADA', NULL),       -- Análisis Matemático II
(2,  90,  69, 'APROBADA', NULL),       -- Probabilidad y Estadística
(2,  90,  75, 'APROBADA', NULL),       -- Gabinete de Informática II
-- Acuicultura II
(2,  91,  65, 'CURSADA',  'APROBADA'), -- Estática y Resistencia de Materiales
(2,  91,  73, 'CURSADA',  'APROBADA'); -- Acuicultura I

-- ============================================================
-- Plan 3 - TUP - Plan 2024
-- ============================================================
INSERT INTO correlatividad_materias
    (plan_estudio_id, plan_materia_id, plan_materia_correlativa_id, estado_para_cursar, estado_para_rendir) VALUES
-- Programación II
(3,  99,  95, 'CURSADA',  'CURSADA'),  -- Programación I
(3,  99,  96, 'CURSADA',  'CURSADA'),  -- Arquitectura y Sistemas Operativos
-- Probabilidad y Estadística
(3, 100,  97, 'CURSADA',  'CURSADA'),  -- Matemática
-- Base de Datos I
(3, 101,  95, 'CURSADA',  'CURSADA'),  -- Programación I
(3, 101,  97, 'CURSADA',  'CURSADA'),  -- Matemática
-- Programación III
(3, 103,  99, 'CURSADA',  'CURSADA'),  -- Programación II
(3, 103, 101, 'CURSADA',  'CURSADA'),  -- Base de Datos I
(3, 103,  95, 'APROBADA', 'APROBADA'), -- Programación I
-- Base de Datos II
(3, 104, 101, 'CURSADA',  'CURSADA'),  -- Base de Datos I
(3, 104,  95, 'APROBADA', 'APROBADA'), -- Programación I
-- Metodología de Sistemas I
(3, 105,  99, 'CURSADA',  'CURSADA'),  -- Programación II
(3, 105, 101, 'CURSADA',  'CURSADA'),  -- Base de Datos I
(3, 105,  95, 'APROBADA', 'APROBADA'), -- Programación I
(3, 105,  98, 'APROBADA', 'APROBADA'), -- Organización Empresarial
-- Inglés II
(3, 106, 102, 'CURSADA',  'CURSADA'),  -- Inglés I
-- Programación IV
(3, 107, 103, 'CURSADA',  'CURSADA'),  -- Programación III
(3, 107, 104, 'CURSADA',  'CURSADA'),  -- Base de Datos II
(3, 107, 105, 'CURSADA',  'CURSADA'),  -- Metodología de Sistemas I
(3, 107,  99, 'APROBADA', 'APROBADA'), -- Programación II
(3, 107, 101, 'APROBADA', 'APROBADA'), -- Base de Datos I
-- Metodología de Sistemas II
(3, 108, 105, 'CURSADA',  'CURSADA'),  -- Metodología de Sistemas I
(3, 108,  98, 'APROBADA', 'APROBADA'), -- Organización Empresarial
-- Introducción al análisis de datos
(3, 109, 104, 'CURSADA',  'CURSADA'),  -- Base de Datos II
(3, 109, 100, 'APROBADA', 'APROBADA'), -- Probabilidad y Estadística
(3, 109, 101, 'APROBADA', 'APROBADA'), -- Base de Datos I
-- Legislación
(3, 110, 101, 'CURSADA',  'CURSADA'),  -- Base de Datos I
(3, 110,  98, 'APROBADA', 'APROBADA'), -- Organización Empresarial
-- Gestión de desarrollo de software
(3, 111, 103, 'CURSADA',  'CURSADA'),  -- Programación III
(3, 111, 104, 'CURSADA',  'CURSADA'),  -- Base de Datos II
(3, 111,  99, 'APROBADA', 'APROBADA'), -- Programación II
(3, 111, 101, 'APROBADA', 'APROBADA'), -- Base de Datos I
-- Trabajo Final Integrador
(3, 112,  95, NULL,       'APROBADA'), -- Programación I
(3, 112,  96, NULL,       'APROBADA'), -- Arquitectura y Sistemas Operativos
(3, 112,  97, NULL,       'APROBADA'), -- Matemática
(3, 112,  98, NULL,       'APROBADA'), -- Organización Empresarial
(3, 112,  99, NULL,       'APROBADA'), -- Programación II
(3, 112, 100, NULL,       'APROBADA'), -- Probabilidad y Estadística
(3, 112, 101, NULL,       'APROBADA'), -- Base de Datos I
(3, 112, 102, 'CURSADA',  'APROBADA'), -- Inglés I
(3, 112, 103, 'CURSADA',  'APROBADA'), -- Programación III
(3, 112, 104, 'CURSADA',  'APROBADA'), -- Base de Datos II
(3, 112, 105, 'CURSADA',  'APROBADA'), -- Metodología de Sistemas I
(3, 112, 106, 'CURSADA',  'APROBADA'), -- Inglés II
(3, 112, 107, NULL,       'APROBADA'), -- Programación IV
(3, 112, 108, NULL,       'APROBADA'), -- Metodología de Sistemas II
(3, 112, 109, NULL,       'APROBADA'), -- Introducción al análisis de datos
(3, 112, 110, NULL,       'APROBADA'), -- Legislación
(3, 112, 111, NULL,       'APROBADA'); -- Gestión de desarrollo de software

-- ============================================================
-- Plan 4 - TUP - Plan 2003
-- ============================================================
INSERT INTO correlatividad_materias
    (plan_estudio_id, plan_materia_id, plan_materia_correlativa_id, estado_para_cursar, estado_para_rendir) VALUES
-- Programación II
(4, 118, 113, 'CURSADA',  'APROBADA'), -- Programación I
(4, 118, 117, 'CURSADA',  'APROBADA'), -- Laboratorio de Computación I
-- Arquitectura y Sistemas Operativos
(4, 119, 114, 'CURSADA',  'APROBADA'), -- Sistemas de Procesamiento de Datos
-- Estadística
(4, 120, 115, 'CURSADA',  'APROBADA'), -- Matemática
-- Inglés II
(4, 121, 116, 'CURSADA',  'APROBADA'), -- Inglés I
-- Laboratorio de Computación II
(4, 122, 113, 'CURSADA',  'APROBADA'), -- Programación I
(4, 122, 117, 'CURSADA',  'APROBADA'), -- Laboratorio de Computación I
-- Programación III
(4, 124, 118, 'CURSADA',  'APROBADA'), -- Programación II
(4, 124, 122, 'CURSADA',  'APROBADA'), -- Laboratorio de Computación II
(4, 124, 113, 'APROBADA', NULL),       -- Programación I
(4, 124, 117, 'APROBADA', NULL),       -- Laboratorio de Computación I
-- Organización Contable de la Empresa
(4, 125, 115, 'CURSADA',  'APROBADA'), -- Matemática
-- Organización Empresarial
(4, 126, 120, 'CURSADA',  'APROBADA'), -- Estadística
-- Elementos de la Investigación Operativa
(4, 127, 120, 'CURSADA',  'APROBADA'), -- Estadística
(4, 127, 115, 'APROBADA', NULL),       -- Matemática
-- Laboratorio de Computación III
(4, 128, 118, 'CURSADA',  'APROBADA'), -- Programación II
(4, 128, 122, 'CURSADA',  'APROBADA'), -- Laboratorio de Computación II
(4, 128, 113, 'APROBADA', NULL),       -- Programación I
(4, 128, 117, 'APROBADA', NULL),       -- Laboratorio de Computación I
-- Metodología de Sistemas I
(4, 129, 123, 'CURSADA',  'APROBADA'), -- Metodología de la Investigación
(4, 129, 124, 'CURSADA',  'APROBADA'), -- Programación III
(4, 129, 125, 'CURSADA',  'APROBADA'), -- Organización Contable de la Empresa
(4, 129, 126, 'CURSADA',  'APROBADA'), -- Organización Empresarial
(4, 129, 128, 'CURSADA',  'APROBADA'), -- Laboratorio de Computación III
(4, 129, 118, 'APROBADA', NULL),       -- Programación II
(4, 129, 122, 'APROBADA', NULL),       -- Laboratorio de Computación II
-- Diseño y Administración de Base de Datos
(4, 130, 124, 'CURSADA',  'APROBADA'), -- Programación III
(4, 130, 128, 'CURSADA',  'APROBADA'), -- Laboratorio de Computación III
(4, 130, 118, 'APROBADA', NULL),       -- Programación II
(4, 130, 122, 'APROBADA', NULL),       -- Laboratorio de Computación II
-- Laboratorio de Computación IV
(4, 132, 124, 'CURSADA',  'APROBADA'), -- Programación III
(4, 132, 128, 'CURSADA',  'APROBADA'), -- Laboratorio de Computación III
(4, 132, 118, 'APROBADA', NULL),       -- Programación II
(4, 132, 122, 'APROBADA', NULL);       -- Laboratorio de Computación II

-- ============================================================
-- Plan 5 - Tecnicatura Superior en Interiorismo
-- ============================================================
-- Los códigos son los del plan de estudios (77.10.xxx, con guiones porque chk_pm_codigo no acepta
-- puntos). El PDF de correlatividades numera distinto a cuatro materias: Diseño I "B" (301),
-- Diseño II "B" (501), Práctica Profesional (509) y Proyecto Final (510); sus filas se cargaron
-- por el nombre de la materia. "TODAS" (Proyecto Final, para rendir) es el resto del plan.
INSERT INTO correlatividad_materias
    (plan_estudio_id, plan_materia_id, plan_materia_correlativa_id, estado_para_cursar, estado_para_rendir) VALUES
-- Diseño I "A"
(5, 139, 134, 'CURSADA',  'APROBADA'), -- Introducción al Diseño
-- Dibujo Técnico II
(5, 140, 136, 'CURSADA',  'APROBADA'), -- Dibujo Técnico I
-- Historia del Diseño I
(5, 141, 138, 'CURSADA',  'APROBADA'), -- Introducción a la Historia del Diseño
-- Técnicas Constructivas I
(5, 142, 135, 'CURSADA',  'APROBADA'), -- Nociones Básicas de Física y Matemática Aplicada
-- Diseño I "B"
(5, 144, 134, 'CURSADA',  NULL),       -- Introducción al Diseño
(5, 144, 139, 'CURSADA',  'APROBADA'), -- Diseño I "A"
-- Historia del Diseño II
(5, 145, 138, 'CURSADA',  'APROBADA'), -- Introducción a la Historia del Diseño
-- Dibujo Técnico III
(5, 146, 140, 'CURSADA',  'APROBADA'), -- Dibujo Técnico II
(5, 146, 136, 'APROBADA', NULL),       -- Dibujo Técnico I
-- Comunicación Visual II
(5, 147, 137, 'CURSADA',  'APROBADA'), -- Comunicación Visual I
-- Técnicas Constructivas II
(5, 148, 142, 'CURSADA',  'APROBADA'), -- Técnicas Constructivas I
(5, 148, 135, 'APROBADA', NULL),       -- Nociones Básicas de Física y Matemática Aplicada
-- Diseño II "A"
(5, 149, 139, 'CURSADA',  'APROBADA'), -- Diseño I "A"
(5, 149, 134, 'APROBADA', NULL),       -- Introducción al Diseño
-- Comunicación Visual III
(5, 150, 147, 'CURSADA',  'APROBADA'), -- Comunicación Visual II
(5, 150, 136, 'APROBADA', NULL),       -- Dibujo Técnico I
-- Técnicas Constructivas III
(5, 151, 148, 'CURSADA',  'APROBADA'), -- Técnicas Constructivas II
(5, 151, 142, 'APROBADA', NULL),       -- Técnicas Constructivas I
-- Seminario Integrador II
(5, 152, 134, NULL,       'APROBADA'), -- Introducción al Diseño
(5, 152, 135, NULL,       'APROBADA'), -- Nociones Básicas de Física y Matemática Aplicada
(5, 152, 136, NULL,       'APROBADA'), -- Dibujo Técnico I
(5, 152, 137, NULL,       'APROBADA'), -- Comunicación Visual I
(5, 152, 138, NULL,       'APROBADA'), -- Introducción a la Historia del Diseño
-- Lenguajes Digitales
(5, 153, 141, 'CURSADA',  'APROBADA'), -- Historia del Diseño I
(5, 153, 140, 'APROBADA', NULL),       -- Dibujo Técnico II
-- Diseño II "B"
(5, 154, 149, 'CURSADA',  'APROBADA'), -- Diseño II "A"
(5, 154, 144, 'APROBADA', NULL),       -- Diseño I "B"
-- Práctica Profesional
(5, 155, 139, 'CURSADA',  'APROBADA'), -- Diseño I "A"
(5, 155, 134, 'APROBADA', NULL),       -- Introducción al Diseño
-- Proyecto Final
(5, 156, 149, 'CURSADA',  'APROBADA'), -- Diseño II "A"
(5, 156, 150, 'CURSADA',  'APROBADA'), -- Comunicación Visual III
(5, 156, 134, 'APROBADA', 'APROBADA'), -- Introducción al Diseño
(5, 156, 135, 'APROBADA', 'APROBADA'), -- Nociones Básicas de Física y Matemática Aplicada
(5, 156, 136, 'APROBADA', 'APROBADA'), -- Dibujo Técnico I
(5, 156, 137, 'APROBADA', 'APROBADA'), -- Comunicación Visual I
(5, 156, 138, 'APROBADA', 'APROBADA'), -- Introducción a la Historia del Diseño
(5, 156, 139, NULL,       'APROBADA'), -- Diseño I "A"
(5, 156, 140, NULL,       'APROBADA'), -- Dibujo Técnico II
(5, 156, 141, NULL,       'APROBADA'), -- Historia del Diseño I
(5, 156, 142, NULL,       'APROBADA'), -- Técnicas Constructivas I
(5, 156, 143, NULL,       'APROBADA'), -- Seminario Integrador I
(5, 156, 144, NULL,       'APROBADA'), -- Diseño I "B"
(5, 156, 145, NULL,       'APROBADA'), -- Historia del Diseño II
(5, 156, 146, NULL,       'APROBADA'), -- Dibujo Técnico III
(5, 156, 147, NULL,       'APROBADA'), -- Comunicación Visual II
(5, 156, 148, NULL,       'APROBADA'), -- Técnicas Constructivas II
(5, 156, 151, NULL,       'APROBADA'), -- Técnicas Constructivas III
(5, 156, 152, NULL,       'APROBADA'), -- Seminario Integrador II
(5, 156, 153, NULL,       'APROBADA'), -- Lenguajes Digitales
(5, 156, 154, NULL,       'APROBADA'), -- Diseño II "B"
(5, 156, 155, NULL,       'APROBADA'), -- Práctica Profesional
(5, 156, 157, NULL,       'APROBADA'), -- Seminario Integrador III
(5, 156, 158, NULL,       'APROBADA'), -- Legislación
(5, 156, 159, NULL,       'APROBADA'), -- Marketing
-- Seminario Integrador III
(5, 157, 141, 'CURSADA',  'APROBADA'), -- Historia del Diseño I
(5, 157, 151, 'CURSADA',  'APROBADA'), -- Técnicas Constructivas III
(5, 157, 138, NULL,       'APROBADA'), -- Introducción a la Historia del Diseño
(5, 157, 139, NULL,       'APROBADA'), -- Diseño I "A"
(5, 157, 142, NULL,       'APROBADA'), -- Técnicas Constructivas I
(5, 157, 147, NULL,       'APROBADA'); -- Comunicación Visual II

-- ============================================================
-- Plan 6 - TUA - Plan 2025
-- ============================================================
-- Formato "para cursar y rendir": se carga como la TUP 2024 (cursada -> CURSADA/CURSADA,
-- aprobada -> APROBADA/APROBADA). Proyecto Final: pide Seminario cursada para empezar y todas
-- las demás aprobadas para aprobarlo, igual que el Trabajo Final Integrador de la TUP 2024.
INSERT INTO correlatividad_materias
    (plan_estudio_id, plan_materia_id, plan_materia_correlativa_id, estado_para_cursar, estado_para_rendir) VALUES
-- Administración General II
(6, 170, 160, 'CURSADA',  'CURSADA'),  -- Administración General I
-- Informática II
(6, 171, 162, 'CURSADA',  'CURSADA'),  -- Informática I
-- Comportamiento Organizacional
(6, 172, 163, 'CURSADA',  'CURSADA'),  -- Problemática Social Contemporánea
(6, 172, 165, 'CURSADA',  'CURSADA'),  -- Psicología de las Organizaciones
-- Estadística
(6, 173, 161, 'CURSADA',  'CURSADA'),  -- Matemática General
(6, 173, 162, 'CURSADA',  'CURSADA'),  -- Informática I
-- Economía II
(6, 174, 161, 'CURSADA',  'CURSADA'),  -- Matemática General
(6, 174, 164, 'CURSADA',  'CURSADA'),  -- Economía I
-- Legislación II
(6, 175, 160, 'CURSADA',  'CURSADA'),  -- Administración General I
(6, 175, 167, 'CURSADA',  'CURSADA'),  -- Legislación I
-- Administración Estatal
(6, 176, 160, 'CURSADA',  'CURSADA'),  -- Administración General I
(6, 176, 167, 'APROBADA', 'APROBADA'), -- Legislación I
(6, 176, 168, 'APROBADA', 'APROBADA'), -- Estado y Sociedad
-- Gestión de Recursos Humanos
(6, 177, 160, 'CURSADA',  'CURSADA'),  -- Administración General I
(6, 177, 163, 'CURSADA',  'CURSADA'),  -- Problemática Social Contemporánea
(6, 177, 167, 'CURSADA',  'CURSADA'),  -- Legislación I
(6, 177, 165, 'APROBADA', 'APROBADA'), -- Psicología de las Organizaciones
(6, 177, 166, 'APROBADA', 'APROBADA'), -- Ambiente, Economía y Sociedad
-- Finanzas y Técnicas Contables
(6, 178, 162, 'CURSADA',  'CURSADA'),  -- Informática I
(6, 178, 161, 'APROBADA', 'APROBADA'), -- Matemática General
-- Proyecto Final
(6, 179, 160, NULL,       'APROBADA'), -- Administración General I
(6, 179, 161, NULL,       'APROBADA'), -- Matemática General
(6, 179, 162, NULL,       'APROBADA'), -- Informática I
(6, 179, 163, NULL,       'APROBADA'), -- Problemática Social Contemporánea
(6, 179, 164, NULL,       'APROBADA'), -- Economía I
(6, 179, 165, NULL,       'APROBADA'), -- Psicología de las Organizaciones
(6, 179, 166, NULL,       'APROBADA'), -- Ambiente, Economía y Sociedad
(6, 179, 167, NULL,       'APROBADA'), -- Legislación I
(6, 179, 168, NULL,       'APROBADA'), -- Estado y Sociedad
(6, 179, 169, 'CURSADA',  'APROBADA'), -- Seminario
(6, 179, 170, NULL,       'APROBADA'), -- Administración General II
(6, 179, 171, NULL,       'APROBADA'), -- Informática II
(6, 179, 172, NULL,       'APROBADA'), -- Comportamiento Organizacional
(6, 179, 173, NULL,       'APROBADA'), -- Estadística
(6, 179, 174, NULL,       'APROBADA'), -- Economía II
(6, 179, 175, NULL,       'APROBADA'), -- Legislación II
(6, 179, 176, NULL,       'APROBADA'), -- Administración Estatal
(6, 179, 177, NULL,       'APROBADA'), -- Gestión de Recursos Humanos
(6, 179, 178, NULL,       'APROBADA'); -- Finanzas y Técnicas Contables

-- ============================================================
-- Plan 7 - Tecnicatura Universitaria en Procedimientos y Tecnologías Ambientales
-- ============================================================
-- Problemática Ambiental Regional y Nacional: el PDF dice "11-11-12-16" para cursar; se tomó
-- 10-11-12-16, que es lo que pide para rendir. La PPS / Trabajo Final pide para empezar
-- todas las del segundo año aprobadas, y nada para rendir.
INSERT INTO correlatividad_materias
    (plan_estudio_id, plan_materia_id, plan_materia_correlativa_id, estado_para_cursar, estado_para_rendir) VALUES
-- Ecología General
(7, 184, 180, 'CURSADA',  'APROBADA'), -- Biología General
(7, 184, 181, 'CURSADA',  NULL),       -- Ciencias de la Tierra
-- Química Ambiental
(7, 185, 182, 'CURSADA',  'APROBADA'), -- Físico-Química General
-- Física
(7, 186, 182, 'CURSADA',  'APROBADA'), -- Físico-Química General
(7, 186, 183, 'CURSADA',  'APROBADA'), -- Matemática
-- Probabilidad y Estadística
(7, 187, 183, 'CURSADA',  'APROBADA'), -- Matemática
-- Principios de Derecho y Normativa Ambiental
(7, 188, 181, 'CURSADA',  'APROBADA'), -- Ciencias de la Tierra
-- Calidad de Aguas Superficiales y Subterráneas
(7, 189, 181, 'CURSADA',  'APROBADA'), -- Ciencias de la Tierra
(7, 189, 185, 'CURSADA',  'APROBADA'), -- Química Ambiental
(7, 189, 182, 'APROBADA', NULL),       -- Físico-Química General
-- Sistema de Tratamiento de Residuos y Efluentes
(7, 190, 181, 'CURSADA',  'APROBADA'), -- Ciencias de la Tierra
(7, 190, 185, 'CURSADA',  'APROBADA'), -- Química Ambiental
(7, 190, 180, 'APROBADA', NULL),       -- Biología General
(7, 190, 182, 'APROBADA', NULL),       -- Físico-Química General
-- Contaminación Atmosférica
(7, 191, 181, 'CURSADA',  'APROBADA'), -- Ciencias de la Tierra
(7, 191, 185, 'CURSADA',  'APROBADA'), -- Química Ambiental
(7, 191, 186, 'CURSADA',  'APROBADA'), -- Física
(7, 191, 182, 'APROBADA', NULL),       -- Físico-Química General
(7, 191, 183, 'APROBADA', NULL),       -- Matemática
-- Gabinete de Electrónica
(7, 192, 186, 'CURSADA',  'APROBADA'), -- Física
(7, 192, 182, 'APROBADA', NULL),       -- Físico-Química General
(7, 192, 183, 'APROBADA', NULL),       -- Matemática
-- Sistemas de Control y Gestión Ambiental
(7, 193, 188, 'CURSADA',  'APROBADA'), -- Principios de Derecho y Normativa Ambiental
(7, 193, 181, 'APROBADA', NULL),       -- Ciencias de la Tierra
-- Tecnologías de Muestreo, Medición y Control Ambiental
(7, 194, 189, 'CURSADA',  'APROBADA'), -- Calidad de Aguas Superficiales y Subterráneas
(7, 194, 190, 'CURSADA',  'APROBADA'), -- Sistema de Tratamiento de Residuos y Efluentes
(7, 194, 191, 'CURSADA',  'APROBADA'), -- Contaminación Atmosférica
(7, 194, 192, 'CURSADA',  'APROBADA'), -- Gabinete de Electrónica
(7, 194, 181, 'APROBADA', NULL),       -- Ciencias de la Tierra
(7, 194, 186, 'APROBADA', NULL),       -- Física
(7, 194, 187, 'APROBADA', NULL),       -- Probabilidad y Estadística
-- Energía y Medio Ambiente
(7, 195, 188, 'CURSADA',  'APROBADA'), -- Principios de Derecho y Normativa Ambiental
(7, 195, 191, 'CURSADA',  'APROBADA'), -- Contaminación Atmosférica
(7, 195, 181, 'APROBADA', NULL),       -- Ciencias de la Tierra
(7, 195, 185, 'APROBADA', NULL),       -- Química Ambiental
(7, 195, 186, 'APROBADA', NULL),       -- Física
-- Gabinete de informática
(7, 196, 187, 'CURSADA',  'APROBADA'), -- Probabilidad y Estadística
(7, 196, 183, 'APROBADA', NULL),       -- Matemática
-- Evaluación de Impacto y Auditorias Ambientales
(7, 198, 188, 'CURSADA',  'APROBADA'), -- Principios de Derecho y Normativa Ambiental
(7, 198, 193, 'CURSADA',  'APROBADA'), -- Sistemas de Control y Gestión Ambiental
(7, 198, 181, 'APROBADA', NULL),       -- Ciencias de la Tierra
(7, 198, 184, 'APROBADA', NULL),       -- Ecología General
(7, 198, 185, 'APROBADA', NULL),       -- Química Ambiental
(7, 198, 187, 'APROBADA', NULL),       -- Probabilidad y Estadística
-- Seguridad e Higiene Laboral
(7, 199, 193, 'CURSADA',  'APROBADA'), -- Sistemas de Control y Gestión Ambiental
(7, 199, 188, 'APROBADA', NULL),       -- Principios de Derecho y Normativa Ambiental
-- Taller de Elaboración de Informes Técnicos
(7, 200, 196, 'CURSADA',  'APROBADA'), -- Gabinete de informática
(7, 200, 197, 'CURSADA',  'APROBADA'), -- Inglés I
(7, 200, 181, 'APROBADA', NULL),       -- Ciencias de la Tierra
(7, 200, 184, 'APROBADA', NULL),       -- Ecología General
(7, 200, 185, 'APROBADA', NULL),       -- Química Ambiental
(7, 200, 187, 'APROBADA', NULL),       -- Probabilidad y Estadística
-- Cartografía y GIS
(7, 201, 196, 'CURSADA',  'APROBADA'), -- Gabinete de informática
(7, 201, 181, 'APROBADA', NULL),       -- Ciencias de la Tierra
(7, 201, 187, 'APROBADA', NULL),       -- Probabilidad y Estadística
-- Problemática Ambiental Regional y Nacional
(7, 202, 189, 'CURSADA',  'APROBADA'), -- Calidad de Aguas Superficiales y Subterráneas
(7, 202, 190, 'CURSADA',  'APROBADA'), -- Sistema de Tratamiento de Residuos y Efluentes
(7, 202, 191, 'CURSADA',  'APROBADA'), -- Contaminación Atmosférica
(7, 202, 195, 'CURSADA',  'APROBADA'), -- Energía y Medio Ambiente
(7, 202, 181, 'APROBADA', NULL),       -- Ciencias de la Tierra
(7, 202, 185, 'APROBADA', NULL),       -- Química Ambiental
(7, 202, 188, 'APROBADA', NULL),       -- Principios de Derecho y Normativa Ambiental
-- Inglés II
(7, 203, 197, 'CURSADA',  'APROBADA'), -- Inglés I
-- Práctica Profesional Supervisada / Trabajo Final Integrador
(7, 204, 188, 'APROBADA', NULL),       -- Principios de Derecho y Normativa Ambiental
(7, 204, 189, 'APROBADA', NULL),       -- Calidad de Aguas Superficiales y Subterráneas
(7, 204, 190, 'APROBADA', NULL),       -- Sistema de Tratamiento de Residuos y Efluentes
(7, 204, 191, 'APROBADA', NULL),       -- Contaminación Atmosférica
(7, 204, 192, 'APROBADA', NULL),       -- Gabinete de Electrónica
(7, 204, 193, 'APROBADA', NULL),       -- Sistemas de Control y Gestión Ambiental
(7, 204, 194, 'APROBADA', NULL),       -- Tecnologías de Muestreo, Medición y Control Ambiental
(7, 204, 195, 'APROBADA', NULL),       -- Energía y Medio Ambiente
(7, 204, 196, 'APROBADA', NULL),       -- Gabinete de informática
(7, 204, 197, 'APROBADA', NULL);       -- Inglés I

-- ============================================================
-- Plan 8 - Tecnicatura Universitaria en Producción Textil
-- ============================================================
-- Seminario Integrador V pide "TODAS" para rendir: el resto del plan.
INSERT INTO correlatividad_materias
    (plan_estudio_id, plan_materia_id, plan_materia_correlativa_id, estado_para_cursar, estado_para_rendir) VALUES
-- Física I
(8, 212, 205, 'CURSADA',  'APROBADA'), -- Matemáticas
-- Química Orgánica
(8, 213, 206, 'CURSADA',  'APROBADA'), -- Química General
-- Dibujo Técnico
(8, 215, 207, 'CURSADA',  'APROBADA'), -- Medios expresivos I
-- Física II
(8, 217, 212, 'CURSADA',  'APROBADA'), -- Física I
(8, 217, 205, 'APROBADA', NULL),       -- Matemáticas
-- Química Textil I
(8, 218, 210, 'CURSADA',  'APROBADA'), -- Fundamentos Textiles
(8, 218, 212, 'CURSADA',  'APROBADA'), -- Física I
(8, 218, 213, 'CURSADA',  'APROBADA'), -- Química Orgánica
(8, 218, 206, 'APROBADA', NULL),       -- Química General
-- Medios expresivos II
(8, 219, 215, 'CURSADA',  'APROBADA'), -- Dibujo Técnico
(8, 219, 207, 'APROBADA', NULL),       -- Medios expresivos I
-- Procesos Productivos Industriales
(8, 221, 210, 'CURSADA',  'APROBADA'), -- Fundamentos Textiles
(8, 221, 211, 'CURSADA',  'APROBADA'), -- Sistemas Productivos I
(8, 221, 215, 'CURSADA',  'APROBADA'), -- Dibujo Técnico
-- Sistemas Productivos II
(8, 222, 210, 'CURSADA',  'APROBADA'), -- Fundamentos Textiles
(8, 222, 211, 'CURSADA',  'APROBADA'), -- Sistemas Productivos I
-- Seminario Integrador III
(8, 220, 210, 'CURSADA',  'APROBADA'), -- Fundamentos Textiles
(8, 220, 211, 'CURSADA',  'APROBADA'), -- Sistemas Productivos I
(8, 220, 215, 'CURSADA',  'APROBADA'), -- Dibujo Técnico
(8, 220, 209, 'APROBADA', NULL),       -- Seminario Integrador I
-- Matemática Aplicada
(8, 223, 211, 'APROBADA', NULL),       -- Sistemas Productivos I
(8, 223, 212, 'APROBADA', NULL),       -- Física I
(8, 223, 213, 'APROBADA', NULL),       -- Química Orgánica
-- Química Textil II
(8, 224, 217, 'CURSADA',  'APROBADA'), -- Física II
(8, 224, 218, 'CURSADA',  'APROBADA'), -- Química Textil I
(8, 224, 210, 'APROBADA', NULL),       -- Fundamentos Textiles
(8, 224, 212, 'APROBADA', NULL),       -- Física I
-- Emprendimientos Comerciales
(8, 225, 210, 'APROBADA', NULL),       -- Fundamentos Textiles
(8, 225, 211, 'APROBADA', NULL),       -- Sistemas Productivos I
(8, 225, 214, 'APROBADA', NULL),       -- Gestión de las Organizaciones
-- Historia de los Estilos
(8, 226, 207, 'APROBADA', NULL),       -- Medios expresivos I
(8, 226, 208, 'APROBADA', NULL),       -- Historia
-- Seminario Integrador IV
(8, 227, 210, 'CURSADA',  'APROBADA'), -- Fundamentos Textiles
(8, 227, 211, 'CURSADA',  'APROBADA'), -- Sistemas Productivos I
(8, 227, 215, 'CURSADA',  'APROBADA'), -- Dibujo Técnico
(8, 227, 216, 'APROBADA', NULL),       -- Seminario Integrador II
-- Informática Aplicada
(8, 228, 221, 'CURSADA',  'APROBADA'), -- Procesos Productivos Industriales
(8, 228, 222, 'CURSADA',  'APROBADA'), -- Sistemas Productivos II
(8, 228, 223, 'CURSADA',  'APROBADA'), -- Matemática Aplicada
-- Tintorería y Estampería
(8, 229, 223, 'CURSADA',  'APROBADA'), -- Matemática Aplicada
(8, 229, 224, 'CURSADA',  'APROBADA'), -- Química Textil II
-- Estrategias de Comercialización
(8, 230, 225, 'CURSADA',  'APROBADA'), -- Emprendimientos Comerciales
(8, 230, 221, NULL,       'APROBADA'), -- Procesos Productivos Industriales
(8, 230, 222, NULL,       'APROBADA'), -- Sistemas Productivos II
-- Gestión de Diseño
(8, 232, 226, 'CURSADA',  'APROBADA'), -- Historia de los Estilos
(8, 232, 219, 'APROBADA', NULL),       -- Medios expresivos II
-- Legislación
(8, 233, 221, 'CURSADA',  'APROBADA'), -- Procesos Productivos Industriales
(8, 233, 222, 'CURSADA',  'APROBADA'), -- Sistemas Productivos II
(8, 233, 225, 'CURSADA',  'APROBADA'), -- Emprendimientos Comerciales
-- Seminario Integrador V
(8, 234, 221, 'CURSADA',  'APROBADA'), -- Procesos Productivos Industriales
(8, 234, 222, 'CURSADA',  'APROBADA'), -- Sistemas Productivos II
(8, 234, 225, 'CURSADA',  'APROBADA'), -- Emprendimientos Comerciales
(8, 234, 220, 'APROBADA', 'APROBADA'), -- Seminario Integrador III
(8, 234, 205, NULL,       'APROBADA'), -- Matemáticas
(8, 234, 206, NULL,       'APROBADA'), -- Química General
(8, 234, 207, NULL,       'APROBADA'), -- Medios expresivos I
(8, 234, 208, NULL,       'APROBADA'), -- Historia
(8, 234, 209, NULL,       'APROBADA'), -- Seminario Integrador I
(8, 234, 210, NULL,       'APROBADA'), -- Fundamentos Textiles
(8, 234, 211, NULL,       'APROBADA'), -- Sistemas Productivos I
(8, 234, 212, NULL,       'APROBADA'), -- Física I
(8, 234, 213, NULL,       'APROBADA'), -- Química Orgánica
(8, 234, 214, NULL,       'APROBADA'), -- Gestión de las Organizaciones
(8, 234, 215, NULL,       'APROBADA'), -- Dibujo Técnico
(8, 234, 216, NULL,       'APROBADA'), -- Seminario Integrador II
(8, 234, 217, NULL,       'APROBADA'), -- Física II
(8, 234, 218, NULL,       'APROBADA'), -- Química Textil I
(8, 234, 219, NULL,       'APROBADA'), -- Medios expresivos II
(8, 234, 223, NULL,       'APROBADA'), -- Matemática Aplicada
(8, 234, 224, NULL,       'APROBADA'), -- Química Textil II
(8, 234, 226, NULL,       'APROBADA'), -- Historia de los Estilos
(8, 234, 227, NULL,       'APROBADA'), -- Seminario Integrador IV
(8, 234, 228, NULL,       'APROBADA'), -- Informática Aplicada
(8, 234, 229, NULL,       'APROBADA'), -- Tintorería y Estampería
(8, 234, 230, NULL,       'APROBADA'), -- Estrategias de Comercialización
(8, 234, 231, NULL,       'APROBADA'), -- Inglés Técnico
(8, 234, 232, NULL,       'APROBADA'), -- Gestión de Diseño
(8, 234, 233, NULL,       'APROBADA'); -- Legislación

-- Resumen: materias y correlatividades de cada plan
SELECT c.id AS carrera_id, c.nombre AS carrera, p.id AS plan_id, p.nombre AS plan,
       (SELECT COUNT(*) FROM plan_materia pm WHERE pm.plan_estudio_id = p.id) AS materias,
       (SELECT COUNT(*) FROM correlatividad_materias cm WHERE cm.plan_estudio_id = p.id) AS correlatividades
FROM carrera c
JOIN plan_estudio p ON p.carrera_id = c.id
ORDER BY c.id, p.id;
