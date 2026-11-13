# Auditoría TP Final - Borrador de las secciones 3 y 4

> Borrador para copiar al documento del TP. Los datos salen del código del repositorio
> (`backend/pom.xml`, `frontend/package.json`, `application.properties`, `docs/adr/0001-monolito-modular.md`).
> Este archivo no forma parte del sistema: se puede borrar después de pasarlo al documento.

---

## 3. Detalle de arquitectura

### Tecnologías

| Parte | Tecnología |
|---|---|
| Lenguaje backend | Java 21 |
| Framework | Spring Boot 4.1.1 (Spring Web MVC) |
| Build | Maven |
| Persistencia | Spring Data JPA / Hibernate, base de datos MySQL. El esquema se crea con los scripts de la carpeta `bdd/` y Hibernate solo lo valida (`ddl-auto=validate`). |
| Seguridad | Spring Security como OAuth2 Resource Server con JWT. Access token de 15 minutos y refresh token de 7 días. Autorización por rol (ADMIN, DOCENTE, ESTUDIANTE) por prefijo de URL (`/api/admin/**`, `/api/docente/**`, `/api/estudiante/**`). |
| Validación | Bean Validation (`@Valid` en los requests) |
| Mapeo | MapStruct (entidad ↔ DTO) y Lombok para reducir código repetido |
| Documentación de la API | springdoc-openapi (Swagger UI) |
| Modularidad | Spring Modulith: un test verifica que los módulos respeten sus fronteras |
| Frontend | Angular 22, TypeScript 6 y RxJS 7.8 |
| Repositorio | GitHub: https://github.com/eliseomonti/Sistema_Gestion_Academica_Atenea |

### Arquitectura web
Atenea es una **API REST** consumida por una **SPA (Single Page Application)** en Angular. El frontend envía
el JWT en cada petición y el backend responde con DTOs en JSON. Los orígenes permitidos se configuran por
CORS (`http://localhost:4200` en desarrollo).

### Arquitectura del backend: monolito modular por dominio
Según el ADR 0001 del proyecto, el backend es **una sola aplicación** dividida en **módulos por tema de
negocio** (package-by-feature):

| Nivel | Módulo | Responsabilidad |
|---|---|---|
| 0 | `seguridad` | usuarios, roles, login, tokens JWT |
| 0 | `carreras` | carreras, planes de estudio, materias y correlatividades |
| 1 | `personas` | personas, estudiantes, docentes e inscripción a carreras |
| 2 | `comisiones` | comisiones, aulas, horarios y docentes asignados |
| 3 | `examenes` | evaluaciones, notas, cierre de cursada y mesas de examen final |
| 4 | `inscripciones` | períodos de inscripción, inscripción a cursadas y a finales, situación académica |
| — | `compartido` | excepciones y manejo global de errores |

Reglas de la arquitectura:
- Un módulo solo puede depender de módulos de **nivel menor**.
- Cada módulo expone una **interfaz pública** (`CarrerasApi`, `PersonasApi`, `ComisionesApi`, etc.). Todo
  lo que está en sus subpaquetes es interno.
- Entre módulos se referencia **por id** (`Long materiaId`), no con relaciones JPA a entidades de otro
  módulo.
- El ciclo `examenes` ↔ `inscripciones` se corta con un puerto (`Inscriptos`) declarado en `examenes` e
  implementado en `inscripciones` (inversión de dependencias).

### Capas dentro de cada módulo

| Capa | Qué hace |
|---|---|
| `model` (Entity) | Clases que representan las tablas (Persona, Materia, InscripcionComision...) |
| `repository` | Acceso a la base de datos con Spring Data JPA |
| `mapper` | Convierte entidades en DTOs (MapStruct) |
| `dto` | Objetos de entrada (`...Request`) y salida (`...Response`) de la API |
| `service` | Lógica de negocio: validar correlatividades, cupos, períodos, cargar notas |
| `controller` | Recibe las peticiones HTTP, toma el usuario del token y delega en el service (Controlador de GRASP) |

### Frontend
- `core`: interceptores HTTP, layout y seguridad (guardas de rutas y manejo del token).
- `shared`: modelos, pipes y componentes de interfaz reutilizables.
- Una carpeta por rol: `admin`, `docente`, `estudiante`, además de `login`.

---

## 4. Estado actual del código

El backend ya aplica varias buenas prácticas: módulos con fronteras verificadas, controllers sin lógica de
negocio, DTOs y reglas académicas concentradas en una clase de Java puro (`SituacionAcademica`). Las
propuestas siguientes son **mejoras sobre esa base**, no correcciones de errores.

### Propuesta 1: Cadena de validaciones de la inscripción a cursada (Chain of Responsibility)

- **Módulo / paquete:** `inscripciones.service` → `AltaInscripcionComisionService.inscribir()`
- **Descripción del dominio:** para inscribirse a una materia de una comisión, el estudiante tiene que
  cumplir varias reglas en orden: comisión activa, inscripción activa al plan, período abierto, no estar ya
  inscripto, no estar en otra comisión de la misma materia, cumplir correlativas y que haya cupo. Hoy cada
  regla es un método privado llamado en secuencia dentro del service.
- **Solución a implementar:** patrón **Chain of Responsibility** (de comportamiento). Se crea la interfaz
  `ReglaInscripcion` con el método `validar(ContextoInscripcion)` y una clase por regla:
  `ComisionActivaRegla`, `PeriodoAbiertoRegla`, `NoDuplicadaRegla`, `CorrelativasRegla`, `CupoRegla`.
  El service recorre la cadena y cada regla corta el proceso con su propio mensaje si no se cumple.
- **Ventajas:** cumple OCP: una regla nueva (por ejemplo, "no tener deuda", del módulo futuro `cuotas`)
  se agrega como clase nueva sin modificar el service. Cada regla queda con una sola responsabilidad y se
  puede probar por separado.
- **Posibles desventajas:** quedan más clases, una por cada validación. Además, hay que tener cuidado con
  el orden en que se ejecutan: por ejemplo, el cupo se tiene que revisar al final, justo antes de guardar
  la inscripción.

### Propuesta 2: Flujo común de inscripción a cursada y a final (Template Method)

- **Módulo / paquete:** `inscripciones.service` → `AltaInscripcionComisionService` y
  `AltaInscripcionFinalService`
- **Descripción del dominio:** inscribirse a una cursada y a una mesa de final siguen **los mismos pasos**:
  buscar el destino, verificar que el período esté abierto, encontrar la inscripción a la carrera, revisar
  si ya existe una inscripción (y reactivarla si estaba dada de baja), validar la situación académica,
  guardar y armar la respuesta. Hoy ese esqueleto está **duplicado** en las dos clases.
- **Solución a implementar:** patrón **Template Method** (de comportamiento). Una clase abstracta
  `AltaInscripcion` define el método `inscribir()` con los pasos fijos, y las subclases implementan solo
  los que cambian: `buscarDestino()`, `validarSituacionAcademica()` (cursar vs. rendir) y `validarCupo()`
  (solo la cursada tiene cupo).
- **Ventajas:** elimina código duplicado, el flujo de inscripción queda en un solo lugar y un cambio
  general (por ejemplo, registrar cada inscripción) se hace una vez.
- **Posibles desventajas:** usa herencia, que acopla las subclases a la clase base. Las dos inscripciones
  tienen diferencias reales (bloqueo de la comisión, firma distinta de `reinscribir()`), y forzarlas en un
  mismo molde puede volver el código más difícil de leer. Es viable, pero conviene evaluarlo junto con la
  propuesta 1, que resuelve parte del mismo problema.

### Propuesta 3: Roles de la persona modelados con columnas nulas

- **Módulo / paquete:** `personas.model` → `Persona`
- **Descripción del dominio:** una misma persona puede ser estudiante, docente y administrador a la vez.
  Hoy la entidad tiene tres columnas de legajo (`legajoEstudiante`, `legajoDocente`, `legajoAdmin`) que
  quedan en `null` cuando la persona no tiene ese rol, y los métodos `esEstudiante()`, `esDocente()` y
  `esAdmin()` preguntan si el campo es nulo.
- **Solución a implementar:** extraer el rol académico a una entidad propia `LegajoPersona` (tipo de rol
  y número de legajo), en una relación uno a muchos con `Persona`. Se elige composición en lugar de
  herencia (`Estudiante extends Persona`) porque una persona puede tener varios roles al mismo tiempo.
- **Ventajas:** cumple OCP: un rol nuevo (por ejemplo, personal no docente o bedel) no requiere agregar
  columnas ni métodos a `Persona`. Se eliminan los campos nulos y `Persona` queda solo con datos personales
  (alta cohesión).
- **Posibles desventajas:** requiere **migrar la base de datos** y cambiar las consultas y restricciones
  `UNIQUE` de los legajos. Con tres roles fijos el diseño actual es simple y funciona. El cambio solo se
  justifica si se prevén roles nuevos.

### Propuesta 4: Estado de la materia con comportamiento propio (State)

- **Módulo / paquete:** `inscripciones.reglas` → `SituacionAcademica` y `EstadoMateria`, y los services
  `AltaInscripcionComisionService` / `AltaInscripcionFinalService`
- **Descripción del dominio:** cada materia de un estudiante está en un estado: no cursada, cursando,
  regular, regularidad vencida, no regular o aprobada. Lo que se puede hacer depende del estado: solo se
  rinde con la materia regular y vigente, y no se cursa si ya está regular o aprobada. Hoy esas reglas están
  en `switch` e `if` repartidos entre `SituacionAcademica` (`cumple()`, `puedeCursar()`, `estadoDe()`) y
  los services, que vuelven a preguntar `estado == APROBADA || estado == REGULAR` para armar el mensaje.
- **Solución a implementar:** patrón **State** (de comportamiento). Cada estado sabe responder
  `puedeCursar()`, `puedeRendir()`, `cumpleCorrelativa(EstadoCorrelativa)` y `motivoDeRechazo()`. En Java
  se puede implementar con un enum con un método por constante, o con una interfaz `EstadoMateria` y una
  clase por estado. `SituacionAcademica` delega en el estado actual de la materia.
- **Ventajas:** desaparecen los `switch` dispersos, cada estado concentra sus reglas y un estado nuevo
  (por ejemplo, "equivalencia otorgada") se agrega sin modificar los services.
- **Posibles desventajas:** algunas reglas dependen de datos externos al estado (la fecha de vencimiento
  de la regularidad, la fecha de la mesa), así que hay que pasarlos como parámetro. Hoy las reglas ya
  están concentradas y probadas en una sola clase, por lo que la mejora es moderada.

### Propuesta 5: Avisos desacoplados con eventos (Observer)

- **Módulo / paquete:** `examenes.service` → `CierreCursadaService`, `NotasExamenFinalService`, y el
  módulo futuro `avisos`
- **Descripción del dominio:** cuando un docente cierra el acta de una cursada o carga la nota de un final,
  el estudiante debería enterarse. El ADR 0001 prevé que, si un módulo solo avisa que "pasó algo", lo haga
  con un evento, pero **hoy no se publica ningún evento** en el código.
- **Solución a implementar:** patrón **Observer** (de comportamiento), usando los eventos de Spring.
  `examenes` publica `CursadaCerrada` y `NotaFinalCargada` con `ApplicationEventPublisher`, y el módulo
  `avisos` los escucha con `@ApplicationModuleListener` para notificar al estudiante (correo o aviso en la
  aplicación).
- **Ventajas:** bajo acoplamiento: `examenes` no conoce a quién le interesa el cambio. Respeta la regla
  de niveles del ADR: el módulo de abajo publica y el de arriba escucha. Agregar un interesado nuevo no
  modifica `examenes`.
- **Posibles desventajas:** es una funcionalidad nueva, no solo un refactor. Los eventos son más difíciles
  de seguir al depurar, y hay que decidir si el aviso se envía dentro o después de la transacción, para no
  avisar notas que finalmente no se guardaron.

### Propuesta 6: `ExamenFinalService` con demasiadas responsabilidades (SRP)

- **Módulo / paquete:** `examenes.service` → `ExamenFinalService`
- **Descripción del dominio:** el service del ABM de mesas de examen hace tres trabajos: el ABM en sí,
  las validaciones de la mesa (horario posterior al cierre del turno, aula libre y activa, docentes sin
  repetir y activos) y el armado de las respuestas, que junta nombres de materias, aulas y docentes de tres
  módulos. Por eso depende de seis componentes.
- **Solución a implementar:** aplicar **SRP / Alta cohesión** con dos clases de **Fabricación Pura**:
  `ValidadorMesaExamen` (con `validarHorario()`, `validarAula()` y `validarDocentes()`) y
  `ExamenFinalResponseAssembler` (con `armarRespuestas()`). `ExamenFinalService` queda solo con alta,
  modificación, baja y listado, y delega en ellas.
- **Ventajas:** si cambia una regla de la mesa, solo se modifica el validador. Si cambia lo que se muestra,
  solo se modifica el assembler. Cada clase se puede probar por separado.
- **Posibles desventajas:** más clases para un archivo de unas 200 líneas que hoy se lee bien. El impacto
  es bajo: es un refactor interno del módulo y no cambia la API. Además, las dependencias no se
  eliminan, solo se reparten entre las clases nuevas, porque el validador y el assembler siguen
  necesitando consultar aulas, docentes y materias.

### Propuesta 7: Dependencia directa del reloj del sistema (DIP)

- **Módulo / paquete:** varios services (`inscripciones`, `examenes`, `comisiones`, `personas`,
  `seguridad`)
- **Descripción del dominio:** muchas reglas dependen de la fecha: si un período de inscripción está
  abierto, si la regularidad venció o la edad mínima de una persona. Hoy el código llama directamente a
  `LocalDate.now()` y `LocalDateTime.now()` en **31 lugares**.
- **Solución a implementar:** aplicar **Inversión de dependencias (DIP)**: se registra un bean
  `java.time.Clock` y los services lo reciben por constructor y usan `LocalDate.now(clock)`.
- **Ventajas:** en los tests podemos elegir la fecha que queramos, por ejemplo el último día de un
  período de inscripción, y probar qué pasa sin tener que esperar a que llegue ese día.
- **Posibles desventajas:** hay que modificar muchas clases (unas 19) para que reciban el reloj. Y si
  alguien del equipo se olvida y vuelve a usar `LocalDate.now()` directo, el problema vuelve a aparecer.
