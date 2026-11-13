# Sección 7: Cierre, documentación y lecciones aprendidas

> Reemplaza el borrador actual. Los datos salen del dossier (Tabla Nº9, secciones 4 y 6) y del
> historial de GitHub al 10/11/2026. **Lo marcado con [completar] solo lo saben ustedes**: no lo
> inventen; si un dato no existe (por ejemplo, horas reales), escriban que no se registró, que eso
> también es un hallazgo del cierre.

---

## 7. Cierre, documentación y lecciones aprendidas

La fecha de corte de esta sección es el 10/11/2026. El código se entrega el 17/11/2026, por lo que
los estados reflejan lo logrado hasta el corte y lo que queda pendiente para esa entrega.

### 7.1 Verificación de entregables y estado final de los requisitos

*Tabla X
**Verificación de los entregables**

| Código | Entregable | Estado al 10/11/2026 | Evidencia |
|---|---|---|---|
| E1 | Autenticación JWT y control de acceso por roles | Completo | PR #1 y PR #11; RF01 a RF04 y RNF01 a RNF07 implementados |
| E2 | Módulo Administrador (API + pantallas) | Parcial | API completa. Pantallas de carreras, personas, estudiantes, docentes y comisiones integradas (PR #13 a #26). Faltan materias de la comisión, períodos y mesas de final |
| E3 | Módulo Docente (API + pantallas) | Parcial | API completa desde el 28/10/2026. Sin pantallas |
| E4 | Módulo Estudiante (API + pantallas) | Parcial | API completa desde el 28/10/2026. Pantallas en curso (SCRUM-149) |
| E5 | Integración frontend–backend | Completo para las pantallas existentes | Interceptor con renovación de sesión (PR #11) y errores ProblemDetail (PR #2) |
| E6 | Base de datos con datos de demostración | Completo | Scripts de la carpeta bdd/ (PR #14) |
| E7 | Documentación | Parcial | Dossier de gestión y ADR 0001 y 0002. README general y manual de usuario pendientes |

**Estado final de los requisitos:** la tabla final de requisitos, con su prioridad y estado real, es
la Tabla Nº9 (sección 2.9). En resumen:

| | Implementado | Parcialmente implementado (API sin pantalla) | No implementado | Total |
|---|---|---|---|---|
| Requisitos funcionales | 21 | 34 | 1 (RF05) | 56 |
| Requisitos no funcionales | 14 | 2 (RNF12, RNF13) | 0 | 16 |
| Must Have funcionales (MVP) | 10 | 12 | 0 | 22 |

**Estado de aceptación:** el backend cumple todos los requisitos del MVP y la seguridad exigida
por la consigna. El MVP no está completo de punta a punta: 12 de los 22 requisitos Must Have tienen
la API funcionando pero todavía no tienen pantalla, principalmente los de docente y estudiante.

### 7.2 Estimado vs. real y análisis de desvíos

*Tabla X
**Comparación entre lo estimado y lo real**

| Dimensión | Estimado | Real | Desvío |
|---|---|---|---|
| Esfuerzo | 1.856,9 h con PERT (11,6 personas-mes); COCOMO intermedio, 59,1 personas-mes | [completar: horas reales, o "no se registraron horas"] | [completar] |
| Velocidad | Sprint 1: 65 story points en dos semanas | 65 story points en tres semanas (con una de extensión); el 66 % se completó en la extensión | La mitad de la velocidad planificada |
| Tiempo: hito 1 (seguridad) | 14/10/2026 | 12/10/2026 (PR #1) | 2 días antes |
| Tiempo: hitos 2 a 4 (backend de los tres actores) | 14/10/2026 | 28/10/2026 | 14 días de atraso (SC-02) |
| Tiempo: hito 5 (frontend integrado) | 17/11/2026 | Al 10/11: solo login y administrador | En riesgo |
| Tiempo: hito 6 (dossier) | 11/11/2026 | [completar: fecha de entrega real] | [completar] |
| Costo | USD 15.089 esperado; línea base USD 16.422 (sección 4.12) | [completar: horas reales × USD 7,94, más infraestructura] | [completar] |
| Infraestructura | USD 35 por mes (hosting y base administrada) | USD 0: no se desplegó (RSK-03) | Ahorro, pero sin URL pública |
| Alcance | MVP: 22 requisitos Must Have de punta a punta | 10 completos y 12 con la API sin pantalla | 55 % del MVP sin pantalla |

**Desvíos significativos y sus causas:**

1. **Se rehízo el backend (RSK-01).** El equipo no estaba conforme con la versión de Programación
   III y la rehízo con una arquitectura documentada. Valió la pena en calidad, pero el frontend
   empezó recién el 29/10/2026, a tres semanas de la entrega del código.
2. **El Sprint 1 rindió la mitad de lo planificado.** No hubo commits entre el 01/10 y el 11/10 ni
   entre el 14/10 y el 22/10, semanas que coincidieron con parciales de otras materias (RSK-02).
   Además, el equipo tenía poca experiencia con Spring Boot y Angular, y eso no se reflejó en la
   capacidad comprometida.
3. **Faltan las pantallas de docente y estudiante.** Las pantallas del administrador llevaron más
   trabajo del previsto: aparecieron necesidades que la API no cubría y que se resolvieron sobre la
   marcha (cinco ajustes entre el 08/11 y el 09/11, sección 6.5).
4. **El equipo pasó de cuatro a tres integrantes** (sección 5.2.2), lo que aumentó la carga de cada
   uno sin reducir el alcance.
5. **No se desplegó el sistema (RSK-03 materializado).** Nadie del equipo había hecho un despliegue y
   se priorizó el desarrollo; la defensa se hace con la demo local.
6. **Seguimiento incompleto.** Las historias del Sprint 2 no se estimaron (D-05), los commits no
   llevan la clave de Jira (D-08) y no se registraron horas reales, por lo que el esfuerzo real no se
   puede comparar con precisión con la estimación.

### 7.3 Replanificaciones y decisiones correctivas

- **Extensión del Sprint 1 y corrimiento de hitos (SC-02):** los hitos 2, 3 y 4 pasaron del 14/10
  al 28/10/2026, sin mover las fechas de entrega. Se actualizó la línea de base temporal.
- **Reducción del alcance del MVP (RSK-07, evitado):** los ABM de carreras, planes y materias
  quedaron de solo lectura, con datos cargados por scripts SQL. Esta decisión, tomada en la
  planificación, evitó que el riesgo se materializara.
- **Priorización del frontend (contingencia de RSK-02):** el Sprint 2 se concentró en el login y el
  administrador, y en reutilizar sus componentes (RSK-05) para acelerar las pantallas siguientes.
- **Redistribución de tareas:** tras la salida de un integrante, sus paquetes de documentación y
  gestión pasaron a Federico Solá.
- **Pull request obligatorio:** después de los commits directos del 28/10 (D-03), todo el código
  entra a main por pull request revisado.
- **Demo local (contingencia de RSK-03):** la defensa se hace con el sistema local y los datos de
  prueba de la carpeta bdd/.

### 7.4 Retrospectiva del equipo

**Logros:**
- Backend completo, con seis módulos cuyas fronteras verifica un test de Spring Modulith.
- Seguridad completa: tokens JWT con renovación, control por rol y aislamiento de los datos de cada
  usuario.
- 26 pull requests integrados en un mes, con dos ADR que dejaron escritas las decisiones de
  arquitectura.
- Pantallas del administrador con componentes compartidos que sirven para los otros actores.

**Dificultades:**
- Las semanas de parciales y la falta de experiencia con Spring Boot y Angular.
- Llegar al frontend con poco tiempo, después de rehacer el backend.
- La salida de un integrante a mitad del proyecto.
- No saber desplegar la aplicación.

**Decisiones acertadas:**
- Rehacer el backend con una arquitectura documentada antes de construir pantallas.
- Sacar los ABM de carreras y planes del MVP.
- Acordar la estructura del frontend (ADR 0002) antes de empezar, lo que permitió trabajar en
  paralelo sin conflictos.
- Exigir pull request para todo el código.

**Aspectos a mejorar:**
- Registrar las horas reales de cada tarea.
- Incluir la clave de Jira en cada commit y pull request.
- Estimar todas las historias, no solo las del primer sprint.
- Empezar el frontend en paralelo con el backend.
- Hacer el primer despliegue al principio, no al final.

### 7.5 Lecciones aprendidas y recomendaciones

1. **Planificar con la capacidad real del equipo.** Comprometer 65 story points sin descontar las
   semanas de parciales llevó a un sprint que duró el doble. Recomendación: calcular la capacidad de
   cada sprint con el calendario de exámenes a la vista.
2. **Medir para poder comparar.** Sin horas registradas no se puede saber qué técnica de estimación
   acertó. Recomendación: registrar el tiempo en Jira desde la primera tarea.
3. **Integrar de punta a punta lo antes posible.** El primer corte completo (login, el 02/11) llegó
   tarde, y los ajustes de la API aparecieron recién al construir las pantallas. Recomendación:
   cerrar cada funcionalidad con su pantalla antes de pasar a la siguiente.
4. **Escribir las decisiones.** Los ADR evitaron discusiones repetidas y conflictos al integrar.
   Recomendación: documentar cada decisión que afecte a más de un integrante.
5. **Desplegar temprano.** Un despliegue al final es un riesgo que se puede evitar con un despliegue
   mínimo en la primera semana.
6. **Vincular Jira y GitHub desde el inicio**, para que la trazabilidad no tenga que reconstruirse
   al final.

### 7.6 Documentación final, pendientes y líneas de evolución

**Documentación para comprender, usar o continuar el proyecto:**

| Documento | Dónde está | Estado |
|---|---|---|
| Dossier de gestión v1.0 | Este documento | Completo |
| Decisiones de arquitectura (ADR 0001 y 0002) | docs/adr en GitHub | Completo |
| Esquema y datos de demostración | Carpeta bdd/ en GitHub | Completo |
| Documentación de la API (OpenAPI) | Generada por springdoc al levantar el backend | Completo |
| README con instalación y ejecución | Raíz del repositorio | [completar: pendiente o fecha en que se agregó] |
| Manual de usuario por rol | [completar] | [completar: pendiente o fecha en que se agregó] |

**Pendientes dentro del alcance aprobado (v1):**
- Pantallas del docente (RF36 a RF45) y del estudiante (RF46 a RF56).
- Pantallas del administrador para materias de la comisión, períodos y mesas de final (RF20, RF24 a
  RF35).
- Cambio de contraseña (RF05).
- Diseño responsive y accesibilidad completos (RNF12 y RNF13).
- README general, manual de usuario y despliegue.

**Líneas de evolución (fuera del alcance de la v1):**
- ABM de carreras, planes de estudio, materias y correlatividades.
- Nuevos módulos previstos en el ADR 0001: avisos, cuotas, encuestas y trámites.
- Notificaciones por correo.
- Integración continua y despliegue automático.

Para continuar el proyecto en el TFI, el punto de partida es la Tabla Nº9: los 34 requisitos
parcialmente implementados ya tienen la API funcionando y solo necesitan sus pantallas.

### 7.7 Versión final del dossier

Esta es la versión 1.0 del dossier (TPFinal_Gestion_Atenea_v1.0.pdf), que consolida las versiones
anteriores del documento [completar: por ejemplo, v0.1 en el Checkpoint 1 y v0.2 en el Checkpoint 2]
y la línea de base del alcance v1.2 (sección 2.13).

**Conclusión:** ATENEA v1 cierra con el backend completo y seguro, las pantallas del administrador
integradas y una documentación de gestión que registra cada decisión y cada desvío. El MVP no se
completó de punta a punta: faltan las pantallas de docente y estudiante, que quedan como el primer
paso para el TFI. El principal aprendizaje del equipo es que la gestión depende tanto de planificar
como de medir lo que realmente pasa.

---

## Referencias (formato APA 7)

> Sangría francesa en cada referencia. Las de la cátedra solo pueden quedar si se citan en el texto,
> por ejemplo "(Cátedra de Gestión de Desarrollo de Software, 2026)" donde usaron COCOMO o PERT.
> El Risk Register ya está en el documento como tabla: no hace falta como referencia.

Cátedra de Gestión de Desarrollo de Software. (2026). *Unidad 1: Introducción a la gestión de
proyectos* [Apunte de cátedra]. Universidad Tecnológica Nacional, Facultad Regional Mar del Plata.

Cátedra de Gestión de Desarrollo de Software. (2026). *Unidad 2: Definición del producto,
interesados y alcance* [Apunte de cátedra]. Universidad Tecnológica Nacional, Facultad Regional Mar
del Plata.

Cátedra de Gestión de Desarrollo de Software. (2026). *Unidad 3: Arquitectura y descomposición del
trabajo* [Apunte de cátedra]. Universidad Tecnológica Nacional, Facultad Regional Mar del Plata.

Cátedra de Gestión de Desarrollo de Software. (2026). *Unidad 4: Medición, estimación y
planificación integral* [Apunte de cátedra]. Universidad Tecnológica Nacional, Facultad Regional Mar
del Plata.

Cátedra de Gestión de Desarrollo de Software. (2026). *Unidad 5: Gestión de riesgos* [Apunte de
cátedra]. Universidad Tecnológica Nacional, Facultad Regional Mar del Plata.

Cátedra de Gestión de Desarrollo de Software. (2026). *Unidad 6: Ejecución, seguimiento y control*
[Apunte de cátedra]. Universidad Tecnológica Nacional, Facultad Regional Mar del Plata.

Cátedra de Gestión de Desarrollo de Software. (2026). *Unidad 7: Cierre de proyectos* [Apunte de
cátedra]. Universidad Tecnológica Nacional, Facultad Regional Mar del Plata.

Monti, E., Silva, J. y Solá, F. (2026). *Sistema_Gestion_Academica_Atenea* [Repositorio de código].
GitHub. https://github.com/eliseomonti/Sistema_Gestion_Academica_Atenea

Monti, E., Silva, J. y Solá, F. (2026). *Proyecto SCRUM: ATENEA* [Tablero de Jira]. Atlassian.
[poner aquí el enlace de Jira como hipervínculo, con el texto "Tablero de Jira del proyecto"]
