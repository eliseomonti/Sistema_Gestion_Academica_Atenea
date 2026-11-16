-- =====================================================================================
-- DATOS DE DEMOSTRACIÓN: un ciclo lectivo completo de la TUP (Plan 2024)
-- =====================================================================================
-- Se corre después de atenea_db_script.sql, 01-carreras.sql, 02-personas.sql y 03-aulas.sql.
--
-- Arma lo necesario para mostrar el sistema de punta a punta, con datos coherentes entre sí:
--
--   * Períodos de inscripción de la carrera: cursada del 1.er y del 2.º cuatrimestre (cerrados),
--     un turno de finales de julio (ya pasado) y un turno de finales ABIERTO hoy.
--   * La Comisión 1 de 1.er año (mañana) con sus 8 materias, docentes, horarios y aulas.
--   * Tres estudiantes de la cohorte inscriptos a las 8 materias, y dos docentes que las dictan.
--   * Parciales con notas. Las materias del 1.er cuatrimestre tienen el acta cerrada
--     (promocionado o regular); las del 2.º están en curso.
--   * Mesas de examen con tribunal: las de julio con notas, y las del turno abierto con
--     inscriptos y lugar para inscribirse en vivo.
--
-- No usa ids fijos: busca todo por datos que no cambian (DNI, nombre de materia y de aula), así
-- funciona en cualquier base creada con los scripts anteriores. Las fechas salen del día en que
-- se corre: el año lectivo es el actual y el turno de finales queda abierto desde hoy. Está
-- pensado para correrse en el segundo cuatrimestre (agosto a diciembre).
--
-- Correrlo UNA vez. Las notas y las actas no se pueden borrar (triggers append-only), así que
-- para empezar de nuevo hay que recrear la base con todos los scripts.
--
-- Usuarios para la demo (contraseña Prueba123):
--   admin        28031246  Mariela Fontana   (ya viene en 02-personas.sql)
--   docente      13096033  Soledad Moyano    Programación I y II, Base de Datos I, Inglés I
--   docente      22212730  Rubén Aguirre     Arquitectura, Matemática, Organización, Probabilidad
--   estudiante   42848941  Federico Castillo
--   estudiante   48907894  Camila Flores
--   estudiante   47884828  Juan Pablo Leiva
-- =====================================================================================

SET NAMES utf8mb4;
USE atenea_db;

-- -------------------------------------------------------------------------------------
-- Datos base
-- -------------------------------------------------------------------------------------
SET @anio = YEAR(CURDATE());

SET @carrera = (SELECT id FROM carrera WHERE nombre = 'Tecnicatura Universitaria en Programación');
SET @plan = (SELECT id FROM plan_estudio WHERE carrera_id = @carrera AND nombre = 'Plan 2024');

-- Docentes
SET @moyano   = (SELECT id FROM persona WHERE dni = '13096033');  -- Soledad Moyano
SET @aguirre  = (SELECT id FROM persona WHERE dni = '22212730');  -- Rubén Aguirre

-- Turno de finales abierto: desde hace una semana hasta dentro de tres. Las mesas son después
-- del cierre, como pide el backend. Si la demo es más adelante, se cambian estas dos fechas
-- (o se editan después desde Períodos, en el admin).
SET @finales_desde = TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '08:00:00');
SET @finales_hasta = TIMESTAMP(CURDATE() + INTERVAL 21 DAY, '23:59:00');

-- -------------------------------------------------------------------------------------
-- Períodos de inscripción
-- -------------------------------------------------------------------------------------
-- ON DUPLICATE KEY ... LAST_INSERT_ID(id): si el período ya existe, se usa ese.
INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
VALUES (@carrera, 'CURSADA', @anio, 'PRIMER_CUATRIMESTRE',
        CONCAT(@anio, '-02-02 08:00:00'), CONCAT(@anio, '-03-13 23:59:00'))
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @cursada_1c = LAST_INSERT_ID();

INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
VALUES (@carrera, 'CURSADA', @anio, 'SEGUNDO_CUATRIMESTRE',
        CONCAT(@anio, '-07-20 08:00:00'), CONCAT(@anio, '-08-07 23:59:00'))
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @cursada_2c = LAST_INSERT_ID();

INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
VALUES (@carrera, 'FINAL', @anio, NULL,
        CONCAT(@anio, '-07-01 08:00:00'), CONCAT(@anio, '-07-15 23:59:00'))
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @finales_julio = LAST_INSERT_ID();

INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
VALUES (@carrera, 'FINAL', @anio, NULL, @finales_desde, @finales_hasta)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @finales_abierto = LAST_INSERT_ID();

-- -------------------------------------------------------------------------------------
-- Comisión 1 de 1.er año, turno mañana, con las 8 materias del año
-- -------------------------------------------------------------------------------------
INSERT INTO comision (plan_estudio_id, anio_cursado, nro_comision, turno, anio_lectivo)
VALUES (@plan, 1, 1, 'MANANA', @anio)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @comision = LAST_INSERT_ID();

INSERT IGNORE INTO comision_materia (comision_id, materia_id, periodo_lectivo, cupo_maximo)
SELECT @comision, pm.materia_id, pm.periodo_lectivo, 40
FROM plan_materia pm
WHERE pm.plan_estudio_id = @plan AND pm.anio_cursado = 1;

-- Cada materia de la comisión, por su nombre
SET @prog1 = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
              WHERE cm.comision_id = @comision AND m.nombre = 'Programación I');
SET @arq   = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
              WHERE cm.comision_id = @comision AND m.nombre = 'Arquitectura y Sistemas Operativos');
SET @mate  = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
              WHERE cm.comision_id = @comision AND m.nombre = 'Matemática');
SET @org   = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
              WHERE cm.comision_id = @comision AND m.nombre = 'Organización Empresarial');
SET @prog2 = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
              WHERE cm.comision_id = @comision AND m.nombre = 'Programación II');
SET @proba = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
              WHERE cm.comision_id = @comision AND m.nombre = 'Probabilidad y Estadística');
SET @bd1   = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
              WHERE cm.comision_id = @comision AND m.nombre = 'Base de Datos I');
SET @ingles = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
               WHERE cm.comision_id = @comision AND m.nombre = 'Inglés I');

-- Docentes: cada uno es titular de cuatro materias y ayudante en una del otro (sin choques de horario)
INSERT IGNORE INTO comision_docente (comision_materia_id, docente_id, rol) VALUES
(@prog1,  @moyano,  'TITULAR'),
(@prog2,  @moyano,  'TITULAR'),
(@bd1,    @moyano,  'TITULAR'),
(@ingles, @moyano,  'TITULAR'),
(@arq,    @aguirre, 'TITULAR'),
(@mate,   @aguirre, 'TITULAR'),
(@org,    @aguirre, 'TITULAR'),
(@proba,  @aguirre, 'TITULAR'),
(@mate,   @moyano,  'AYUDANTE'),
(@prog2,  @aguirre, 'AYUDANTE');

-- Horarios: los bloques del turno mañana (08:30 a 10:30 y 11:00 a 13:00), sin choques de aula
INSERT IGNORE INTO comision_horario (comision_materia_id, dia_semana, hora_inicio, hora_fin, aula_id) VALUES
-- 1.er cuatrimestre
(@prog1,  'LUNES',     '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'LAB 01')),
(@prog1,  'MIERCOLES', '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'LAB 01')),
(@arq,    'MARTES',    '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'Aula 101')),
(@mate,   'JUEVES',    '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'Aula 102')),
(@org,    'VIERNES',   '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'Aula 103')),
-- 2.º cuatrimestre
(@prog2,  'LUNES',     '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'LAB 02')),
(@prog2,  'MIERCOLES', '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'LAB 02')),
(@proba,  'MARTES',    '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'Aula 104')),
(@bd1,    'JUEVES',    '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'LAB 03')),
(@ingles, 'VIERNES',   '11:00', '13:00', (SELECT id FROM aula WHERE nombre = 'Aula 105'));

-- -------------------------------------------------------------------------------------
-- Estudiantes: Federico Castillo y dos compañeros de la cohorte, inscriptos a las 8 materias
-- -------------------------------------------------------------------------------------
-- Cada materia con el período en que se anotaron: el del 1.er o el del 2.º cuatrimestre.
INSERT IGNORE INTO estudiante_inscripcion_comision (inscripcion_carrera_id, comision_materia_id, periodo_id, fecha_inscripcion)
SELECT ic.id, cm.id,
       IF(cm.periodo_lectivo = 'PRIMER_CUATRIMESTRE', @cursada_1c, @cursada_2c),
       IF(cm.periodo_lectivo = 'PRIMER_CUATRIMESTRE', CONCAT(@anio, '-02-10'), CONCAT(@anio, '-07-27'))
FROM persona p
JOIN inscripcion_carrera ic ON ic.estudiante_id = p.id AND ic.plan_estudio_id = @plan
JOIN comision_materia cm ON cm.comision_id = @comision
WHERE p.dni IN ('42848941',   -- Federico Castillo
                '48907894',   -- Camila Flores
                '47884828');  -- Juan Pablo Leiva

-- -------------------------------------------------------------------------------------
-- Evaluaciones y notas
-- -------------------------------------------------------------------------------------
-- 1.er cuatrimestre: dos parciales. 2.º cuatrimestre: el primero ya rendido y el segundo en unos
-- días, todavía sin notas (para cargarlas en vivo).
INSERT IGNORE INTO evaluacion_cursada (comision_materia_id, nombre, fecha, tipo)
SELECT cm.id, 'Primer parcial',
       IF(cm.periodo_lectivo = 'PRIMER_CUATRIMESTRE', CONCAT(@anio, '-04-20'), CONCAT(@anio, '-09-21')),
       'PARCIAL'
FROM comision_materia cm WHERE cm.comision_id = @comision;

INSERT IGNORE INTO evaluacion_cursada (comision_materia_id, nombre, fecha, tipo)
SELECT cm.id, 'Segundo parcial',
       IF(cm.periodo_lectivo = 'PRIMER_CUATRIMESTRE', CONCAT(@anio, '-06-15'), CURDATE() + INTERVAL 7 DAY),
       'PARCIAL'
FROM comision_materia cm WHERE cm.comision_id = @comision;

-- Las notas las carga el titular de cada materia, una semana después de la evaluación. Cada
-- estudiante tiene su nivel (base) y la nota varía un poco entre evaluaciones.
INSERT IGNORE INTO nota_evaluacion (comision_materia_id, evaluacion_cursada_id, inscripcion_comision_id,
                                    docente_id, condicion, nota, fecha_registro)
SELECT ev.comision_materia_id, ev.id, eic.id, cd.docente_id, 'PRESENTE',
       LEAST(10, CASE p.dni WHEN '42848941' THEN 7   -- Federico
                            WHEN '48907894' THEN 6   -- Camila
                            ELSE 5 END + (ev.id + eic.id) % 3),
       ev.fecha + INTERVAL 7 DAY
FROM evaluacion_cursada ev
JOIN estudiante_inscripcion_comision eic ON eic.comision_materia_id = ev.comision_materia_id
JOIN inscripcion_carrera ic ON ic.id = eic.inscripcion_carrera_id
JOIN persona p ON p.id = ic.estudiante_id
JOIN comision_docente cd ON cd.comision_materia_id = ev.comision_materia_id AND cd.rol = 'TITULAR'
JOIN comision_materia cm ON cm.id = ev.comision_materia_id
WHERE cm.comision_id = @comision
  AND ev.fecha < CURDATE();   -- las evaluaciones futuras quedan sin nota

-- -------------------------------------------------------------------------------------
-- Actas de cierre del 1.er cuatrimestre
-- -------------------------------------------------------------------------------------
-- Todos quedan al menos regulares: así pueden cursar las del 2.º cuatrimestre sin romper
-- correlativas. La regularidad vence a los dos años.
INSERT IGNORE INTO cierre_cursada (inscripcion_comision_id, docente_id, condicion, nota_cursada,
                                   fecha_cierre, fecha_vencimiento)
SELECT eic.id, cd.docente_id, x.condicion, x.nota, CONCAT(@anio, '-07-10'),
       IF(x.condicion = 'REGULAR', CONCAT(@anio + 2, '-07-10'), NULL)
FROM (
    SELECT '42848941' AS dni, 'Programación I' AS materia, 'PROMOCIONADO' AS condicion, 8 AS nota
    UNION ALL SELECT '42848941', 'Arquitectura y Sistemas Operativos', 'PROMOCIONADO', 9
    UNION ALL SELECT '42848941', 'Matemática',                         'REGULAR',      NULL
    UNION ALL SELECT '42848941', 'Organización Empresarial',           'REGULAR',      NULL
    UNION ALL SELECT '48907894', 'Programación I',                     'REGULAR',      NULL
    UNION ALL SELECT '48907894', 'Arquitectura y Sistemas Operativos', 'REGULAR',      NULL
    UNION ALL SELECT '48907894', 'Matemática',                         'REGULAR',      NULL
    UNION ALL SELECT '48907894', 'Organización Empresarial',           'PROMOCIONADO', 8
    UNION ALL SELECT '47884828', 'Programación I',                     'REGULAR',      NULL
    UNION ALL SELECT '47884828', 'Arquitectura y Sistemas Operativos', 'PROMOCIONADO', 7
    UNION ALL SELECT '47884828', 'Matemática',                         'REGULAR',      NULL
    UNION ALL SELECT '47884828', 'Organización Empresarial',           'REGULAR',      NULL
) x
JOIN persona p ON p.dni = x.dni
JOIN inscripcion_carrera ic ON ic.estudiante_id = p.id AND ic.plan_estudio_id = @plan
JOIN comision_materia cm ON cm.comision_id = @comision
JOIN materia m ON m.id = cm.materia_id AND m.nombre = x.materia
JOIN estudiante_inscripcion_comision eic ON eic.inscripcion_carrera_id = ic.id AND eic.comision_materia_id = cm.id
JOIN comision_docente cd ON cd.comision_materia_id = cm.id AND cd.rol = 'TITULAR';

-- -------------------------------------------------------------------------------------
-- Turno de finales de julio (ya pasado), con notas
-- -------------------------------------------------------------------------------------
INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id)
SELECT cm.materia_id, @finales_julio, CONCAT(@anio, '-07-22'), '09:00', '11:00',
       (SELECT id FROM aula WHERE nombre = 'Aula 110')
FROM comision_materia cm WHERE cm.id = @mate
ON DUPLICATE KEY UPDATE examen_final.id = LAST_INSERT_ID(examen_final.id);
SET @mesa_julio_mate = LAST_INSERT_ID();

INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id)
SELECT cm.materia_id, @finales_julio, CONCAT(@anio, '-07-24'), '09:00', '11:00',
       (SELECT id FROM aula WHERE nombre = 'Aula 110')
FROM comision_materia cm WHERE cm.id = @org
ON DUPLICATE KEY UPDATE examen_final.id = LAST_INSERT_ID(examen_final.id);
SET @mesa_julio_org = LAST_INSERT_ID();

INSERT IGNORE INTO examen_final_docente (examen_final_id, docente_id) VALUES
(@mesa_julio_mate, @aguirre),
(@mesa_julio_mate, @moyano),
(@mesa_julio_org,  @aguirre);

-- Juan Pablo aprobó Matemática con 7. Federico se anotó a Organización y no se presentó.
INSERT IGNORE INTO estudiante_inscripcion_examen_final (inscripcion_carrera_id, examen_final_id, fecha_inscripcion)
SELECT ic.id, x.mesa, CONCAT(@anio, '-07-12')
FROM (SELECT '47884828' AS dni, @mesa_julio_mate AS mesa
      UNION ALL SELECT '42848941', @mesa_julio_org) x
JOIN persona p ON p.dni = x.dni
JOIN inscripcion_carrera ic ON ic.estudiante_id = p.id AND ic.plan_estudio_id = @plan;

INSERT IGNORE INTO nota_final (inscripcion_examen_final_id, docente_id, condicion, nota, fecha_registro)
SELECT eief.id, x.docente, x.condicion, x.nota, ef.fecha
FROM (SELECT '47884828' AS dni, @mesa_julio_mate AS mesa, @aguirre AS docente, 'PRESENTE' AS condicion, 7 AS nota
      UNION ALL SELECT '42848941', @mesa_julio_org, @aguirre, 'AUSENTE', NULL) x
JOIN persona p ON p.dni = x.dni
JOIN inscripcion_carrera ic ON ic.estudiante_id = p.id AND ic.plan_estudio_id = @plan
JOIN estudiante_inscripcion_examen_final eief ON eief.inscripcion_carrera_id = ic.id AND eief.examen_final_id = x.mesa
JOIN examen_final ef ON ef.id = eief.examen_final_id;

-- -------------------------------------------------------------------------------------
-- Turno de finales abierto: cuatro mesas después del cierre de la inscripción
-- -------------------------------------------------------------------------------------
INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id)
SELECT cm.materia_id, @finales_abierto, DATE(@finales_hasta) + INTERVAL 3 DAY, '18:00', '20:00',
       (SELECT id FROM aula WHERE nombre = 'LAB 01')
FROM comision_materia cm WHERE cm.id = @prog1
ON DUPLICATE KEY UPDATE examen_final.id = LAST_INSERT_ID(examen_final.id);
SET @mesa_prog1 = LAST_INSERT_ID();

INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id)
SELECT cm.materia_id, @finales_abierto, DATE(@finales_hasta) + INTERVAL 4 DAY, '09:00', '11:00', NULL
FROM comision_materia cm WHERE cm.id = @arq
ON DUPLICATE KEY UPDATE examen_final.id = LAST_INSERT_ID(examen_final.id);
SET @mesa_arq = LAST_INSERT_ID();

INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id)
SELECT cm.materia_id, @finales_abierto, DATE(@finales_hasta) + INTERVAL 5 DAY, '18:00', '20:00',
       (SELECT id FROM aula WHERE nombre = 'Aula 101')
FROM comision_materia cm WHERE cm.id = @mate
ON DUPLICATE KEY UPDATE examen_final.id = LAST_INSERT_ID(examen_final.id);
SET @mesa_mate = LAST_INSERT_ID();

INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id)
SELECT cm.materia_id, @finales_abierto, DATE(@finales_hasta) + INTERVAL 6 DAY, '18:00', '20:00',
       (SELECT id FROM aula WHERE nombre = 'Aula 102')
FROM comision_materia cm WHERE cm.id = @org
ON DUPLICATE KEY UPDATE examen_final.id = LAST_INSERT_ID(examen_final.id);
SET @mesa_org = LAST_INSERT_ID();

INSERT IGNORE INTO examen_final_docente (examen_final_id, docente_id) VALUES
(@mesa_prog1, @moyano),
(@mesa_prog1, @aguirre),
(@mesa_arq,   @aguirre),
(@mesa_mate,  @aguirre),
(@mesa_mate,  @moyano),
(@mesa_org,   @aguirre);

-- Inscriptos de antemano. Federico está anotado a Matemática y tiene Organización libre para
-- anotarse en vivo (Programación I y Arquitectura no le aparecen: las promocionó).
INSERT IGNORE INTO estudiante_inscripcion_examen_final (inscripcion_carrera_id, examen_final_id, fecha_inscripcion)
SELECT ic.id, x.mesa, CURDATE()
FROM (SELECT '42848941' AS dni, @mesa_mate AS mesa       -- Federico: Matemática
      UNION ALL SELECT '48907894', @mesa_prog1           -- Camila: Programación I
      UNION ALL SELECT '48907894', @mesa_mate            -- Camila: Matemática
      UNION ALL SELECT '47884828', @mesa_prog1) x        -- Juan Pablo: Programación I
JOIN persona p ON p.dni = x.dni
JOIN inscripcion_carrera ic ON ic.estudiante_id = p.id AND ic.plan_estudio_id = @plan;

-- -------------------------------------------------------------------------------------
-- Resumen
-- -------------------------------------------------------------------------------------
SELECT 'períodos'               AS que, COUNT(*) AS cantidad FROM periodo_inscripcion WHERE carrera_id = @carrera
UNION ALL SELECT 'materias de la comisión', COUNT(*) FROM comision_materia WHERE comision_id = @comision
UNION ALL SELECT 'inscripciones a cursada', COUNT(*) FROM estudiante_inscripcion_comision eic
          JOIN comision_materia cm ON cm.id = eic.comision_materia_id WHERE cm.comision_id = @comision
UNION ALL SELECT 'notas de evaluaciones',   COUNT(*) FROM nota_evaluacion WHERE comision_materia_id IN
          (SELECT id FROM comision_materia WHERE comision_id = @comision)
UNION ALL SELECT 'actas de cierre',         COUNT(*) FROM cierre_cursada cc
          JOIN estudiante_inscripcion_comision eic ON eic.id = cc.inscripcion_comision_id
          JOIN comision_materia cm ON cm.id = eic.comision_materia_id WHERE cm.comision_id = @comision
UNION ALL SELECT 'mesas de examen',         COUNT(*) FROM examen_final WHERE periodo_id IN (@finales_julio, @finales_abierto)
UNION ALL SELECT 'inscripciones a finales', COUNT(*) FROM estudiante_inscripcion_examen_final eief
          JOIN examen_final ef ON ef.id = eief.examen_final_id WHERE ef.periodo_id IN (@finales_julio, @finales_abierto);
