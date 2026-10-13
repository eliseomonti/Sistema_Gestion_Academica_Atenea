SET NAMES utf8mb4;
SET default_storage_engine = InnoDB;
SET time_zone = '-03:00';

DROP DATABASE IF EXISTS atenea_db;
CREATE DATABASE IF NOT EXISTS atenea_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE atenea_db;

-- Unica tabla donde se guardan todas las personas. El manejo de roles (admin, docente o alumno) queda a cargo de SpringSecurity y la tabla usuario_rol.
CREATE TABLE persona (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    tipo_documento VARCHAR(50) NOT NULL,
	nro_documento VARCHAR(50) NOT NULL,
    UNIQUE (tipo_documento, nro_documento),
    telefono VARCHAR(50) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
	legajo_alumno BIGINT NULL UNIQUE,
    legajo_docente BIGINT NULL UNIQUE,
    legajo_admin BIGINT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,  -- Baja logica: una persona nunca se borra (el egresado, el que abandono y el docente
    fecha_baja DATE NULL,					-- jubilado siguen existiendo).
    CONSTRAINT chk_persona_baja CHECK (
        (activo = TRUE AND fecha_baja IS NULL) OR (activo = FALSE AND fecha_baja IS NOT NULL)
    ),
    CONSTRAINT chk_persona_legajos CHECK (
        (legajo_alumno  IS NULL OR legajo_alumno  > 0) AND
        (legajo_docente IS NULL OR legajo_docente > 0) AND
        (legajo_admin   IS NULL OR legajo_admin   > 0)
    )
);

-- SPRING SECURITY --

-- La tabla Usuario se encarga de darle las credenciales a la persona para loguearse.
CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    persona_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE, 
    password VARCHAR(255) NOT NULL,    
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE, -- bloqueo MANUAL, lo pone un admin y lo saca un admin.
    intentos_fallidos TINYINT NOT NULL DEFAULT 0,
	-- fecha_bloqueo     = bloqueo AUTOMATICO y temporal, por intentos fallidos. Se guarda cuando
    --                    empezo el bloqueo y el vencimiento se deduce al consultar, en vez de
    --                    dejar la cuenta trabada para siempre esperando que alguien la libere.
    fecha_bloqueo DATETIME NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso DATETIME NULL,          -- cuando entro por ultima vez
    fecha_cambio_password DATETIME NULL,   -- para credentialsNonExpired; mide otra cosa que ultimo_acceso
    FOREIGN KEY (persona_id) REFERENCES persona (id) ON DELETE RESTRICT,
    CONSTRAINT chk_usuario_intentos CHECK (intentos_fallidos >= 0)
);

CREATE TABLE rol (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO rol (nombre) VALUES ('ROLE_ADMIN'), ('ROLE_DOCENTE'), ('ROLE_ALUMNO');

-- Se hace esta tabla para que un usuario pueda tener varios roles
CREATE TABLE usuario_rol (
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    FOREIGN KEY (rol_id) REFERENCES rol (id) ON DELETE RESTRICT
);

CREATE TABLE refresh_token (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiracion DATETIME NOT NULL,
    revocado BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT chk_token_expiracion CHECK (expiracion > fecha_creacion)
);

-- Token de un solo uso para "olvide mi contrasena". El flujo: el usuario lo pide, el backend
-- genera un valor aleatorio, guarda su HASH aca con vencimiento corto (15-30 min) y le manda el
-- valor en claro por mail. Al usarlo se valida que exista, no haya vencido y no se haya usado.
--
-- fecha_uso en vez de un booleano 'usado': "usado" es fecha_uso IS NOT NULL, y ademas queda
-- registrado CUANDO. Mismo criterio que el resto del esquema: guardar el hecho, deducir el estado.
CREATE TABLE password_reset_token (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,   -- el HASH del token, nunca el token en claro
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiracion DATETIME NOT NULL,
    fecha_uso DATETIME NULL,              -- NULL = todavia no se uso
    FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT chk_prt_expiracion CHECK (expiracion > fecha_creacion),
    CONSTRAINT chk_prt_uso CHECK (fecha_uso IS NULL OR fecha_uso >= fecha_creacion)
);

-- CARRERAS Y PLANES DE ESTUDIO --

CREATE TABLE carrera(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL UNIQUE,	
    duracion VARCHAR(20) NOT NULL,
    titulo_otorgado VARCHAR(100) NOT NULL,
    modalidad_carrera ENUM ('PRESENCIAL', 'VIRTUAL') NOT NULL,
    -- Baja logica: una carrera se deja de dictar, pero sus egresados conservan el titulo.
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_baja DATE NULL,
    CONSTRAINT chk_carrera_baja CHECK (
        (activa = TRUE AND fecha_baja IS NULL) OR (activa = FALSE AND fecha_baja IS NOT NULL)
    )
);
	
-- Una carrera puede tener varios planes de estudios
CREATE TABLE plan_estudio (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    carrera_id BIGINT NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    anio_inicio SMALLINT,
    -- Baja logica: el plan 2003 no se borra, se deja de ofrecer. 'vigente' es lo que impide
    -- que alguien se inscriba hoy a un plan discontinuado.
    vigente BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_baja DATE NULL,
    FOREIGN KEY (carrera_id) REFERENCES carrera (id) ON DELETE RESTRICT,
    UNIQUE (carrera_id, nombre),
    CONSTRAINT chk_plan_baja CHECK (
        (vigente = TRUE AND fecha_baja IS NULL) OR (vigente = FALSE AND fecha_baja IS NOT NULL)
    ),
    CONSTRAINT chk_plan_anio CHECK (anio_inicio IS NULL OR anio_inicio BETWEEN 1950 AND 2100)
);	

-- Un plan de estudio tiene varias materias
CREATE TABLE materia(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    carga_horaria_semanal INT NOT NULL,
    carga_horaria_total INT NOT NULL,
    -- La semanal puede ser 0 (PPS, Trabajo Final: no tienen dictado semanal); la total no.
    CONSTRAINT chk_materia_cargas CHECK (carga_horaria_semanal >= 0 AND carga_horaria_total > 0)
);

-- Tabla intermedia entre plan y materia porque un plan tiene muchas materias y una materia puede estar en muchos planes
CREATE TABLE plan_materia (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    plan_estudio_id BIGINT NOT NULL,
    materia_id BIGINT NOT NULL,
    codigo VARCHAR(10) NULL,
    anio_cursado TINYINT NOT NULL, -- año de la carrera en que se ubica la materia
    -- Mitad del año lectivo en que se dicta, NO el cuatrimestre de la carrera:
    -- anio_cursado=2 + periodo_lectivo='SEGUNDO_CUATRIMESTRE' es el 4to cuatrimestre de la carrera.
    periodo_lectivo ENUM('PRIMER_CUATRIMESTRE','SEGUNDO_CUATRIMESTRE','ANUAL') NOT NULL,
    UNIQUE (plan_estudio_id, materia_id),
    UNIQUE (plan_estudio_id, codigo),
    -- Redundante contra la PK, pero necesario: es el destino de las FK compuestas de
    -- correlatividad_materias, que son las que obligan a que una materia y su correlativa
    -- sean del mismo plan.
    UNIQUE uq_pm_plan_id (plan_estudio_id, id),
    FOREIGN KEY (plan_estudio_id) REFERENCES plan_estudio (id) ON DELETE RESTRICT,
    FOREIGN KEY (materia_id) REFERENCES materia (id) ON DELETE RESTRICT,
    -- 7 y no 6 para dejar margen: la carrera mas larga del sistema (Naval) tiene 6 años.
    CONSTRAINT chk_pm_anio_cursado CHECK (anio_cursado BETWEEN 1 AND 7),
    -- Sin espacios ni vacio: '' no es "sin codigo", para eso esta el NULL.
    CONSTRAINT chk_pm_codigo CHECK (codigo IS NULL OR codigo REGEXP '^[0-9A-Z][0-9A-Z-]*$')
);

-- Tabla para guardar las correlatividades de las materias de un determinado plan
CREATE TABLE correlatividad_materias (
    plan_estudio_id BIGINT NOT NULL,            -- plan al que pertenecen las dos materias
    plan_materia_id BIGINT NOT NULL,            -- la materia que requiere la correlativa
    plan_materia_correlativa_id BIGINT NOT NULL, -- la correlativa requerida
    estado_para_cursar ENUM('CURSADA','APROBADA') NULL,
    estado_para_rendir ENUM('CURSADA','APROBADA') NULL,
    PRIMARY KEY (plan_materia_id, plan_materia_correlativa_id),
    -- Las dos FK son compuestas y comparten la columna plan_estudio_id. Eso es lo que obliga a que
    -- la materia y su correlativa sean del MISMO plan: con FK simples a plan_materia_id la base
    -- aceptaba "Probabilidad (plan Naval) requiere Programacion I (plan TUP 2003)".
    -- Apuntan al UNIQUE uq_pm_plan_id de plan_materia, no a su PK.
    -- El precio es la columna plan_estudio_id, que es redundante (ya sale de cualquiera de las dos
    -- plan_materia), pero las FK garantizan que no pueda quedar desincronizada.
    CONSTRAINT fk_corr_materia FOREIGN KEY (plan_estudio_id, plan_materia_id)
        REFERENCES plan_materia (plan_estudio_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_corr_correlativa FOREIGN KEY (plan_estudio_id, plan_materia_correlativa_id)
        REFERENCES plan_materia (plan_estudio_id, id) ON DELETE CASCADE,
    CONSTRAINT chk_no_autocorrelativa CHECK (plan_materia_id <> plan_materia_correlativa_id),
    CONSTRAINT chk_algun_estado CHECK (estado_para_cursar IS NOT NULL OR estado_para_rendir IS NOT NULL),
    CONSTRAINT chk_corr_rendir_no_menor CHECK (
        NOT (estado_para_cursar = 'APROBADA' AND estado_para_rendir = 'CURSADA')
    )
);

-- INSCRIPCIONES --

-- Guarda los datos de inscripcion de un alumno a una carrera (y a su plan de estudio correspondiente)
CREATE TABLE inscripcion_carrera (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    alumno_id BIGINT NOT NULL,
    plan_estudio_id BIGINT NOT NULL,
    fecha_inscripcion DATE NOT NULL DEFAULT (CURRENT_DATE),
    estado ENUM('ACTIVA','EGRESADO','BAJA') NOT NULL DEFAULT 'ACTIVA',
    fecha_estado DATE NULL,  -- solo se usa con EGRESADO o BAJA
    CONSTRAINT chk_inscripcion_estado_fecha CHECK (
        (estado =  'ACTIVA' AND fecha_estado IS NULL) OR
        (estado <> 'ACTIVA' AND fecha_estado IS NOT NULL)
    ),
    -- Nadie egresa ni se da de baja antes de haberse inscripto.
    CONSTRAINT chk_inscripcion_fecha_estado_posterior CHECK (
        fecha_estado IS NULL OR fecha_estado >= fecha_inscripcion
    ),
    FOREIGN KEY (alumno_id) REFERENCES persona (id) ON DELETE RESTRICT,
    FOREIGN KEY (plan_estudio_id) REFERENCES plan_estudio (id) ON DELETE RESTRICT,
    -- Un alumno no puede inscribirse dos veces al mismo plan: si no, las cuotas,
    -- las cursadas y los finales quedan repartidos entre las dos inscripciones.
    UNIQUE (alumno_id, plan_estudio_id)
);

-- Guarda los datos de una materia en una comision determinada, que se dicta en determinado periodo del año y en determinado año. Guarda el nro de comision y su cupo maximo
-- EJ: Matemática 1, Comisión 1, primer cuatrimestre de 2026, cupo 40 alumnos
CREATE TABLE comision(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    materia_id BIGINT NOT NULL,
    nro_comision INT NOT NULL,
    cupo_maximo INT,
    periodo_lectivo ENUM('PRIMER_CUATRIMESTRE','SEGUNDO_CUATRIMESTRE','ANUAL') NOT NULL,
    anio_lectivo SMALLINT NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE, -- Baja logica
    fecha_baja DATE NULL,
    UNIQUE (materia_id,nro_comision,periodo_lectivo,anio_lectivo),
    FOREIGN KEY (materia_id) REFERENCES materia (id) ON DELETE RESTRICT,
    CONSTRAINT chk_comision_baja CHECK (
        (activa = TRUE AND fecha_baja IS NULL) OR (activa = FALSE AND fecha_baja IS NOT NULL)
    ),
    CONSTRAINT chk_comision_numeros CHECK (
        nro_comision > 0 AND
        anio_lectivo BETWEEN 2000 AND 2100 AND
        (cupo_maximo IS NULL OR cupo_maximo > 0)   -- NULL = sin limite de cupo
    )
);

-- Guarda los datos de un profesor en una determinada materia de una determinada comision.
CREATE TABLE comision_profesor (
    comision_id BIGINT NOT NULL,
    profesor_id BIGINT NOT NULL,
    rol ENUM('TITULAR','AYUDANTE') NOT NULL,
    PRIMARY KEY (comision_id, profesor_id),
    FOREIGN KEY (comision_id) REFERENCES comision (id) ON DELETE CASCADE,
    FOREIGN KEY (profesor_id) REFERENCES persona (id) ON DELETE RESTRICT
);

CREATE TABLE aula (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE, 
    capacidad INT NULL,          -- NULL si no esta relevada
    activa BOOLEAN NOT NULL DEFAULT TRUE, -- Baja logica
    fecha_baja DATE NULL,
    CONSTRAINT chk_aula_baja CHECK (
        (activa = TRUE AND fecha_baja IS NULL) OR (activa = FALSE AND fecha_baja IS NOT NULL)
    ),
    CONSTRAINT chk_aula_capacidad CHECK (capacidad IS NULL OR capacidad > 0)
);

-- Guarda los datos de,dias que se cursa, horario de inicio, horario de fin y aula de una determinada materia en una determinada comisión
-- EJ: La Comisión 1 de Matemática 1 se cursa los martes de 8:30 a 10:30 en el aula 107, y los jueves de 11:00 a 13:00 en el aula 9
CREATE TABLE comision_horario (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    comision_id BIGINT NOT NULL,
    dia_semana ENUM('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO') NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
	aula_id BIGINT NULL, -- queda null por si la cursada es virtual
    FOREIGN KEY (comision_id) REFERENCES comision (id) ON DELETE CASCADE,
    FOREIGN KEY (aula_id) REFERENCES aula (id) ON DELETE RESTRICT,
    -- Evita cargar dos veces el mismo horario para una comision. Ojo: los solapamientos
    -- (lunes 8-10 y lunes 9-11) no los puede detectar un UNIQUE, quedan para el backend.
    UNIQUE (comision_id, dia_semana, hora_inicio),
    CONSTRAINT chk_horario_valido CHECK (hora_fin > hora_inicio)
);


-- Ventana de inscripcion de una carrera. Para las cursadas hay una por cuatrimestre;
-- para los finales, una por turno.
CREATE TABLE periodo_inscripcion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    carrera_id BIGINT NOT NULL,
    tipo ENUM('CURSADA','FINAL') NOT NULL,
    anio_lectivo SMALLINT NOT NULL,
    periodo_lectivo ENUM('PRIMER_CUATRIMESTRE','SEGUNDO_CUATRIMESTRE','ANUAL') NULL, -- Campo que solo se utiliza si es inscripcion a cursada.
    fecha_inicio DATETIME NOT NULL,
    fecha_cierre DATETIME NOT NULL,
    habilitada BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (carrera_id) REFERENCES carrera (id) ON DELETE RESTRICT,
    UNIQUE (carrera_id, tipo, fecha_inicio),
    -- Redundante contra la PK, pero es el destino de la FK compuesta de examen_final, que es la
    -- que obliga a que una mesa de examen cuelgue de un periodo de tipo FINAL y no de una cursada.
    UNIQUE uq_periodo_tipo (id, tipo),
    CONSTRAINT chk_periodo_fechas CHECK (fecha_cierre > fecha_inicio),
    CONSTRAINT chk_periodo_anio CHECK (anio_lectivo BETWEEN 2000 AND 2100),
    -- Una ventana de cursada dice a que cuatrimestre corresponde; una de finales no lo necesita.
    CONSTRAINT chk_periodo_tipo_lectivo CHECK (
        (tipo = 'CURSADA' AND periodo_lectivo IS NOT NULL) OR
        (tipo = 'FINAL'   AND periodo_lectivo IS NULL)
    )
);

-- Instancia de evaluacion de una cursada: pertenece a una comision (docente, materia y
-- año/cuatrimestre salen de la comision).
CREATE TABLE evaluacion_cursada (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    comision_id BIGINT NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    fecha DATE NOT NULL,
    -- QUE instrumento es. No incluye 'RECUPERATORIO' a proposito: ser recuperatorio no es una
    -- clase de evaluacion sino una relacion con otra, y esa relacion ya la expresa la columna de
    -- abajo.
    tipo ENUM('PARCIAL','TRABAJO_PRACTICO','TRABAJO_FINAL','EXPOSICION_ORAL','COLOQUIO')
         NOT NULL DEFAULT 'PARCIAL',
    -- A QUE evaluacion recupera. NULL = es una instancia original;
    -- NO NULL = es el recuperatorio de aquella.
    evaluacion_recuperada_id BIGINT NULL,
    UNIQUE (comision_id, nombre),
    -- Redundante contra la PK, pero es el destino de la FK compuesta de abajo.
    UNIQUE uq_eval_comision_id (comision_id, id),
    FOREIGN KEY (comision_id) REFERENCES comision (id) ON DELETE RESTRICT,
    CONSTRAINT fk_eval_recuperada FOREIGN KEY (comision_id, evaluacion_recuperada_id)
        REFERENCES evaluacion_cursada (comision_id, id) ON DELETE RESTRICT
);

-- Final: institucional, pertenece a la materia
CREATE TABLE examen_final (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    materia_id BIGINT NOT NULL,
    periodo_id BIGINT NOT NULL,  -- Turno al que pertenece la mesa.
    -- Columna generada, siempre 'FINAL': no se escribe, se calcula, asi que no puede tener otro
    -- valor. Existe solo para poder armar la FK compuesta de abajo, que es la que impide colgar
    -- una mesa de examen de un periodo de tipo CURSADA. No hace falta nombrarla en los INSERT.
    tipo_periodo ENUM('CURSADA','FINAL') AS ('FINAL') STORED,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    aula_id BIGINT NULL, -- null si la carrera es virtual
    UNIQUE (materia_id, fecha, hora_inicio),
    CONSTRAINT chk_horario_final_valido CHECK (hora_fin > hora_inicio),
    FOREIGN KEY (materia_id) REFERENCES materia (id) ON DELETE RESTRICT,
    FOREIGN KEY (aula_id) REFERENCES aula (id) ON DELETE RESTRICT,
    CONSTRAINT fk_final_periodo FOREIGN KEY (periodo_id, tipo_periodo)
        REFERENCES periodo_inscripcion (id, tipo) ON DELETE RESTRICT
);

-- Vincula examen final con los profesores que van a estar en la mesa.
CREATE TABLE examen_final_profesor (
    examen_final_id BIGINT NOT NULL,
    profesor_id BIGINT NOT NULL,
    PRIMARY KEY (examen_final_id, profesor_id),
    FOREIGN KEY (examen_final_id) REFERENCES examen_final (id) ON DELETE CASCADE,
    FOREIGN KEY (profesor_id) REFERENCES persona (id) ON DELETE RESTRICT
);

CREATE TABLE alumno_inscripcion_comision (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inscripcion_carrera_id BIGINT NOT NULL,
    comision_id BIGINT NOT NULL,
    periodo_id BIGINT NOT NULL,   -- ventana de inscripcion en la que se anoto
    -- Columna generada, siempre 'CURSADA': no se escribe, se calcula, asi que no puede tener otro
    -- valor. Existe solo para armar la FK compuesta de abajo, que es la que impide anotarse a una
    -- cursada usando un periodo de inscripcion a FINALES. No hace falta nombrarla en los INSERT.
    -- Misma tecnica que examen_final.tipo_periodo, pero al reves: alla se fija 'FINAL'.
    tipo_periodo ENUM('CURSADA','FINAL') AS ('CURSADA') STORED,
    fecha_inscripcion DATE NOT NULL DEFAULT (CURRENT_DATE),
    -- El alumno puede darse de baja de la cursada. Antes la unica salida era borrar la fila, y eso
    -- no dejaba rastro: no se distinguia "nunca se anoto" de "se anoto y se dio de baja en abril".
    -- Ademas el DELETE deja de ser posible apenas hay una nota cargada (FK RESTRICT), asi que la
    -- baja tardia no tenia forma de registrarse. Mismo patron que inscripcion_carrera.estado.
    -- OJO: toda cuenta de inscriptos (el cupo, sobre todo) debe filtrar estado = 'INSCRIPTO'.
    estado ENUM('INSCRIPTO','BAJA') NOT NULL DEFAULT 'INSCRIPTO',
    fecha_baja DATE NULL,
    CONSTRAINT chk_aic_baja CHECK (
        (estado = 'INSCRIPTO' AND fecha_baja IS NULL) OR
        (estado = 'BAJA'      AND fecha_baja IS NOT NULL)
    ),
    CONSTRAINT chk_aic_baja_posterior CHECK (fecha_baja IS NULL OR fecha_baja >= fecha_inscripcion),
    UNIQUE (inscripcion_carrera_id, comision_id),
    -- Redundante contra la PK (id ya es unico por si solo), pero es el destino de la FK compuesta
    -- de nota_evaluacion, que es la que obliga a que la nota y la evaluacion sean de la misma
    -- comision. Una FK solo puede apuntar a columnas indexadas y unicas en la tabla destino.
    UNIQUE uq_aic_comision_id (comision_id, id),
    FOREIGN KEY (inscripcion_carrera_id) REFERENCES inscripcion_carrera (id) ON DELETE RESTRICT,
    FOREIGN KEY (comision_id) REFERENCES comision (id) ON DELETE RESTRICT,
    -- Apunta al UNIQUE uq_periodo_tipo de periodo_inscripcion, no a su PK. Reemplaza a la FK
    -- simple contra periodo_inscripcion(id), que aceptaba cualquier periodo.
    CONSTRAINT fk_aic_periodo FOREIGN KEY (periodo_id, tipo_periodo)
        REFERENCES periodo_inscripcion (id, tipo) ON DELETE RESTRICT
);
	    
CREATE TABLE alumno_inscripcion_examen_final (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inscripcion_carrera_id BIGINT NOT NULL,
    examen_final_id BIGINT NOT NULL,
    fecha_inscripcion DATE NOT NULL DEFAULT (CURRENT_DATE),
    -- Baja logica, igual que en la cursada: el alumno puede bajarse de la mesa y queda el rastro.
    -- Las reglas que cruzan tablas van al backend: la baja tiene que ser ANTES de la fecha de la
    -- mesa (examen_final.fecha), y una inscripcion en BAJA no puede tener nota_final.
    -- OJO: el acta de la mesa y cualquier conteo de inscriptos deben filtrar estado = 'INSCRIPTO'.
    estado ENUM('INSCRIPTO','BAJA') NOT NULL DEFAULT 'INSCRIPTO',
    fecha_baja DATE NULL,
    CONSTRAINT chk_aief_baja CHECK (
        (estado = 'INSCRIPTO' AND fecha_baja IS NULL) OR
        (estado = 'BAJA'      AND fecha_baja IS NOT NULL)
    ),
    CONSTRAINT chk_aief_baja_posterior CHECK (fecha_baja IS NULL OR fecha_baja >= fecha_inscripcion),
    UNIQUE (inscripcion_carrera_id, examen_final_id),
    FOREIGN KEY (inscripcion_carrera_id) REFERENCES inscripcion_carrera (id) ON DELETE RESTRICT,
    FOREIGN KEY (examen_final_id) REFERENCES examen_final (id) ON DELETE RESTRICT
);

CREATE TABLE nota_final (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inscripcion_examen_final_id BIGINT NOT NULL,
    profesor_id BIGINT NOT NULL,
    condicion ENUM('PRESENTE','AUSENTE') NOT NULL DEFAULT 'PRESENTE',
    nota TINYINT NULL, -- Escala entera 0..10. Si el alumno esta ausente la nota puede ser nula.
    fecha_registro DATE NOT NULL DEFAULT (CURRENT_DATE),
    -- APPEND-ONLY, igual que cierre_cursada: la nota no se actualiza nunca. Corregir un 4 por un 7
    -- es insertar otra fila apuntando a la anterior, y la vigente es la ultima de la cadena. Asi
    -- queda el historial completo de correcciones, y el "quien" sale gratis: profesor_id ya es una
    -- columna de la fila, no hay que sacarlo de un trigger (donde solo se ve el usuario de la BD).
    nota_rectificada_id BIGINT NULL,   -- la nota que esta corrige, si es una rectificacion
    -- Vale 1 en las notas originales y NULL en las rectificaciones. Como dos NULL nunca chocan en
    -- un indice unico, el UNIQUE de abajo permite muchas rectificaciones pero una sola nota
    -- original por inscripcion: sin esto la cadena podria bifurcarse.
    es_original TINYINT AS (IF(nota_rectificada_id IS NULL, 1, NULL)) VIRTUAL,
    UNIQUE (inscripcion_examen_final_id, es_original),
    UNIQUE (nota_rectificada_id),      -- una nota se rectifica una sola vez: la cadena es lineal
    FOREIGN KEY (inscripcion_examen_final_id) REFERENCES alumno_inscripcion_examen_final (id) ON DELETE RESTRICT,
    FOREIGN KEY (nota_rectificada_id) REFERENCES nota_final (id) ON DELETE RESTRICT,
    FOREIGN KEY (profesor_id) REFERENCES persona (id) ON DELETE RESTRICT,
    CONSTRAINT chk_nota_final CHECK (nota BETWEEN 0 AND 10),
    CONSTRAINT chk_condicion_nota_final CHECK (
        (condicion = 'AUSENTE'  AND nota IS NULL) OR
        (condicion = 'PRESENTE' AND nota IS NOT NULL)
    )
);

CREATE TABLE nota_evaluacion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    -- Comision a la que pertenecen TANTO la evaluacion COMO la inscripcion del alumno. Es
    -- redundante (sale de cualquiera de las dos), pero las dos FK compuestas de abajo la comparten
    -- y por eso no puede quedar desincronizada
    comision_id BIGINT NOT NULL,
    evaluacion_cursada_id BIGINT NOT NULL,
    inscripcion_comision_id BIGINT NOT NULL,
    profesor_id BIGINT NOT NULL,
    condicion ENUM('PRESENTE','AUSENTE') NOT NULL DEFAULT 'PRESENTE',
    nota TINYINT NULL,   -- escala entera 0..10
    fecha_registro DATE NOT NULL DEFAULT (CURRENT_DATE),
    -- APPEND-ONLY, mismo criterio que nota_final y cierre_cursada. Ver el comentario de nota_final.
    nota_rectificada_id BIGINT NULL,
    es_original TINYINT AS (IF(nota_rectificada_id IS NULL, 1, NULL)) VIRTUAL,
    UNIQUE (evaluacion_cursada_id, inscripcion_comision_id, es_original),
    UNIQUE (nota_rectificada_id),
    FOREIGN KEY (nota_rectificada_id) REFERENCES nota_evaluacion (id) ON DELETE RESTRICT,
    -- Reemplazan a las dos FK simples contra evaluacion_cursada(id) y alumno_inscripcion_comision(id).
    -- Apuntan a uq_eval_comision_id y uq_aic_comision_id, no a las PK.
    CONSTRAINT fk_nota_eval_evaluacion FOREIGN KEY (comision_id, evaluacion_cursada_id)
        REFERENCES evaluacion_cursada (comision_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_nota_eval_inscripcion FOREIGN KEY (comision_id, inscripcion_comision_id)
        REFERENCES alumno_inscripcion_comision (comision_id, id) ON DELETE RESTRICT,
    FOREIGN KEY (profesor_id) REFERENCES persona (id) ON DELETE RESTRICT,
    CONSTRAINT chk_nota_evaluacion CHECK (nota BETWEEN 0 AND 10),
    CONSTRAINT chk_condicion_nota_evaluacion CHECK (
        (condicion = 'AUSENTE'  AND nota IS NULL) OR
        (condicion = 'PRESENTE' AND nota IS NOT NULL)
    )
);

-- CIERRE DE CURSADA (regularidad) --

-- Acta de cierre de cursada: el resultado de que un alumno haya cursado una comision. 
CREATE TABLE cierre_cursada (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inscripcion_comision_id BIGINT NOT NULL,
    profesor_id BIGINT NOT NULL,        -- docente que firma el acta
    condicion ENUM('REGULAR','PROMOCIONADO','NO_REGULAR') NOT NULL,
    nota_cursada TINYINT NULL,                   -- escala entera 0..10: nota de promocion
    fecha_cierre DATE NOT NULL,                  -- fecha del acta
    -- Se guarda como dato del acta y no se calcula con la regla de hoy: asi un cambio de
    -- reglamento no reescribe el pasado. Solo tiene sentido para REGULAR (la promocion no vence,
    -- y NO_REGULAR es terminal).
    fecha_vencimiento DATE NULL,
    cierre_rectificado_id BIGINT NULL,   -- acta que esta corrige, si es una rectificacion
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Columna generada: vale 1 en las actas originales y NULL en las rectificaciones. Como dos NULL
    -- nunca chocan en un indice unico, el UNIQUE de abajo permite muchas rectificaciones pero una
    -- sola acta original por cursada. Sin esto la cadena podria bifurcarse y no habria un
    -- "acta vigente" bien definida.
    es_original TINYINT AS (IF(cierre_rectificado_id IS NULL, 1, NULL)) VIRTUAL,
    UNIQUE (inscripcion_comision_id, es_original),
    UNIQUE (cierre_rectificado_id),               -- un acta se rectifica una sola vez: la cadena es lineal

    FOREIGN KEY (inscripcion_comision_id) REFERENCES alumno_inscripcion_comision (id) ON DELETE RESTRICT,
    FOREIGN KEY (profesor_id) REFERENCES persona (id) ON DELETE RESTRICT,
    FOREIGN KEY (cierre_rectificado_id) REFERENCES cierre_cursada (id) ON DELETE RESTRICT,

    CONSTRAINT chk_cierre_nota CHECK (nota_cursada BETWEEN 0 AND 10),
    -- La promocion siempre lleva nota. REGULAR y NO_REGULAR pueden llevarla o no: NO_REGULAR
    -- agrupa al que quedo libre por nota (tiene una) y al que abandono (no tiene ninguna).
    CONSTRAINT chk_cierre_condicion_nota CHECK (
        (condicion = 'PROMOCIONADO' AND nota_cursada IS NOT NULL) OR
        (condicion IN ('REGULAR','NO_REGULAR'))
    ),
    -- Solo la regularidad vence.
    CONSTRAINT chk_cierre_vencimiento CHECK (
        (condicion =  'REGULAR' AND fecha_vencimiento IS NOT NULL AND fecha_vencimiento > fecha_cierre) OR
        (condicion <> 'REGULAR' AND fecha_vencimiento IS NULL)
    )
);

-- AVISOS
CREATE TABLE aviso (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    autor_id BIGINT NOT NULL,     -- quien publica el aviso
    titulo VARCHAR(255) NOT NULL,
    contenido TEXT NOT NULL,
    fecha_publicacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Hasta cuando tiene sentido mostrarlo. NULL = no vence. Sirve para que un aviso del tipo
    -- "inscripcion abierta hasta el 15" se apague solo, sin que nadie tenga que acordarse de
    -- ponerle activo = FALSE. 'activo' queda para bajarlo a mano antes de tiempo.
    fecha_vencimiento DATETIME NULL,
    alcance ENUM('GENERAL','CARRERA','COMISION') NOT NULL,  -- A quien va dirigido.
    carrera_id BIGINT NULL,  -- solo si alcance = 'CARRERA'
    comision_id BIGINT NULL,  -- solo si alcance = 'COMISION'
    activo BOOLEAN NOT NULL DEFAULT TRUE, -- para bajar un aviso a mano, antes de que venza
    FOREIGN KEY (autor_id) REFERENCES persona (id) ON DELETE RESTRICT,
    FOREIGN KEY (carrera_id) REFERENCES carrera (id) ON DELETE RESTRICT,
    FOREIGN KEY (comision_id) REFERENCES comision (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aviso_vencimiento CHECK (
        fecha_vencimiento IS NULL OR fecha_vencimiento > fecha_publicacion
    ),
    -- Cada alcance usa exactamente su columna y deja la otra en NULL. Sin esto se podria cargar
    -- un aviso 'GENERAL' apuntando a una comision, o uno 'CARRERA' sin decir a cual.
    CONSTRAINT chk_aviso_alcance CHECK (
        (alcance = 'GENERAL'  AND carrera_id IS NULL     AND comision_id IS NULL) OR
        (alcance = 'CARRERA'  AND carrera_id IS NOT NULL AND comision_id IS NULL) OR
        (alcance = 'COMISION' AND carrera_id IS NULL     AND comision_id IS NOT NULL)
    )
);

-- CUOTAS

CREATE TABLE cuota (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inscripcion_carrera_id BIGINT NOT NULL,
    mes TINYINT NOT NULL, -- 1..12 para las cuotas; 0 para la matricula, que es anual.
    anio SMALLINT NOT NULL,
    valor_cuota DECIMAL(10,2) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    fecha_emision DATE NOT NULL DEFAULT (CURRENT_DATE),
    concepto_cuota ENUM('CUOTA','MATRICULA') NOT NULL,
    FOREIGN KEY (inscripcion_carrera_id) REFERENCES inscripcion_carrera (id) ON DELETE RESTRICT,
    UNIQUE (inscripcion_carrera_id, concepto_cuota, mes, anio),
    CONSTRAINT chk_mes_valido CHECK (
        (concepto_cuota = 'CUOTA' AND mes BETWEEN 1 AND 12) OR
        (concepto_cuota = 'MATRICULA' AND mes = 0)
  ),
    CONSTRAINT chk_valor_positivo CHECK (valor_cuota > 0),
    CONSTRAINT chk_cuota_anio CHECK (anio BETWEEN 2000 AND 2100)
);

CREATE TABLE pago_cuota (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cuota_id BIGINT NOT NULL UNIQUE,
    monto_pagado DECIMAL(10,2) NOT NULL,
    fecha_pago DATE NOT NULL DEFAULT (CURRENT_DATE),
    FOREIGN KEY (cuota_id) REFERENCES cuota (id) ON DELETE RESTRICT,
    CONSTRAINT chk_monto_positivo CHECK (monto_pagado > 0)
);

-- ENCUESTAS --

-- Representa el lanzamiento de una encuesta para una comisión determinada.
CREATE TABLE encuesta (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    comision_id BIGINT NOT NULL,
    fecha_apertura DATETIME NOT NULL,
    fecha_cierre DATETIME NOT NULL,
    FOREIGN KEY (comision_id) REFERENCES comision (id) ON DELETE RESTRICT,
    UNIQUE (comision_id),
    CONSTRAINT chk_fechas_encuesta CHECK (fecha_cierre > fecha_apertura)
);

-- Banco de preguntas fijas, global para todo el sistema. Se reutilizan en todas las encuestas.
CREATE TABLE pregunta_encuesta (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    orden INT NOT NULL,
    enunciado VARCHAR(300) NOT NULL,
    -- Una pregunta que ya tiene respuestas no se puede borrar (la FK de respuesta_pregunta es
    -- RESTRICT, y esta bien: se perderian las encuestas viejas). 'activa' es la forma de retirarla
    -- del banco: las encuestas nuevas dejan de incluirla y las viejas quedan intactas.
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (orden),
    CONSTRAINT chk_pregunta_orden CHECK (orden > 0)
);

-- Representa que un alumno (anónimo) completó una encuesta. hash_alumno evita respuestas duplicadas
-- sin permitir reconstruir la identidad del alumno.
CREATE TABLE respuesta_encuesta (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    encuesta_id BIGINT NOT NULL,
    hash_alumno VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    fecha_respuesta DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    comentario_final VARCHAR(1000) NULL,
    FOREIGN KEY (encuesta_id) REFERENCES encuesta (id) ON DELETE RESTRICT,
    UNIQUE (encuesta_id, hash_alumno)
);

-- Detalle de calificaciones por pregunta dentro de una respuesta_encuesta.
CREATE TABLE respuesta_pregunta (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    respuesta_encuesta_id BIGINT NOT NULL,
    pregunta_encuesta_id BIGINT NOT NULL,
    calificacion TINYINT NOT NULL,
    FOREIGN KEY (respuesta_encuesta_id) REFERENCES respuesta_encuesta (id) ON DELETE CASCADE,
    FOREIGN KEY (pregunta_encuesta_id) REFERENCES pregunta_encuesta (id) ON DELETE RESTRICT,
    UNIQUE (respuesta_encuesta_id, pregunta_encuesta_id),
    CONSTRAINT chk_calificacion CHECK (calificacion BETWEEN 1 AND 5)
);

INSERT INTO pregunta_encuesta (orden, enunciado) VALUES
(1, '¿El profesor cumplió con los temas del programa?'),
(2, '¿El profesor explicó con claridad?'),
(3, '¿El profesor estuvo disponible para consultas?'),
(4, '¿La cursada cumplió tus expectativas generales?'),
(5, '¿Recomendarías esta materia/comisión?');

-- BOLETO ESTUDIANTIL

CREATE TABLE boleto_estudiantil (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    alumno_id BIGINT NOT NULL,
    estado ENUM('SOLICITADO','APROBADO','RECHAZADO') NOT NULL DEFAULT 'SOLICITADO',
    fecha_solicitud DATE NOT NULL DEFAULT (CURRENT_DATE),
    FOREIGN KEY (alumno_id) REFERENCES persona (id) ON DELETE RESTRICT
);

-- INSERTS DE SEGURIDAD --

-- Administrador inicial, para poder loguearse en un sistema vacio.
-- usuario: admin / contrasena: admin123 (hash BCrypt). SOLO PARA DESARROLLO: en produccion se
-- cambia la contrasena apenas se instala.
INSERT INTO persona (id, nombre, apellido, tipo_documento, nro_documento, telefono, fecha_nacimiento, email, legajo_admin) VALUES
(1, 'Administrador', 'Sistema', 'DNI', '00000000', '0000000000', '2000-01-01', 'admin@atenea.local', 1);

INSERT INTO usuario (id, persona_id, username, password) VALUES
(1, 1, 'admin', '$2a$10$N0Iz328VrX8JHJ5am0MYZ.Kb9rgTR9sCnqe6aTVCIXSVYZC8bt6ZK');

INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT 1, id FROM rol WHERE nombre = 'ROLE_ADMIN';

-- INSERTS DE CARRERAS -- 

INSERT INTO carrera (id, nombre, duracion, titulo_otorgado, modalidad_carrera) VALUES
(1, 'Ingeniería Naval', '6 años', 'Ingeniero Naval', 'PRESENCIAL'),
(2, 'Ingeniería Pesquera (Orientación Procesamiento)', '5 años', 'Ingeniero Pesquero', 'PRESENCIAL'),
(3, 'Tecnicatura Universitaria en Programación', '2 años', 'Técnico Universitario en Programación', 'PRESENCIAL');
 
INSERT INTO plan_estudio (id, carrera_id, nombre, anio_inicio) VALUES
(1, 1, 'Plan de Estudios Ingeniería Naval', 2004),
(2, 2, 'Plan de Estudios Ingeniería Pesquera', 2004),
(3, 3, 'Plan 2024', 2024),
(4, 3, 'Plan 2003', 2003);
 
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
(126, 'Práctica Profesional', 0, 60);
 
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
(133, 4, 126, NULL,      2, 'SEGUNDO_CUATRIMESTRE'); -- Práctica Profesional
 
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


-- -- TRIGGERS: hacen que los tres registros academicos sean realmente append-only.
-- -- Van de a pares (UPDATE y DELETE) por cada tabla. Todos hacen lo mismo: cortar con un error.

-- -- cierre_cursada (actas de cursada)
 CREATE TRIGGER trg_cierre_cursada_no_update BEFORE UPDATE ON cierre_cursada
 FOR EACH ROW SIGNAL SQLSTATE '45000'
     SET MESSAGE_TEXT = 'cierre_cursada es append-only: para corregir un acta, insertar otra con cierre_rectificado_id.';

 CREATE TRIGGER trg_cierre_cursada_no_delete BEFORE DELETE ON cierre_cursada
 FOR EACH ROW SIGNAL SQLSTATE '45000'
     SET MESSAGE_TEXT = 'cierre_cursada es append-only: un acta de cursada no se borra.';

-- -- nota_final (notas de examen final)
 CREATE TRIGGER trg_nota_final_no_update BEFORE UPDATE ON nota_final
 FOR EACH ROW SIGNAL SQLSTATE '45000'
     SET MESSAGE_TEXT = 'nota_final es append-only: para corregir una nota, insertar otra con nota_rectificada_id.';

 CREATE TRIGGER trg_nota_final_no_delete BEFORE DELETE ON nota_final
 FOR EACH ROW SIGNAL SQLSTATE '45000'
     SET MESSAGE_TEXT = 'nota_final es append-only: una nota de final no se borra.';

-- -- nota_evaluacion (notas de parciales, trabajos, coloquios)
 CREATE TRIGGER trg_nota_evaluacion_no_update BEFORE UPDATE ON nota_evaluacion
 FOR EACH ROW SIGNAL SQLSTATE '45000'
     SET MESSAGE_TEXT = 'nota_evaluacion es append-only: para corregir una nota, insertar otra con nota_rectificada_id.';

 CREATE TRIGGER trg_nota_evaluacion_no_delete BEFORE DELETE ON nota_evaluacion
 FOR EACH ROW SIGNAL SQLSTATE '45000'
     SET MESSAGE_TEXT = 'nota_evaluacion es append-only: una nota de evaluacion no se borra.';
