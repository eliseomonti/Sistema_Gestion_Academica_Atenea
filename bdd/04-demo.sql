-- =====================================================================================
-- DATOS DE DEMOSTRACIÓN: un ciclo lectivo completo de la TUP (Plan 2024)
-- =====================================================================================
-- Se corre después de atenea_db_script.sql, 01-carreras.sql, 02-personas.sql y 03-aulas.sql.
--
-- Deja la base lista para probar TODAS las funcionalidades de los tres actores, con datos
-- coherentes con las reglas del sistema (correlativas, cupos, períodos, actas):
--
--   * Períodos de la carrera: cursada del 1.er cuatrimestre (cerrada), cursada del 2.º
--     cuatrimestre (ABIERTA), finales de julio (pasado) y un turno de finales ABIERTO.
--   * Comisión 1 de 1.er año (mañana): las 8 materias, 12 estudiantes, parciales con notas y el
--     1.er cuatrimestre con el acta cerrada. Comisión 2 (noche): dos materias del 2.º
--     cuatrimestre, una con el cupo lleno.
--   * Mesas de examen con tribunal: de julio (con notas y una sin cargar) y del turno abierto.
--
-- No usa ids fijos: busca todo por datos que no cambian (DNI, nombre de materia y de aula), así
-- funciona en cualquier base creada con los scripts anteriores. Las fechas salen del día en que
-- se corre: el año lectivo es el actual y los períodos abiertos quedan abiertos tres semanas.
-- Está pensado para correrse en el segundo cuatrimestre (agosto a diciembre).
--
-- Correrlo UNA vez. Las notas y las actas no se pueden borrar (triggers append-only): para
-- empezar de nuevo hay que recrear la base con todos los scripts. Si se corre dos veces el
-- mismo día no duplica nada.
--
-- No mezclarlo con scripts de prueba que inscriben estudiantes sin mirar correlativas.
--
-- -------------------------------------------------------------------------------------
-- USUARIOS (contraseña Prueba123)
-- -------------------------------------------------------------------------------------
--   admin        28031246  Mariela Fontana   (ya viene en 02-personas.sql)
--   docente      13096033  Soledad Moyano    Programación I y II, Base de Datos I, Inglés I
--   docente      22212730  Rubén Aguirre     Arquitectura, Matemática, Organización, Probabilidad
--   estudiante   42848941  Federico Castillo
--   estudiante   48907894  Camila Flores
--   estudiante   47884828  Juan Pablo Leiva
--   (y 9 compañeros más en la Comisión 1, para que las planillas del docente tengan gente)
--
-- -------------------------------------------------------------------------------------
-- QUÉ SE PUEDE PROBAR
-- -------------------------------------------------------------------------------------
-- Estudiante (Federico):
--   * Inicio, Plan de estudio (2 aprobadas, 2 regulares, 3 cursando, 1 sin cursar), Mis notas.
--   * Inscripción a cursado: Probabilidad y Estadística está libre. En la Comisión 2 no hay cupo
--     (409) y en la Comisión 1 sí. Después se puede dar de baja (el período está abierto).
--     Inglés I en la Comisión 2: "ya estás inscripto en otra comisión" (409).
--   * Inscripción a finales: anotado a Matemática (se puede dar de baja) y Organización libre
--     para anotarse. Programación I y Arquitectura no aparecen: las promocionó.
-- Docente:
--   * Inicio con las clases del día y las próximas mesas.
--   * Materias del 2.º cuatrimestre: el Segundo parcial (de hace dos días) sin notas para
--     cargarlas; crear evaluaciones y recuperatorios; cerrar el acta (regular, promocionado,
--     libre). Las del 1.er cuatrimestre tienen el acta cerrada: no dejan cambiar notas.
--   * Exámenes finales: la mesa de Arquitectura de julio (Rubén Aguirre) tiene a Camila sin nota.
-- Admin (Mariela):
--   * Carreras, planes, materias, estudiantes, docentes y aulas (de los scripts anteriores).
--   * Comisiones: detalle con grilla horaria; agregar materias a la Comisión 2; editar docentes.
--   * Inscriptos de una materia y dar de baja a un estudiante (las del 2.º cuatrimestre; las del
--     1.º tienen acta y el sistema no deja: 409).
--   * Períodos: alta, edición, habilitar y deshabilitar. Mesas: alta, edición con tribunal y
--     eliminación (la de Arquitectura del turno abierto no tiene inscriptos; las otras sí: 409).
-- =====================================================================================

SET NAMES utf8mb4;
USE atenea_db;

-- -------------------------------------------------------------------------------------
-- Datos base
-- -------------------------------------------------------------------------------------
SET @anio = YEAR(CURDATE());

SET @carrera = (SELECT id FROM carrera WHERE nombre = 'Tecnicatura Universitaria en Programación');
SET @plan = (SELECT id FROM plan_estudio WHERE carrera_id = @carrera AND nombre = 'Plan 2024');

SET @moyano  = (SELECT id FROM persona WHERE dni = '13096033');  -- Soledad Moyano
SET @aguirre = (SELECT id FROM persona WHERE dni = '22212730');  -- Rubén Aguirre

-- Las ventanas abiertas: desde hace una semana hasta dentro de tres. Si la demo es más adelante,
-- se cambian estas dos fechas, o se editan después desde Períodos, en el admin.
SET @abierto_desde = TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '08:00:00');
SET @abierto_hasta = TIMESTAMP(CURDATE() + INTERVAL 21 DAY, '23:59:00');

-- Los estudiantes: los 3 de la demo y 9 compañeros de la misma cohorte (TUP Plan 2024).
DROP TEMPORARY TABLE IF EXISTS demo_estudiantes;
CREATE TEMPORARY TABLE demo_estudiantes (dni VARCHAR(10) PRIMARY KEY, principal BOOLEAN, base TINYINT);
INSERT INTO demo_estudiantes (dni, principal, base) VALUES
('42848941', TRUE,  7),   -- Federico Castillo
('48907894', TRUE,  6),   -- Camila Flores
('47884828', TRUE,  5),   -- Juan Pablo Leiva
('48119220', FALSE, 5),   -- Juan Martín Gutiérrez
('48796140', FALSE, 6),   -- Melina Basile
('49693718', FALSE, 4),   -- Juan Manuel Pérez
('48579956', FALSE, 5),   -- Juan Leiva
('40735695', FALSE, 6),   -- Daniela Messina
('46068975', FALSE, 5),   -- Abril Rojas
('48246535', FALSE, 4),   -- Bautista Molina
('48348124', FALSE, 6),   -- Clara Peralta
('49026990', FALSE, 5);   -- Agustín Becker

-- -------------------------------------------------------------------------------------
-- Períodos de inscripción
-- -------------------------------------------------------------------------------------
-- ON DUPLICATE KEY ... LAST_INSERT_ID(id): si el período ya existe, se usa ese.
INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
VALUES (@carrera, 'CURSADA', @anio, 'PRIMER_CUATRIMESTRE',
        CONCAT(@anio, '-02-02 08:00:00'), CONCAT(@anio, '-03-13 23:59:00'))
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @cursada_1c = LAST_INSERT_ID();

-- Abierta: para poder probar la inscripción y la baja a cursado.
INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
VALUES (@carrera, 'CURSADA', @anio, 'SEGUNDO_CUATRIMESTRE', CONCAT(@anio, '-07-20 08:00:00'), @abierto_hasta)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @cursada_2c = LAST_INSERT_ID();

INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
VALUES (@carrera, 'FINAL', @anio, NULL, CONCAT(@anio, '-07-01 08:00:00'), CONCAT(@anio, '-07-15 23:59:00'))
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @finales_julio = LAST_INSERT_ID();

INSERT INTO periodo_inscripcion (carrera_id, tipo, anio_lectivo, periodo_lectivo, fecha_inicio, fecha_cierre)
VALUES (@carrera, 'FINAL', @anio, NULL, @abierto_desde, @abierto_hasta)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @finales_abierto = LAST_INSERT_ID();

-- -------------------------------------------------------------------------------------
-- Comisión 1 de 1.er año, turno mañana, con las 8 materias del año
-- -------------------------------------------------------------------------------------
INSERT INTO comision (plan_estudio_id, anio_cursado, nro_comision, turno, anio_lectivo)
VALUES (@plan, 1, 1, 'MANANA', @anio)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @comision1 = LAST_INSERT_ID();

INSERT IGNORE INTO comision_materia (comision_id, materia_id, periodo_lectivo, cupo_maximo)
SELECT @comision1, pm.materia_id, pm.periodo_lectivo, 40
FROM plan_materia pm
WHERE pm.plan_estudio_id = @plan AND pm.anio_cursado = 1;

SET @prog1  = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
               WHERE cm.comision_id = @comision1 AND m.nombre = 'Programación I');
SET @arq    = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
               WHERE cm.comision_id = @comision1 AND m.nombre = 'Arquitectura y Sistemas Operativos');
SET @mate   = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
               WHERE cm.comision_id = @comision1 AND m.nombre = 'Matemática');
SET @org    = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
               WHERE cm.comision_id = @comision1 AND m.nombre = 'Organización Empresarial');
SET @prog2  = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
               WHERE cm.comision_id = @comision1 AND m.nombre = 'Programación II');
SET @proba  = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
               WHERE cm.comision_id = @comision1 AND m.nombre = 'Probabilidad y Estadística');
SET @bd1    = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
               WHERE cm.comision_id = @comision1 AND m.nombre = 'Base de Datos I');
SET @ingles = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
               WHERE cm.comision_id = @comision1 AND m.nombre = 'Inglés I');

-- Cada docente es titular de cuatro materias y ayudante en una del otro (sin choques de horario)
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

-- Los bloques del turno mañana (08:30 a 10:30 y 11:00 a 13:00), sin choques de aula
INSERT IGNORE INTO comision_horario (comision_materia_id, dia_semana, hora_inicio, hora_fin, aula_id) VALUES
(@prog1,  'LUNES',     '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'LAB 01')),
(@prog1,  'MIERCOLES', '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'LAB 01')),
(@arq,    'MARTES',    '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'Aula 101')),
(@mate,   'JUEVES',    '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'Aula 102')),
(@org,    'VIERNES',   '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'Aula 103')),
(@prog2,  'LUNES',     '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'LAB 02')),
(@prog2,  'MIERCOLES', '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'LAB 02')),
(@proba,  'MARTES',    '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'Aula 104')),
(@bd1,    'JUEVES',    '08:30', '10:30', (SELECT id FROM aula WHERE nombre = 'LAB 03')),
(@ingles, 'VIERNES',   '11:00', '13:00', (SELECT id FROM aula WHERE nombre = 'Aula 105'));

-- -------------------------------------------------------------------------------------
-- Comisión 2 de 1.er año, turno noche: dos materias del 2.º cuatrimestre
-- -------------------------------------------------------------------------------------
-- Probabilidad tiene cupo 1 y ya lo ocupa Camila: sirve para mostrar el "no hay cupo". A esta
-- comisión el admin le puede agregar las materias que le faltan.
INSERT INTO comision (plan_estudio_id, anio_cursado, nro_comision, turno, anio_lectivo)
VALUES (@plan, 1, 2, 'NOCHE', @anio)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @comision2 = LAST_INSERT_ID();

INSERT IGNORE INTO comision_materia (comision_id, materia_id, periodo_lectivo, cupo_maximo)
SELECT @comision2, pm.materia_id, pm.periodo_lectivo,
       IF(m.nombre = 'Probabilidad y Estadística', 1, 30)
FROM plan_materia pm
JOIN materia m ON m.id = pm.materia_id
WHERE pm.plan_estudio_id = @plan AND pm.anio_cursado = 1
  AND m.nombre IN ('Probabilidad y Estadística', 'Inglés I');

SET @proba2  = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
                WHERE cm.comision_id = @comision2 AND m.nombre = 'Probabilidad y Estadística');
SET @ingles2 = (SELECT cm.id FROM comision_materia cm JOIN materia m ON m.id = cm.materia_id
                WHERE cm.comision_id = @comision2 AND m.nombre = 'Inglés I');

INSERT IGNORE INTO comision_docente (comision_materia_id, docente_id, rol) VALUES
(@proba2,  @aguirre, 'TITULAR'),
(@ingles2, @moyano,  'TITULAR');

INSERT IGNORE INTO comision_horario (comision_materia_id, dia_semana, hora_inicio, hora_fin, aula_id) VALUES
(@proba2,  'MARTES',  '18:00', '20:00', (SELECT id FROM aula WHERE nombre = 'Aula 104')),
(@ingles2, 'VIERNES', '18:00', '20:00', (SELECT id FROM aula WHERE nombre = 'Aula 105'));

-- -------------------------------------------------------------------------------------
-- Inscripciones a cursada
-- -------------------------------------------------------------------------------------
-- Los 12 a las 8 materias de la Comisión 1, salvo Probabilidad para Federico (queda libre para
-- inscribirse en vivo) y para Camila (la cursa en la Comisión 2).
INSERT IGNORE INTO estudiante_inscripcion_comision (inscripcion_carrera_id, comision_materia_id, periodo_id, fecha_inscripcion)
SELECT ic.id, cm.id,
       IF(cm.periodo_lectivo = 'PRIMER_CUATRIMESTRE', @cursada_1c, @cursada_2c),
       IF(cm.periodo_lectivo = 'PRIMER_CUATRIMESTRE', CONCAT(@anio, '-02-10'), CONCAT(@anio, '-07-27'))
FROM demo_estudiantes d
JOIN persona p ON p.dni = d.dni
JOIN inscripcion_carrera ic ON ic.estudiante_id = p.id AND ic.plan_estudio_id = @plan
JOIN comision_materia cm ON cm.comision_id = @comision1
WHERE NOT (cm.id = @proba AND d.dni IN ('42848941', '48907894'));

INSERT IGNORE INTO estudiante_inscripcion_comision (inscripcion_carrera_id, comision_materia_id, periodo_id, fecha_inscripcion)
SELECT ic.id, @proba2, @cursada_2c, CONCAT(@anio, '-07-27')
FROM persona p
JOIN inscripcion_carrera ic ON ic.estudiante_id = p.id AND ic.plan_estudio_id = @plan
WHERE p.dni = '48907894';

-- -------------------------------------------------------------------------------------
-- Evaluaciones y notas
-- -------------------------------------------------------------------------------------
-- Dos parciales por materia. El Segundo parcial del 2.º cuatrimestre se tomó hace dos días y
-- queda sin notas, para cargarlas en vivo (el backend no deja cargar notas con fecha futura).
INSERT IGNORE INTO evaluacion_cursada (comision_materia_id, nombre, fecha, tipo)
SELECT cm.id, 'Primer parcial',
       IF(cm.periodo_lectivo = 'PRIMER_CUATRIMESTRE', CONCAT(@anio, '-04-20'), CONCAT(@anio, '-09-21')),
       'PARCIAL'
FROM comision_materia cm WHERE cm.comision_id IN (@comision1, @comision2);

INSERT IGNORE INTO evaluacion_cursada (comision_materia_id, nombre, fecha, tipo)
SELECT cm.id, 'Segundo parcial',
       IF(cm.periodo_lectivo = 'PRIMER_CUATRIMESTRE', CONCAT(@anio, '-06-15'), CURDATE() - INTERVAL 2 DAY),
       'PARCIAL'
FROM comision_materia cm WHERE cm.comision_id IN (@comision1, @comision2);

-- La nota la carga el titular, una semana después. Cada estudiante tiene su nivel (base) y la
-- nota varía un poco entre evaluaciones. Juan Manuel faltó al primer parcial de Matemática.
INSERT IGNORE INTO nota_evaluacion (comision_materia_id, evaluacion_cursada_id, inscripcion_comision_id,
                                    docente_id, condicion, nota, fecha_registro)
SELECT ev.comision_materia_id, ev.id, eic.id, cd.docente_id,
       IF(p.dni = '49693718' AND ev.comision_materia_id = @mate AND ev.nombre = 'Primer parcial',
          'AUSENTE', 'PRESENTE'),
       IF(p.dni = '49693718' AND ev.comision_materia_id = @mate AND ev.nombre = 'Primer parcial',
          NULL, LEAST(10, d.base + (ev.id + eic.id) % 3)),
       ev.fecha + INTERVAL 7 DAY
FROM evaluacion_cursada ev
JOIN comision_materia cm ON cm.id = ev.comision_materia_id
JOIN estudiante_inscripcion_comision eic ON eic.comision_materia_id = ev.comision_materia_id
JOIN inscripcion_carrera ic ON ic.id = eic.inscripcion_carrera_id
JOIN persona p ON p.id = ic.estudiante_id
JOIN demo_estudiantes d ON d.dni = p.dni
JOIN comision_docente cd ON cd.comision_materia_id = ev.comision_materia_id AND cd.rol = 'TITULAR'
WHERE cm.comision_id IN (@comision1, @comision2)
  AND NOT (cm.periodo_lectivo = 'SEGUNDO_CUATRIMESTRE' AND ev.nombre = 'Segundo parcial');

-- -------------------------------------------------------------------------------------
-- Actas de cierre del 1.er cuatrimestre
-- -------------------------------------------------------------------------------------
-- Todos quedan al menos regulares (así pueden cursar el 2.º cuatrimestre sin romper
-- correlativas). La regularidad vence a los dos años. Los 3 de la demo tienen promociones; los
-- compañeros, regulares.
INSERT IGNORE INTO cierre_cursada (inscripcion_comision_id, docente_id, condicion, nota_cursada,
                                   fecha_cierre, fecha_vencimiento)
SELECT eic.id, cd.docente_id,
       COALESCE(x.condicion, 'REGULAR'),
       x.nota,
       CONCAT(@anio, '-07-10'),
       IF(COALESCE(x.condicion, 'REGULAR') = 'REGULAR', CONCAT(@anio + 2, '-07-10'), NULL)
FROM demo_estudiantes d
JOIN persona p ON p.dni = d.dni
JOIN inscripcion_carrera ic ON ic.estudiante_id = p.id AND ic.plan_estudio_id = @plan
JOIN comision_materia cm ON cm.comision_id = @comision1 AND cm.periodo_lectivo = 'PRIMER_CUATRIMESTRE'
JOIN materia m ON m.id = cm.materia_id
JOIN estudiante_inscripcion_comision eic ON eic.inscripcion_carrera_id = ic.id AND eic.comision_materia_id = cm.id
JOIN comision_docente cd ON cd.comision_materia_id = cm.id AND cd.rol = 'TITULAR'
LEFT JOIN (
    SELECT '42848941' AS dni, 'Programación I' AS materia, 'PROMOCIONADO' AS condicion, 8 AS nota
    UNION ALL SELECT '42848941', 'Arquitectura y Sistemas Operativos', 'PROMOCIONADO', 9
    UNION ALL SELECT '48907894', 'Organización Empresarial',           'PROMOCIONADO', 8
    UNION ALL SELECT '47884828', 'Arquitectura y Sistemas Operativos', 'PROMOCIONADO', 7
) x ON x.dni = d.dni AND x.materia = m.nombre;

-- -------------------------------------------------------------------------------------
-- Turno de finales de julio (ya pasado)
-- -------------------------------------------------------------------------------------
INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id)
SELECT cm.materia_id, @finales_julio, CONCAT(@anio, '-07-22'), '09:00', '11:00',
       (SELECT id FROM aula WHERE nombre = 'Aula 110')
FROM comision_materia cm WHERE cm.id = @mate
ON DUPLICATE KEY UPDATE examen_final.id = LAST_INSERT_ID(examen_final.id);
SET @mesa_julio_mate = LAST_INSERT_ID();

INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id)
SELECT cm.materia_id, @finales_julio, CONCAT(@anio, '-07-23'), '09:00', '11:00',
       (SELECT id FROM aula WHERE nombre = 'Aula 110')
FROM comision_materia cm WHERE cm.id = @arq
ON DUPLICATE KEY UPDATE examen_final.id = LAST_INSERT_ID(examen_final.id);
SET @mesa_julio_arq = LAST_INSERT_ID();

INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id)
SELECT cm.materia_id, @finales_julio, CONCAT(@anio, '-07-24'), '09:00', '11:00',
       (SELECT id FROM aula WHERE nombre = 'Aula 110')
FROM comision_materia cm WHERE cm.id = @org
ON DUPLICATE KEY UPDATE examen_final.id = LAST_INSERT_ID(examen_final.id);
SET @mesa_julio_org = LAST_INSERT_ID();

INSERT IGNORE INTO examen_final_docente (examen_final_id, docente_id) VALUES
(@mesa_julio_mate, @aguirre),
(@mesa_julio_mate, @moyano),
(@mesa_julio_arq,  @aguirre),
(@mesa_julio_org,  @aguirre);

-- Juan Pablo aprobó Matemática con 7, Federico faltó a Organización y Camila rindió
-- Arquitectura pero todavía no tiene la nota (para cargarla en vivo).
INSERT IGNORE INTO estudiante_inscripcion_examen_final (inscripcion_carrera_id, examen_final_id, fecha_inscripcion)
SELECT ic.id, x.mesa, CONCAT(@anio, '-07-12')
FROM (SELECT '47884828' AS dni, @mesa_julio_mate AS mesa
      UNION ALL SELECT '42848941', @mesa_julio_org
      UNION ALL SELECT '48907894', @mesa_julio_arq) x
JOIN persona p ON p.dni = x.dni
JOIN inscripcion_carrera ic ON ic.estudiante_id = p.id AND ic.plan_estudio_id = @plan;

INSERT IGNORE INTO nota_final (inscripcion_examen_final_id, docente_id, condicion, nota, fecha_registro)
SELECT eief.id, @aguirre, x.condicion, x.nota, ef.fecha
FROM (SELECT '47884828' AS dni, @mesa_julio_mate AS mesa, 'PRESENTE' AS condicion, 7 AS nota
      UNION ALL SELECT '42848941', @mesa_julio_org, 'AUSENTE', NULL) x
JOIN persona p ON p.dni = x.dni
JOIN inscripcion_carrera ic ON ic.estudiante_id = p.id AND ic.plan_estudio_id = @plan
JOIN estudiante_inscripcion_examen_final eief ON eief.inscripcion_carrera_id = ic.id AND eief.examen_final_id = x.mesa
JOIN examen_final ef ON ef.id = eief.examen_final_id;

-- -------------------------------------------------------------------------------------
-- Turno de finales abierto: cuatro mesas después del cierre de la inscripción
-- -------------------------------------------------------------------------------------
INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id)
SELECT cm.materia_id, @finales_abierto, DATE(@abierto_hasta) + INTERVAL 3 DAY, '18:00', '20:00',
       (SELECT id FROM aula WHERE nombre = 'LAB 01')
FROM comision_materia cm WHERE cm.id = @prog1
ON DUPLICATE KEY UPDATE examen_final.id = LAST_INSERT_ID(examen_final.id);
SET @mesa_prog1 = LAST_INSERT_ID();

INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id)
SELECT cm.materia_id, @finales_abierto, DATE(@abierto_hasta) + INTERVAL 4 DAY, '09:00', '11:00', NULL
FROM comision_materia cm WHERE cm.id = @arq
ON DUPLICATE KEY UPDATE examen_final.id = LAST_INSERT_ID(examen_final.id);
SET @mesa_arq = LAST_INSERT_ID();

INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id)
SELECT cm.materia_id, @finales_abierto, DATE(@abierto_hasta) + INTERVAL 5 DAY, '18:00', '20:00',
       (SELECT id FROM aula WHERE nombre = 'Aula 101')
FROM comision_materia cm WHERE cm.id = @mate
ON DUPLICATE KEY UPDATE examen_final.id = LAST_INSERT_ID(examen_final.id);
SET @mesa_mate = LAST_INSERT_ID();

INSERT INTO examen_final (materia_id, periodo_id, fecha, hora_inicio, hora_fin, aula_id)
SELECT cm.materia_id, @finales_abierto, DATE(@abierto_hasta) + INTERVAL 6 DAY, '18:00', '20:00',
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

-- Anotados de antemano: Federico a Matemática, Camila a Programación I y Matemática, y algunos
-- compañeros. Arquitectura queda sin inscriptos (el admin la puede eliminar) y Organización
-- queda libre para que Federico se anote en vivo.
INSERT IGNORE INTO estudiante_inscripcion_examen_final (inscripcion_carrera_id, examen_final_id, fecha_inscripcion)
SELECT ic.id, x.mesa, CURDATE()
FROM (SELECT '42848941' AS dni, @mesa_mate AS mesa       -- Federico: Matemática
      UNION ALL SELECT '48907894', @mesa_prog1           -- Camila: Programación I
      UNION ALL SELECT '48907894', @mesa_mate            -- Camila: Matemática
      UNION ALL SELECT '47884828', @mesa_prog1           -- Juan Pablo: Programación I
      UNION ALL SELECT '48119220', @mesa_prog1           -- Juan Martín: Programación I
      UNION ALL SELECT '40735695', @mesa_mate            -- Daniela: Matemática
      UNION ALL SELECT '49026990', @mesa_org) x          -- Agustín: Organización
JOIN persona p ON p.dni = x.dni
JOIN inscripcion_carrera ic ON ic.estudiante_id = p.id AND ic.plan_estudio_id = @plan;

DROP TEMPORARY TABLE demo_estudiantes;

-- -------------------------------------------------------------------------------------
-- Resumen
-- -------------------------------------------------------------------------------------
SELECT 'períodos' AS que, COUNT(*) AS cantidad FROM periodo_inscripcion WHERE carrera_id = @carrera
UNION ALL SELECT 'materias en comisiones', COUNT(*) FROM comision_materia WHERE comision_id IN (@comision1, @comision2)
UNION ALL SELECT 'inscripciones a cursada', COUNT(*) FROM estudiante_inscripcion_comision
          WHERE comision_materia_id IN (SELECT id FROM comision_materia WHERE comision_id IN (@comision1, @comision2))
UNION ALL SELECT 'notas de evaluaciones', COUNT(*) FROM nota_evaluacion
          WHERE comision_materia_id IN (SELECT id FROM comision_materia WHERE comision_id IN (@comision1, @comision2))
UNION ALL SELECT 'actas de cierre', COUNT(*) FROM cierre_cursada cc
          JOIN estudiante_inscripcion_comision eic ON eic.id = cc.inscripcion_comision_id
          WHERE eic.comision_materia_id IN (SELECT id FROM comision_materia WHERE comision_id = @comision1)
UNION ALL SELECT 'mesas de examen', COUNT(*) FROM examen_final WHERE periodo_id IN (@finales_julio, @finales_abierto)
UNION ALL SELECT 'inscripciones a finales', COUNT(*) FROM estudiante_inscripcion_examen_final eief
          JOIN examen_final ef ON ef.id = eief.examen_final_id WHERE ef.periodo_id IN (@finales_julio, @finales_abierto);
