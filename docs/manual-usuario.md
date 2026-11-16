# Manual de Usuario — ATENEA (Gestor Universitario)

Este manual describe la operatoria del sistema **ATENEA** para cada uno de sus perfiles de usuario: **Administrador**, **Docente** y **Estudiante**.

---

## 1. Ingreso al sistema y gestión de sesión

### 1.1 Inicio de sesión (`RF01`)
1. Acceder a la pantalla de ingreso (`/login`). La dirección depende de dónde esté corriendo el sistema:
   - **Versión publicada:** https://atenea-frontend.calmcliff-ac059761.chilecentral.azurecontainerapps.io/login
   - **En una PC con Docker:** `http://localhost:8081/login`.
   - **En una PC sin Docker (desarrollo con `ng serve`):** `http://localhost:4200/login`.
2. Ingresar el **Usuario** (DNI de la persona) y la **Contraseña** (en los datos de demostración, `Prueba123`).
3. Presionar **Ingresar**.
   - Si las credenciales son incorrectas o la cuenta se encuentra inactiva/bloqueada, el sistema informa el motivo en pantalla sin iniciar sesión.
   - Si el usuario posee **un único rol**, ingresa directamente al panel de ese perfil (`/admin`, `/docente` o `/estudiante`).

### 1.2 Selección y cambio de rol activo (`RF04`)
- Cuando una misma persona posee más de un rol asignado (por ejemplo, es Docente y Estudiante, o Administrador y Docente), luego de autenticarse o desde la barra lateral puede elegir con qué perfil desea operar.
- Al cambiar de rol activo, el menú lateral y las rutas habilitadas se adaptan automáticamente sin necesidad de volver a ingresar usuario y contraseña.

### 1.3 Renovación automática y cierre de sesión (`RF02`, `RF03`)
- La sesión se renueva de forma transparente mediante el *refresh token* mientras el usuario permanece activo.
- Para salir del sistema de forma segura, presionar **Cerrar sesión** en el panel lateral; esto revoca el token de renovación en el servidor y redirige a la pantalla de ingreso.

---

## 2. Perfil Administrador (`/admin`)

El perfil Administrador centraliza la consulta de la estructura académica, la administración del padrón único de personas, las inscripciones a planes de estudio, la organización de comisiones y la apertura de períodos y mesas de examen.

### 2.1 Carreras y planes de estudio (`RF06`, `RF07`, `RF08`)
1. En el menú lateral, seleccionar **Carreras**.
2. Se listan las carreras registradas en la institución junto con la cantidad de planes de estudio vigentes.
3. Al seleccionar una carrera y elegir uno de sus **Planes de estudio**, se despliegan las materias organizadas por año/cuatrimestre junto con sus **correlatividades** (para cursar y para rendir examen final) y carga horaria.
4. Desde la vista de un plan de estudio también es posible iniciar directamente la **inscripción de un estudiante a dicho plan**.

### 2.2 Estudiantes y Docentes — Padrón único de personas (`RF09` a `RF15`)
ATENEA mantiene un **único registro de persona por DNI** (`RN01`), evitando duplicar datos personales cuando alguien cumple más de una función en la universidad:
1. **Listado y búsqueda (`RF09`, `RF11`)**:
   - Desde **Estudiantes** o **Docentes**, se visualiza el padrón paginado.
   - Se puede filtrar por texto libre (nombre, apellido, DNI o legajo) y por estado (**Activos** / **Inactivos**), además de filtrar estudiantes por carrera.
2. **Alta de Estudiante o Docente (`RF10`, `RF12`, `RF13`)**:
   - Presionar **Nuevo estudiante** o **Nuevo docente**.
   - Ingresar primero el **DNI** para verificar si la persona ya existe en el padrón:
     - **Si el DNI no existe**: completar nombre, apellido, fecha de nacimiento (entre 16 y 100 años), teléfono y correo electrónico (y plan de estudio inicial en el caso de estudiantes). El sistema crea la persona, genera su legajo, asigna el rol correspondiente y crea su usuario con clave inicial igual a su DNI.
     - **Si el DNI ya existe con otro rol**: el sistema muestra sus datos personales actuales y ofrece **asignarle el rol faltante** generando su nuevo legajo sin duplicar la persona ni su cuenta de usuario.
3. **Edición y baja lógica (`RF14`, `RF15`)**:
   - Desde la fila o el detalle de la persona se pueden actualizar sus datos de contacto (nombre, apellido, fecha de nacimiento, teléfono y email).
   - La **baja de una persona** es lógica: conserva todo su historial académico (cursadas, actas y finales) y deshabilita su acceso si no conserva otros roles activos.

### 2.3 Inscripciones a planes de estudio (`RF16`, `RF17`, `RF18`)
1. Desde el detalle de un estudiante o desde la pantalla de un plan de estudio, seleccionar **Inscribir a carrera / plan**.
2. El sistema verifica que la persona tenga rol de estudiante activo y que no posea ya una inscripción activa en la misma carrera.
3. En la sección de inscripciones del estudiante se listan sus planes de estudio (`ACTIVA`, `BAJA`, `EGRESADO`), permitiendo registrar la **baja de la inscripción** (`RF18`) cuando corresponda.

### 2.4 Comisiones (`RF19` a `RF26`)
1. En el menú lateral, ingresar a **Comisiones**.
2. **Listado y filtros (`RF19`)**: permite filtrar comisiones por carrera, plan de estudio, año lectivo, turno (`MAÑANA`, `TARDE`, `NOCHE`) y estado (`ABIERTA`, `CERRADA`, `BAJA`).
3. **Crear y editar comisión (`RF21`, `RF22`, `RF23`)**:
   - Definir nombre de la comisión, plan de estudio, nivel/año, año lectivo y turno.
   - Una comisión sin alumnos inscriptos en sus materias puede eliminarse físicamente; si ya registra actividad o inscriptos, se aplica la **baja lógica** (`RF23`).
4. **Materias, cupos, docentes y horarios de una comisión (`RF20`, `RF24`, `RF25`, `RF26`)**:
   - Dentro del detalle de la comisión se agregan las materias del plan de estudio indicando su **cupo máximo** de alumnos.
   - A cada materia-comisión se le asignan los **docentes** indicando su cargo (`TITULAR` o `AYUDANTE`), respetando la regla de un único titular activo por materia-comisión (`RN03`).
   - Se configuran los bloques de **día de la semana, horario de inicio/fin y aula**, validando que no existan solapamientos horarios.

### 2.5 Períodos de inscripción y mesas de examen final (`RF27` a `RF35`)
1. **Períodos y turnos (`RF27` a `RF31`)**:
   - El administrador crea los períodos de inscripción a cursadas (`PRIMER_CUATRIMESTRE`, `SEGUNDO_CUATRIMESTRE`, `ANUAL`) y los turnos de exámenes finales definiendo sus fechas de apertura y cierre.
   - Puede habilitar o deshabilitar manualmente un período (`RF30`) o eliminarlo si aún no registra inscripciones ni mesas asociadas (`RF31`).
2. **Mesas de examen final (`RF32` a `RF35`)**:
   - Dentro de un turno de finales, el administrador crea las mesas indicando materia, fecha, horario, aula y la conformación del **tribunal examinador** (docentes con rol `PRESIDENTE` o `VOCAL`).
   - Permite reprogramar fecha, hora, aula o tribunal (`RF34`), o dar de baja una mesa sin alumnos inscriptos (`RF35`).

---

## 3. Perfil Docente (`/docente`)

El perfil Docente permite gestionar el dictado de las materias asignadas, registrar evaluaciones parciales y recuperatorios, cerrar las actas de cursada y calificar mesas de examen final.

### 3.1 Mis materias y nómina de inscriptos (`RF36`, `RF37`)
1. Al ingresar con rol **Docente**, se listan las materias-comisiones en las que el docente está asignado como titular o ayudante en el ciclo lectivo.
2. Al seleccionar una materia-comisión, se visualiza la **nómina de estudiantes inscriptos** junto con su legajo, DNI y condición actual.

### 3.2 Evaluaciones y carga de notas de cursada (`RF38` a `RF42`)
1. **Gestión de evaluaciones (`RF38`, `RF40`, `RF41`)**:
   - Mientras el acta de cursada se encuentre abierta (`RN02`), el docente puede crear instancias de evaluación (`PARCIAL`, `RECUPERATORIO`, `TRABAJO_PRACTICO`) indicando descripción y fecha.
   - Si una evaluación aún no tiene notas cargadas, puede eliminarse (`RF41`).
2. **Carga y corrección de calificaciones (`RF42`)**:
   - Para cada evaluación, el docente registra la nota numérica (1 a 10) o condición de ausente de cada alumno inscripto, pudiendo actualizarla mientras el acta de cursada no haya sido cerrada (`RN05`).

### 3.3 Cierre definitivo de acta de cursada (`RF43`)
1. Al finalizar el cuatrimestre, el docente titular ejecuta el **Cierre de acta de cursada**.
2. Se asigna a cada estudiante su condición final de cursada (`PROMOCIONADO`, `REGULAR`, `NO_REGULAR` o `ABANDONO`) y nota final de cursada.
3. Al confirmar el cierre:
   - El acta pasa a estado **CERRADA** de manera **inmutable** (`RN02`, `RN05`), bloqueando nuevas evaluaciones o ediciones directas de notas (cualquier cambio posterior requiere un registro formal de rectificación).
   - Para los estudiantes que obtienen condición `REGULAR`, el sistema calcula y almacena automáticamente la **fecha de vencimiento de la regularidad** a 2 años desde la fecha de cierre (`RN04`, configurable mediante `atenea.regularidad.vigencia`).

### 3.4 Mesas de examen final (`RF44`, `RF45`)
1. En la sección **Mesas de final**, el docente consulta las mesas en las que integra el tribunal examinador (`PRESIDENTE` o `VOCAL`).
2. Al abrir una mesa, visualiza los estudiantes inscriptos y registra la **nota del examen final** (o ausente). El sistema determina automáticamente la aprobación cuando la calificación es mayor o igual a 6 (`RN06`).

---

## 4. Perfil Estudiante (`/estudiante`)

El perfil Estudiante permite autogestionar las inscripciones a cursadas y exámenes finales y consultar en tiempo real el avance en el plan de estudio.

### 4.1 Inscripción y baja a cursadas (`RF46` a `RF49`)
1. **Oferta de materias disponibles (`RF46`)**: durante un período de inscripción a cursadas habilitado, el estudiante consulta las materias de su plan de estudio para las cuales **cumple las correlatividades de cursada** y que aún no tiene aprobadas ni regularizadas vigentes.
2. **Inscripción a comisión (`RF47`)**: elige una comisión con cupo disponible y confirma su inscripción. El sistema valida automáticamente:
   - Período de inscripción abierto.
   - Cumplimiento de todas las materias correlativas requeridas para cursar.
   - Existencia de cupo disponible en la materia-comisión.
   - Ausencia de superposición horaria o inscripción duplicada a la misma materia.
3. **Mis inscripciones y baja (`RF48`, `RF49`)**: mientras el período de inscripción permanece abierto, el estudiante puede consultar sus comisiones activas y darse de baja liberando el cupo.

### 4.2 Inscripción y baja a mesas de examen final (`RF50` a `RF53`)
1. **Mesas disponibles (`RF50`)**: durante un turno de finales habilitado, el sistema muestra las mesas de las materias en las que el estudiante posee **regularidad vigente** (no vencida) y cumple las **correlatividades para rendir final**.
2. **Inscripción y baja (`RF51`, `RF52`, `RF53`)**: el estudiante se inscribe a la mesa elegida y puede darse de baja hasta el día anterior a la fecha del examen (`RF53`).

### 4.3 Seguimiento del plan de estudio e historial académico (`RF54`, `RF55`, `RF56`)
1. **Avance del plan de estudio (`RF54`)**: muestra todas las materias del plan con su estado actual (`APROBADA`, `REGULAR`, `CURSANDO`, `PENDIENTE`) y calcula el **porcentaje de avance de la carrera**.
2. **Historial de cursadas (`RF55`)**: detalla todas las cursadas realizadas, las notas obtenidas en cada parcial/recuperatorio y la condición final del acta con su fecha de vencimiento de regularidad.
3. **Historial de finales (`RF56`)**: lista todas las mesas de examen final rendidas con su fecha, calificación y resultado.
