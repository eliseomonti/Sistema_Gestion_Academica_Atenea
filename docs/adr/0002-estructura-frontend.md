# ADR 0002: Estructura del frontend por actor y feature

- **Estado:** Aceptada
- **Fecha:** 2026-10-29
- **Decisor/es:** Equipo del proyecto
- **Sustituye a:** N/A

## Contexto

El frontend es una SPA en Angular 22 que consume la API REST del backend (ADR 0001). Es el primer proyecto grande del equipo en Angular, así que hace falta una organización que diga, sin dudas, dónde va cada archivo.

Factores que pesan en la decisión:

- **Tres tipos de usuario** (admin, docente y estudiante), cada uno con su menú, sus pantallas y sus
  permisos. Una misma persona puede ser docente y estudiante.
- **La API ya está partida por actor:** `/api/admin/**`, `/api/docente/**` y `/api/estudiante/**`.
  Salvo `/api/auth`, cada endpoint lo consume un solo actor.
- **Las reglas de negocio viven en el backend y en la base.** El frontend muestra datos y llama a la
  API. Los DTO son casi todos de un solo actor: en `examenes`, por ejemplo, el único que usan dos
  controllers es `CargaNotasRequest`, y los dos son del docente.

## Alternativas consideradas

| Alternativa | Pro | Contra |
|---|---|---|
| **Por tipo técnico** (`components/`, `services/`, `models/`) | Simple al empezar. | Es la v1 del backend: baja cohesión. Tocar una pantalla obliga a abrir varias carpetas. |
| **Por dominio**, igual que el backend (`personas/`, `examenes/`...) | Mismo mapa que el backend. | Cada carpeta mezcla pantallas de tres actores que no comparten menú, diseño ni permisos. |
| **DDD** (dominios con `feature-*`, `ui`, `domain`, `util`) | Espejo de los módulos del backend. Patrón documentado y verificable con Sheriff. | La capa `domain` casi no comparte nada entre actores. Más carpetas por dominio. Lo que ve cada actor queda repartido. Es el mismo argumento por el que el ADR 0001 descarta la hexagonal. |
| **Por features en lista plana** (`features/...`) | La más simple. Es la que muestra la guía de Angular. | Nombres que chocan (exámenes finales del admin y del docente). Las carpetas no dicen de qué actor es cada feature. Con las funcionalidades futuras pasa de 20 carpetas. |
| **Por actor, sin subcarpetas** | Coincide con las rutas, los menús y los permisos. | `admin/` llega a más de 20 pantallas sueltas, como le pasó a `AdministradorController`. |
| **Por actor y feature** | Una ruta diferida y un guard por rol. Features alineadas con la API. Escala con las funcionalidades futuras. | Un nivel más de carpetas. Lo que usan dos actores tiene que ir a `shared/`. |

## Decisión

Se organiza el frontend **primero por actor y después por feature**. Adentro de cada feature se
separan las pantallas, los componentes y el acceso a datos.

### Estructura

```
src/app/
  core/                         lo que existe una sola vez y usa toda la app
    seguridad/                  sesión, interceptor (Bearer + refresh), guard por rol
    layout/                     barra superior y menú según el rol
    http/                       URL base y manejo de errores
  shared/                       sin llamadas HTTP ni estado
    ui/                         tabla, paginador, confirmación, campo con error
    pipes/
    models/                     Pagina<T> y tipos comunes a varios actores
  login/
  admin/
    admin.routes.ts
    estudiantes/                /api/admin/estudiantes
    docentes/                   /api/admin/docentes
    personas/                   /api/admin/personas (datos personales y baja)
    carreras/                   /api/admin/carreras (planes y materias)
    comisiones/                 /api/admin/comisiones (materias, horarios, docentes) y /api/admin/aulas
    periodos-inscripcion/       /api/admin/periodos-inscripcion
    examenes-finales/           /api/admin/examenes-finales
  docente/
    docente.routes.ts
    comisiones-materia/         /api/docente/comisiones-materia y /api/docente/evaluaciones
    examenes-finales/           /api/docente/examenes-finales
  estudiante/
    estudiante.routes.ts
    inscripciones-comision/     /api/estudiante/inscripciones-comision
    inscripciones-final/        /api/estudiante/inscripciones-final
    notas/                      /api/estudiante/notas
    plan-estudio/               /api/estudiante/plan-estudio
  app.routes.ts
```

### Adentro de una feature

```
admin/estudiantes/
  estudiantes.routes.ts         qué pantalla se abre en cada URL
  data/
    estudiantes-api.ts          un método por endpoint, sólo HttpClient
    estudiante.ts               interfaces que copian los DTO del backend
  pages/
    estudiantes-lista/          pantallas con ruta: piden datos y guardan el estado
    estudiante-form/
  components/
    estudiante-filtros/         partes de una pantalla: input() y output()
```

| Si es... | Va en |
|---|---|
| Una llamada a la API | `data/` de la feature que tiene ese endpoint |
| Un tipo que copia un DTO | `data/` de la misma feature |
| Una pantalla con su propia URL | `pages/` |
| Una parte de una pantalla, propia de la feature | `components/` |
| Una parte de una pantalla, genérica | `shared/ui/` |
| Algo de sesión, menú o errores HTTP | `core/` |

### Rutas

`app.routes.ts` define una ruta por actor, protegida con `canMatch` y cargada de forma diferida.
Cada `xxx.routes.ts` de actor carga sus features también de forma diferida.

```ts
{ path: 'admin', canMatch: [rolGuard('ADMIN')], loadChildren: () => import('./admin/admin.routes') },
{ path: 'docente', canMatch: [rolGuard('DOCENTE')], loadChildren: () => import('./docente/docente.routes') },
{ path: 'estudiante', canMatch: [rolGuard('ESTUDIANTE')], loadChildren: () => import('./estudiante/estudiante.routes') },
```

### Reglas de dependencia

1. `admin`, `docente` y `estudiante` no se importan entre sí.
2. Todos pueden usar `core` y `shared`. `core` sólo usa `shared`, y `shared` no usa nada del
   proyecto.
3. Dentro de un mismo actor, una feature puede usar el `data/` de otra, pero no sus `pages/` ni sus
   `components/`. Por ejemplo, `admin/comisiones` usa `admin/docentes/data` para el desplegable de
   docentes.
4. Sólo las `pages/` inyectan servicios. Los `components/` reciben datos con `input()` y avisan con
   `output()`.

### Convenciones

- El nombre de cada feature es el del recurso principal de su URL en la API.
- Los archivos siguen la convención de Angular 22, sin sufijos: `estudiantes-lista.ts`, no
  `estudiantes-lista.component.ts`.

## Consecuencias

**Impactos positivos (beneficios)**

- **Cada pantalla se encuentra en dos pasos:** quién la usa y de qué trata.
- **Carga diferida por rol:** el estudiante nunca descarga el código del admin.
- **Permisos en un solo lugar por actor:** el guard está en la ruta del actor, no repetido en cada
  feature.
- **Sin clientes HTTP duplicados:** cada `data/` corresponde a un grupo de endpoints de un solo
  prefijo.
- **Una persona con dos roles** cambia de sección (de `/docente` a `/estudiante`) sin mezclar
  pantallas.
- **Las funcionalidades futuras entran como features nuevas** dentro de cada actor, sin tocar las
  existentes.

**Impactos negativos (riesgos y costos)**

- **Un nivel más de carpetas** que la lista plana de features.
- **Una funcionalidad de varios actores queda repartida.** Avisos, por ejemplo, va a estar en
  `admin/avisos` y en las carpetas de los actores que los leen.
- **No coincide uno a uno con los módulos del backend.** Para encontrar el endpoint de una pantalla se
  mira el prefijo de la URL, no el nombre del módulo.
- **`shared/` puede llenarse** de cosas que usa una sola feature. Sólo sube ahí lo que usan dos
  actores o más.
- **La regla 4 no la verifica ninguna herramienta.** Depende de la revisión de los pull requests.
- **Sheriff va por la versión 0.20.** Puede cambiar la forma de configurarlo en versiones futuras.

## Cumplimiento

- **Las reglas 1 a 3 las verifica [Sheriff](https://github.com/softarc-consulting/sheriff)**, igual
  que Spring Modulith en el backend. Están escritas en `frontend/sheriff.config.ts` y Sheriff corre
  como plugin de ESLint, así que un import prohibido aparece como error en el editor y en
  `npm run lint`.
- **`npm run lint` tiene que pasar siempre.** Los pull requests no se aprueban si lo rompen.
- **Un error de Sheriff se corrige moviendo el código, no relajando `sheriff.config.ts`.** La
  configuración sólo cambia si cambia la estructura (un actor o una carpeta nueva en el primer nivel
  de `src/app/`), y en ese caso también se actualiza este ADR.
- **La regla 4, y que los archivos nuevos estén donde indica la tabla,** se controlan en la revisión
  de cada pull request.
