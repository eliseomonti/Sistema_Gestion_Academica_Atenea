-- =====================================================================================
-- AULAS
-- =====================================================================================
-- Se corre después de atenea_db_script.sql. No depende de 01-carreras.sql ni de 02-personas.sql.
-- Los ids son explícitos para que los scripts de comisiones y finales puedan usarlos.
--
--   id      aula
--   1-23    Aula 101 a Aula 123
--   24-33   LAB 01 a LAB 10 (con cero adelante para que el desplegable, que ordena por nombre,
--           los muestre en orden: si no, LAB 10 queda entre LAB 1 y LAB 2)
--   34      SUM
--   35      Aula Magna
-- =====================================================================================

SET NAMES utf8mb4;
USE atenea_db;

INSERT INTO aula (id, nombre, capacidad) VALUES
    -- Aulas comunes
    ( 1, 'Aula 101', 40),
    ( 2, 'Aula 102', 40),
    ( 3, 'Aula 103', 35),
    ( 4, 'Aula 104', 35),
    ( 5, 'Aula 105', 50),
    ( 6, 'Aula 106', 50),
    ( 7, 'Aula 107', 60),
    ( 8, 'Aula 108', 60),
    ( 9, 'Aula 109', 30),
    (10, 'Aula 110', 30),
    (11, 'Aula 111', 45),
    (12, 'Aula 112', 45),
    (13, 'Aula 113', 40),
    (14, 'Aula 114', 40),
    (15, 'Aula 115', 70),
    (16, 'Aula 116', 70),
    (17, 'Aula 117', 35),
    (18, 'Aula 118', 35),
    (19, 'Aula 119', 50),
    (20, 'Aula 120', 50),
    (21, 'Aula 121', 25),
    (22, 'Aula 122', 25),
    (23, 'Aula 123', 40),
    -- Laboratorios
    (24, 'LAB 01', 25),
    (25, 'LAB 02', 25),
    (26, 'LAB 03', 30),
    (27, 'LAB 04', 30),
    (28, 'LAB 05', 20),
    (29, 'LAB 06', 20),
    (30, 'LAB 07', 25),
    (31, 'LAB 08', 25),
    (32, 'LAB 09', 15),
    (33, 'LAB 10', 15),
    -- Espacios grandes
    (34, 'SUM', 120),
    (35, 'Aula Magna', 180);
