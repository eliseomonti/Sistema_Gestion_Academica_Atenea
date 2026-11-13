# Sección 3: lo que falta

## 3.2 Diccionario de la EDT: columnas Resp. y Esf.(h)

- **Resp.:** salen del historial de GitHub (quién hizo los commits de cada módulo). Los paquetes de
  documentación no figuran en git: confirmen entre ustedes quién los hizo.
- **Esf.(h):** son horas **estimadas** (planificadas). El total da **322 h**. Tiene que coincidir con el
  total de la sección 4 (PERT/COCOMO): si allá les da otro número, ajusten proporcionalmente.

| Cód. | Paquete | Resp. | Esf.(h) |
|---|---|---|---|
| 1.1.1 | Iniciación y requisitos | Solá | 12 |
| 1.1.2 | Arquitectura y decisiones | Monti | 16 |
| 1.1.3 | Estimación y planificación | Solá | 14 |
| 1.1.4 | Riesgos, seguimiento y cambios | Solá | 12 |
| 1.1.5 | Cierre y presentación | Todos | 10 |
| 1.2.1 | Autenticación JWT | Monti | 14 |
| 1.2.2 | Control de acceso por roles | Monti | 8 |
| 1.3.1 | Estructura académica (consulta) | Monti | 10 |
| 1.3.2 | Personas y roles | Silva / Monti | 24 |
| 1.3.3 | Comisiones | Monti / Silva | 24 |
| 1.3.4 | Períodos y mesas de final | Solá / Silva | 20 |
| 1.4.1 | Materias y alumnos | Silva | 10 |
| 1.4.2 | Evaluaciones y notas | Silva | 20 |
| 1.4.3 | Cierre de actas | Silva | 12 |
| 1.4.4 | Mesas de final | Silva | 12 |
| 1.5.1 | Inscripción a cursadas | Solá | 22 |
| 1.5.2 | Finales | Silva | 18 |
| 1.5.3 | Seguimiento académico | Silva | 12 |
| 1.6.1 | Interceptor y sesión | Monti | 8 |
| 1.6.2 | Errores y pruebas de integración | Monti / Solá | 14 |
| 1.7.1 | Modelo y esquema SQL | Silva / Monti | 12 |
| 1.7.2 | Datos de demostración | Silva | 6 |
| 1.8.1 | README y guía | Solá | 4 |
| 1.8.2 | Manual de usuario | Solá | 8 |
| | **Total** | | **322** |

Control de horas por integrante (no va en el documento, es para que revisen el reparto):

| Integrante | Horas estimadas |
|---|---|
| Monti | 16 + 14 + 8 + 10 + 12 (½ de 1.3.2) + 12 (½ de 1.3.3) + 8 + 7 (½ de 1.6.2) + 6 (½ de 1.7.1) = **93** |
| Silva | 12 + 12 + 10 + 10 + 20 + 12 + 12 + 18 + 12 + 6 + 6 = **130** |
| Solá | 12 + 14 + 12 + 10 (½ de 1.3.4) + 22 + 7 (½ de 1.6.2) + 4 + 8 = **89** |
| Todos | 10 (1.1.5) |

> Los paquetes de documentación (1.1.1, 1.1.3, 1.1.4, 1.8.1 y 1.8.2) se los asigné a Solá para
> equilibrar la carga. Si los hizo otro, cámbienlos.

## 3.3 Descomposición complementaria: Story Mapping

> Va **después del Cuadro Nº13** (diccionario de la EDT) y **antes del Registro de decisiones**, que
> pasa a ser la **3.4**.

La EDT descompone el proyecto por entregables: muestra qué se construye y en qué paquetes, pero no
cómo usa el sistema cada actor ni en qué orden se entrega valor. Por eso se complementa con un Story
Mapping, que ordena el trabajo según el recorrido de cada usuario. Cada fila es una épica (una
actividad del actor) y sus requisitos se reparten en tres entregas: la **Entrega 1** es el MVP
(sección 2.12, requisitos Must Have), la **Entrega 2** suma los Should y Could Have de la Tabla Nº8,
y la **Entrega 3** reúne lo que queda fuera de la primera versión y se plantea como un proyecto nuevo
(sección 1.2). Así se ve qué porción del sistema es usable al terminar cada entrega, algo que la EDT no
muestra.

*Cuadro Nº14
**Story Mapping de ATENEA**

| Actor | Épica | Entrega 1 (MVP) | Entrega 2 | Entrega 3 (futuro) |
|---|---|---|---|---|
| Todos | Acceder al sistema | Iniciar sesión y mantenerla (RF01, RF02) | Cerrar sesión, elegir rol y cambiar contraseña (RF03 a RF05) | — |
| Administrador | Consultar la estructura académica | Carreras, planes y materias (RF06 a RF08) | — | ABM de carreras, planes y materias |
| Administrador | Gestionar personas | Alta de estudiantes y docentes; inscripción a un plan (RF10, RF12, RF16) | Listados, asignar rol, modificar, dar de baja e inscripciones a carreras (RF09, RF11, RF13 a RF15, RF17, RF18) | — |
| Administrador | Armar comisiones | Listar, crear y agregar materias con docentes y horarios (RF19, RF21, RF24) | Detalle, modificación, baja y quitar materias (RF20, RF22, RF23, RF25, RF26) | — |
| Administrador | Gestionar períodos y mesas | Crear y habilitar períodos (RF28, RF30) | Listar, modificar y borrar períodos; mesas de final (RF27, RF29, RF31 a RF35) | — |
| Docente | Consultar sus materias | Materias asignadas y alumnos (RF36, RF37) | — | — |
| Docente | Evaluar y cerrar cursadas | Crear evaluaciones, cargar notas y cerrar actas (RF38, RF42, RF43) | Ver, modificar y borrar evaluaciones (RF39 a RF41) | — |
| Docente | Tomar finales | — | Mesas del tribunal y notas de final (RF44, RF45) | — |
| Estudiante | Inscribirse a cursadas | Oferta, inscripción y baja (RF46, RF47, RF49) | Ver sus inscripciones (RF48) | — |
| Estudiante | Inscribirse a finales | — | Mesas disponibles, inscripción, consulta y baja (RF50 a RF53) | — |
| Estudiante | Seguir su avance | Estado del plan y porcentaje de avance (RF54) | Historial de cursadas y finales rendidos (RF55, RF56) | Avisos, cuotas y encuestas |
