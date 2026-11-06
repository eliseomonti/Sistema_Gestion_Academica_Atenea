DROP DATABASE IF EXISTS atenea_db;

SET NAMES utf8mb4;
SET default_storage_engine = InnoDB;
SET time_zone = '-03:00';

CREATE DATABASE IF NOT EXISTS atenea_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE atenea_db;

-- Unica tabla donde se guardan todas las personas. El manejo de roles (admin, docente o estudiante) queda a cargo de SpringSecurity y la tabla usuario_rol.
CREATE TABLE persona (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
	dni VARCHAR(10) NOT NULL UNIQUE,
    telefono VARCHAR(50) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
	legajo_estudiante BIGINT NULL UNIQUE,
    legajo_docente BIGINT NULL UNIQUE,
    legajo_admin BIGINT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,  -- Baja logica: una persona nunca se borra (el egresado, el que abandono y el docente
    fecha_baja DATE NULL,					-- jubilado siguen existiendo).
    CONSTRAINT chk_persona_baja CHECK (
        (activo = TRUE AND fecha_baja IS NULL) OR (activo = FALSE AND fecha_baja IS NOT NULL)
    ),
    CONSTRAINT chk_persona_legajos CHECK (
        (legajo_estudiante IS NULL OR legajo_estudiante > 0) AND
        (legajo_docente    IS NULL OR legajo_docente    > 0) AND
        (legajo_admin      IS NULL OR legajo_admin      > 0)
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

INSERT INTO rol (nombre) VALUES ('ROLE_ADMIN'), ('ROLE_DOCENTE'), ('ROLE_ESTUDIANTE');

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

-- Guarda los datos de inscripcion de un estudiante a una carrera (y a su plan de estudio correspondiente)
CREATE TABLE inscripcion_carrera (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    estudiante_id BIGINT NOT NULL,
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
    FOREIGN KEY (estudiante_id) REFERENCES persona (id) ON DELETE RESTRICT,
    FOREIGN KEY (plan_estudio_id) REFERENCES plan_estudio (id) ON DELETE RESTRICT,
    -- Un estudiante no puede inscribirse dos veces al mismo plan: si no, las cuotas,
    -- las cursadas y los finales quedan repartidos entre las dos inscripciones.
    UNIQUE (estudiante_id, plan_estudio_id)
);

-- Grupo de estudiantes de un plan que cursa junto las materias de un año de la carrera, en un turno
-- y año lectivo determinados. Las materias que cursa estan en comision_materia, cada una con su
-- periodo: un mismo grupo puede tener materias anuales y cuatrimestrales.
-- EJ: TUP Plan 2024, Comisión 1 de 2° año, turno mañana, 2026.
-- anio_cursado es parte del UNIQUE porque el numero se repite entre años: en un mismo año lectivo
-- conviven la Comisión 1 de 1° año (la "1/26", cohorte 2026) y la de 2° año (la "1/25").
CREATE TABLE comision(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    plan_estudio_id BIGINT NOT NULL,
    anio_cursado TINYINT NOT NULL, -- año de la carrera, como plan_materia.anio_cursado
    nro_comision INT NOT NULL,
    turno ENUM('MANANA','MEDIA_MANANA','TARDE','NOCHE') NOT NULL,
    anio_lectivo SMALLINT NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE, -- Baja logica
    fecha_baja DATE NULL,
    UNIQUE (plan_estudio_id, anio_cursado, nro_comision, anio_lectivo),
    FOREIGN KEY (plan_estudio_id) REFERENCES plan_estudio (id) ON DELETE RESTRICT,
    CONSTRAINT chk_comision_baja CHECK (
        (activa = TRUE AND fecha_baja IS NULL) OR (activa = FALSE AND fecha_baja IS NOT NULL)
    ),
    CONSTRAINT chk_comision_numeros CHECK (
        nro_comision > 0 AND
        anio_cursado BETWEEN 1 AND 7 AND
        anio_lectivo BETWEEN 2000 AND 2100
    )
);

-- Una materia que se dicta en una comision, con su cupo. Es la unidad a la que se inscribe el
-- estudiante: un recursante puede cursar una materia en otra comision que el resto.
-- De aca cuelgan los docentes, los horarios, las evaluaciones y las inscripciones.
-- EJ: Programación II en la Comisión 1, segundo cuatrimestre, cupo 40 estudiantes.
CREATE TABLE comision_materia (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    comision_id BIGINT NOT NULL,
    materia_id BIGINT NOT NULL,
    -- Copia de plan_materia.periodo_lectivo, que el backend pone al agregar la materia. Se guarda
    -- aca porque comisiones lo consulta seguido (choques de aula y de docente, filtros) y
    -- plan_materia es de otro modulo.
    periodo_lectivo ENUM('PRIMER_CUATRIMESTRE','SEGUNDO_CUATRIMESTRE','ANUAL') NOT NULL,
    cupo_maximo INT NULL, -- NULL = sin limite de cupo
    UNIQUE (comision_id, materia_id),
    FOREIGN KEY (comision_id) REFERENCES comision (id) ON DELETE CASCADE,
    FOREIGN KEY (materia_id) REFERENCES materia (id) ON DELETE RESTRICT,
    CONSTRAINT chk_cm_cupo CHECK (cupo_maximo IS NULL OR cupo_maximo > 0)
);

-- Guarda los datos de un docente en una determinada materia de una determinada comision.
CREATE TABLE comision_docente (
    comision_materia_id BIGINT NOT NULL,
    docente_id BIGINT NOT NULL,
    rol ENUM('TITULAR','AYUDANTE') NOT NULL,
    PRIMARY KEY (comision_materia_id, docente_id),
    FOREIGN KEY (comision_materia_id) REFERENCES comision_materia (id) ON DELETE CASCADE,
    FOREIGN KEY (docente_id) REFERENCES persona (id) ON DELETE RESTRICT
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
    comision_materia_id BIGINT NOT NULL,
    dia_semana ENUM('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO') NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
	aula_id BIGINT NULL, -- queda null por si la cursada es virtual
    FOREIGN KEY (comision_materia_id) REFERENCES comision_materia (id) ON DELETE CASCADE,
    FOREIGN KEY (aula_id) REFERENCES aula (id) ON DELETE RESTRICT,
    -- Evita cargar dos veces el mismo horario para una materia de la comision. Ojo: los solapamientos
    -- (lunes 8-10 y lunes 9-11) no los puede detectar un UNIQUE, quedan para el backend.
    UNIQUE (comision_materia_id, dia_semana, hora_inicio),
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

-- Instancia de evaluacion de una cursada: pertenece a una materia de una comision (docente, materia y
-- año/cuatrimestre salen de comision_materia y de su comision).
CREATE TABLE evaluacion_cursada (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    comision_materia_id BIGINT NOT NULL,
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
    UNIQUE (comision_materia_id, nombre),
    -- Redundante contra la PK, pero es el destino de la FK compuesta de abajo.
    UNIQUE uq_eval_comision_materia_id (comision_materia_id, id),
    FOREIGN KEY (comision_materia_id) REFERENCES comision_materia (id) ON DELETE RESTRICT,
    CONSTRAINT fk_eval_recuperada FOREIGN KEY (comision_materia_id, evaluacion_recuperada_id)
        REFERENCES evaluacion_cursada (comision_materia_id, id) ON DELETE RESTRICT
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

-- Vincula examen final con los docentes que van a estar en la mesa.
CREATE TABLE examen_final_docente (
    examen_final_id BIGINT NOT NULL,
    docente_id BIGINT NOT NULL,
    PRIMARY KEY (examen_final_id, docente_id),
    FOREIGN KEY (examen_final_id) REFERENCES examen_final (id) ON DELETE CASCADE,
    FOREIGN KEY (docente_id) REFERENCES persona (id) ON DELETE RESTRICT
);

CREATE TABLE estudiante_inscripcion_comision (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inscripcion_carrera_id BIGINT NOT NULL,
    comision_materia_id BIGINT NOT NULL,
    periodo_id BIGINT NOT NULL,   -- ventana de inscripcion en la que se anoto
    -- Columna generada, siempre 'CURSADA': no se escribe, se calcula, asi que no puede tener otro
    -- valor. Existe solo para armar la FK compuesta de abajo, que es la que impide anotarse a una
    -- cursada usando un periodo de inscripcion a FINALES. No hace falta nombrarla en los INSERT.
    -- Misma tecnica que examen_final.tipo_periodo, pero al reves: alla se fija 'FINAL'.
    tipo_periodo ENUM('CURSADA','FINAL') AS ('CURSADA') STORED,
    fecha_inscripcion DATE NOT NULL DEFAULT (CURRENT_DATE),
    -- El estudiante puede darse de baja de la cursada. Antes la unica salida era borrar la fila, y eso
    -- no dejaba rastro: no se distinguia "nunca se anoto" de "se anoto y se dio de baja en abril".
    -- Ademas el DELETE deja de ser posible apenas hay una nota cargada (FK RESTRICT), asi que la
    -- baja tardia no tenia forma de registrarse. Mismo patron que inscripcion_carrera.estado.
    -- OJO: toda cuenta de inscriptos (el cupo, sobre todo) debe filtrar estado = 'INSCRIPTO'.
    estado ENUM('INSCRIPTO','BAJA') NOT NULL DEFAULT 'INSCRIPTO',
    fecha_baja DATE NULL,
    CONSTRAINT chk_eic_baja CHECK (
        (estado = 'INSCRIPTO' AND fecha_baja IS NULL) OR
        (estado = 'BAJA'      AND fecha_baja IS NOT NULL)
    ),
    CONSTRAINT chk_eic_baja_posterior CHECK (fecha_baja IS NULL OR fecha_baja >= fecha_inscripcion),
    UNIQUE (inscripcion_carrera_id, comision_materia_id),
    -- Redundante contra la PK (id ya es unico por si solo), pero es el destino de la FK compuesta
    -- de nota_evaluacion, que es la que obliga a que la nota y la evaluacion sean de la misma
    -- materia de la misma comision. Una FK solo puede apuntar a columnas indexadas y unicas en la tabla destino.
    UNIQUE uq_eic_comision_materia_id (comision_materia_id, id),
    FOREIGN KEY (inscripcion_carrera_id) REFERENCES inscripcion_carrera (id) ON DELETE RESTRICT,
    FOREIGN KEY (comision_materia_id) REFERENCES comision_materia (id) ON DELETE RESTRICT,
    -- Apunta al UNIQUE uq_periodo_tipo de periodo_inscripcion, no a su PK. Reemplaza a la FK
    -- simple contra periodo_inscripcion(id), que aceptaba cualquier periodo.
    CONSTRAINT fk_eic_periodo FOREIGN KEY (periodo_id, tipo_periodo)
        REFERENCES periodo_inscripcion (id, tipo) ON DELETE RESTRICT
);
	    
CREATE TABLE estudiante_inscripcion_examen_final (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inscripcion_carrera_id BIGINT NOT NULL,
    examen_final_id BIGINT NOT NULL,
    fecha_inscripcion DATE NOT NULL DEFAULT (CURRENT_DATE),
    -- Baja logica, igual que en la cursada: el estudiante puede bajarse de la mesa y queda el rastro.
    -- Las reglas que cruzan tablas van al backend: la baja tiene que ser ANTES de la fecha de la
    -- mesa (examen_final.fecha), y una inscripcion en BAJA no puede tener nota_final.
    -- OJO: el acta de la mesa y cualquier conteo de inscriptos deben filtrar estado = 'INSCRIPTO'.
    estado ENUM('INSCRIPTO','BAJA') NOT NULL DEFAULT 'INSCRIPTO',
    fecha_baja DATE NULL,
    CONSTRAINT chk_eief_baja CHECK (
        (estado = 'INSCRIPTO' AND fecha_baja IS NULL) OR
        (estado = 'BAJA'      AND fecha_baja IS NOT NULL)
    ),
    CONSTRAINT chk_eief_baja_posterior CHECK (fecha_baja IS NULL OR fecha_baja >= fecha_inscripcion),
    UNIQUE (inscripcion_carrera_id, examen_final_id),
    FOREIGN KEY (inscripcion_carrera_id) REFERENCES inscripcion_carrera (id) ON DELETE RESTRICT,
    FOREIGN KEY (examen_final_id) REFERENCES examen_final (id) ON DELETE RESTRICT
);

CREATE TABLE nota_final (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inscripcion_examen_final_id BIGINT NOT NULL,
    docente_id BIGINT NOT NULL,
    condicion ENUM('PRESENTE','AUSENTE') NOT NULL DEFAULT 'PRESENTE',
    nota TINYINT NULL, -- Escala entera 0..10. Si el estudiante esta ausente la nota puede ser nula.
    fecha_registro DATE NOT NULL DEFAULT (CURRENT_DATE),
    -- APPEND-ONLY, igual que cierre_cursada: la nota no se actualiza nunca. Corregir un 4 por un 7
    -- es insertar otra fila apuntando a la anterior, y la vigente es la ultima de la cadena. Asi
    -- queda el historial completo de correcciones, y el "quien" sale gratis: docente_id ya es una
    -- columna de la fila, no hay que sacarlo de un trigger (donde solo se ve el usuario de la BD).
    nota_rectificada_id BIGINT NULL,   -- la nota que esta corrige, si es una rectificacion
    -- Vale 1 en las notas originales y NULL en las rectificaciones. Como dos NULL nunca chocan en
    -- un indice unico, el UNIQUE de abajo permite muchas rectificaciones pero una sola nota
    -- original por inscripcion: sin esto la cadena podria bifurcarse.
    es_original TINYINT AS (IF(nota_rectificada_id IS NULL, 1, NULL)) VIRTUAL,
    UNIQUE (inscripcion_examen_final_id, es_original),
    UNIQUE (nota_rectificada_id),      -- una nota se rectifica una sola vez: la cadena es lineal
    FOREIGN KEY (inscripcion_examen_final_id) REFERENCES estudiante_inscripcion_examen_final (id) ON DELETE RESTRICT,
    FOREIGN KEY (nota_rectificada_id) REFERENCES nota_final (id) ON DELETE RESTRICT,
    FOREIGN KEY (docente_id) REFERENCES persona (id) ON DELETE RESTRICT,
    CONSTRAINT chk_nota_final CHECK (nota BETWEEN 0 AND 10),
    CONSTRAINT chk_condicion_nota_final CHECK (
        (condicion = 'AUSENTE'  AND nota IS NULL) OR
        (condicion = 'PRESENTE' AND nota IS NOT NULL)
    )
);

CREATE TABLE nota_evaluacion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    -- Materia de la comision a la que pertenecen TANTO la evaluacion COMO la inscripcion del estudiante. Es
    -- redundante (sale de cualquiera de las dos), pero las dos FK compuestas de abajo la comparten
    -- y por eso no puede quedar desincronizada
    comision_materia_id BIGINT NOT NULL,
    evaluacion_cursada_id BIGINT NOT NULL,
    inscripcion_comision_id BIGINT NOT NULL,
    docente_id BIGINT NOT NULL,
    condicion ENUM('PRESENTE','AUSENTE') NOT NULL DEFAULT 'PRESENTE',
    nota TINYINT NULL,   -- escala entera 0..10
    fecha_registro DATE NOT NULL DEFAULT (CURRENT_DATE),
    -- APPEND-ONLY, mismo criterio que nota_final y cierre_cursada. Ver el comentario de nota_final.
    nota_rectificada_id BIGINT NULL,
    es_original TINYINT AS (IF(nota_rectificada_id IS NULL, 1, NULL)) VIRTUAL,
    UNIQUE (evaluacion_cursada_id, inscripcion_comision_id, es_original),
    UNIQUE (nota_rectificada_id),
    FOREIGN KEY (nota_rectificada_id) REFERENCES nota_evaluacion (id) ON DELETE RESTRICT,
    -- Reemplazan a las dos FK simples contra evaluacion_cursada(id) y estudiante_inscripcion_comision(id).
    -- Apuntan a uq_eval_comision_materia_id y uq_eic_comision_materia_id, no a las PK.
    CONSTRAINT fk_nota_eval_evaluacion FOREIGN KEY (comision_materia_id, evaluacion_cursada_id)
        REFERENCES evaluacion_cursada (comision_materia_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_nota_eval_inscripcion FOREIGN KEY (comision_materia_id, inscripcion_comision_id)
        REFERENCES estudiante_inscripcion_comision (comision_materia_id, id) ON DELETE RESTRICT,
    FOREIGN KEY (docente_id) REFERENCES persona (id) ON DELETE RESTRICT,
    CONSTRAINT chk_nota_evaluacion CHECK (nota BETWEEN 0 AND 10),
    CONSTRAINT chk_condicion_nota_evaluacion CHECK (
        (condicion = 'AUSENTE'  AND nota IS NULL) OR
        (condicion = 'PRESENTE' AND nota IS NOT NULL)
    )
);

-- CIERRE DE CURSADA (regularidad) --

-- Acta de cierre de cursada: el resultado de que un estudiante haya cursado una comision. 
CREATE TABLE cierre_cursada (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inscripcion_comision_id BIGINT NOT NULL,
    docente_id BIGINT NOT NULL,         -- docente que firma el acta
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

    FOREIGN KEY (inscripcion_comision_id) REFERENCES estudiante_inscripcion_comision (id) ON DELETE RESTRICT,
    FOREIGN KEY (docente_id) REFERENCES persona (id) ON DELETE RESTRICT,
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
    comision_id BIGINT NULL,  -- solo si alcance = 'COMISION': va a todo el grupo, no a una sola materia
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

-- Representa el lanzamiento de una encuesta para una materia de una comisión determinada.
CREATE TABLE encuesta (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    comision_materia_id BIGINT NOT NULL,
    fecha_apertura DATETIME NOT NULL,
    fecha_cierre DATETIME NOT NULL,
    FOREIGN KEY (comision_materia_id) REFERENCES comision_materia (id) ON DELETE RESTRICT,
    UNIQUE (comision_materia_id),
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

-- Representa que un estudiante (anónimo) completó una encuesta. hash_estudiante evita respuestas duplicadas
-- sin permitir reconstruir la identidad del estudiante.
CREATE TABLE respuesta_encuesta (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    encuesta_id BIGINT NOT NULL,
    hash_estudiante VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    fecha_respuesta DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    comentario_final VARCHAR(1000) NULL,
    FOREIGN KEY (encuesta_id) REFERENCES encuesta (id) ON DELETE RESTRICT,
    UNIQUE (encuesta_id, hash_estudiante)
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
(1, '¿El docente cumplió con los temas del programa?'),
(2, '¿El docente explicó con claridad?'),
(3, '¿El docente estuvo disponible para consultas?'),
(4, '¿La cursada cumplió tus expectativas generales?'),
(5, '¿Recomendarías esta materia/comisión?');

-- BOLETO ESTUDIANTIL

CREATE TABLE boleto_estudiantil (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    estudiante_id BIGINT NOT NULL,
    estado ENUM('SOLICITADO','APROBADO','RECHAZADO') NOT NULL DEFAULT 'SOLICITADO',
    fecha_solicitud DATE NOT NULL DEFAULT (CURRENT_DATE),
    FOREIGN KEY (estudiante_id) REFERENCES persona (id) ON DELETE RESTRICT
);

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
     
