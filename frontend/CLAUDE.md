# Contexto del proyecto: Sistema de Gestión Académica (frontend)

## 1. Descripción general

Estoy desarrollando, junto con un equipo de compañeros, un **Sistema de Gestión Académica** similar a SIU Guaraní o SYSACAD.

El proyecto está dividido en dos partes:

- **Backend:** API REST en Java con Spring Boot, en `../backend` (ver su `CLAUDE.md`).
- **Frontend:** esta aplicación, una SPA en Angular que consume esa API.

## 2. Indicaciones para trabajar sobre el proyecto

Actuá como un desarrollador frontend con experiencia en Angular moderno, TypeScript y consumo de APIs REST.

1. **Respetá el contexto existente:** antes de proponer cambios importantes, analizá el código y la estructura. No supongas que el proyecto empieza de cero.
2. **No inventes implementaciones existentes:** si no sabés cómo está resuelto algo (en el front o en el backend), indicá qué información falta.
3. **Mantené la coherencia arquitectónica:** todo lo nuevo respeta la estructura de la sección 4 y las convenciones de la sección 5.
4. **Usá Angular moderno:** nada de NgModules, `@Input()`/`@Output()`, `*ngIf`/`*ngFor` ni inyección por constructor.
5. **No agregues complejidad innecesaria:** no sumes librerías (NgRx, Material, etc.) ni abstracciones sin una necesidad concreta.
6. **Las reglas de negocio viven en el backend.** El front no recalcula correlativas, cupos ni estados de materias: muestra lo que devuelve la API.
7. **No modifiques decisiones importantes unilateralmente:** si una propuesta cambia la estructura o una decisión tomada, explicá sus consecuencias antes de recomendarla.
8. **Tené en cuenta el trabajo en equipo:** el equipo no tiene experiencia previa en Angular. Preferí soluciones fáciles de entender.

## 3. Forma de responder

Quiero respuestas técnicas, claras y bien fundamentadas.

- Proporcioná código completo cuando sea necesario, indicando en qué archivo va.
- Explicá las partes importantes, sobre todo las que son propias de Angular.
- Señalá errores, problemas de diseño o malas prácticas que detectes.
- Diferenciá entre una solución necesaria y una mejora opcional.

**Importante:** no quiero que simplemente valides mis propuestas. Si hay un error conceptual o una alternativa claramente mejor, señalalo y explicá los motivos.

## 4. Arquitectura del frontend

La decisión completa está en `../docs/adr/0002-estructura-frontend.md`.

El código se organiza **primero por actor y después por feature**:

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
  admin/                        /api/admin/**
    admin.routes.ts
    <feature>/
  docente/                      /api/docente/**
    docente.routes.ts
    <feature>/
  estudiante/                   /api/estudiante/**
    estudiante.routes.ts
    <feature>/
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

### Reglas de dependencia

1. `admin`, `docente` y `estudiante` no se importan entre sí.
2. Todos pueden usar `core` y `shared`. `core` sólo usa `shared`, y `shared` no usa nada del proyecto.
3. Dentro de un mismo actor, una feature puede usar el `data/` de otra, pero no sus `pages/` ni sus `components/`.
4. Sólo las `pages/` inyectan servicios. Los `components/` reciben datos con `input()` y avisan con `output()`.
5. A `shared/` sólo sube lo que usan dos actores o más.

Sheriff verifica las reglas 1 a 3 (ver sección 7). Las reglas 4 y 5 se controlan en la revisión de los pull requests.

### Rutas y seguridad

- `app.routes.ts` tiene una ruta por actor, protegida con `canMatch: [rolGuard(...)]` y cargada con `loadChildren`. Cada actor carga sus features también de forma diferida.
- El guard es sólo para la experiencia de uso: la seguridad real la aplica el backend por prefijo de URL.
- En el frontend no va ningún secreto: todo lo que se compila lo puede leer el usuario.

## 5. Convenciones de código

### Angular

- `inject()` para las dependencias, no el constructor.
- `input()`, `output()` y `model()`, no los decoradores.
- El estado de una pantalla va en `signal()` y `computed()`. La app es zoneless: si un dato que se muestra no está en un signal, la vista no se actualiza.
- En las plantillas, `@if`, `@for` (siempre con `track`) y `@switch`.
- Guards e interceptores como funciones (`rolGuard`, `authInterceptor`), no como clases.
- Plantilla y estilos en archivos separados (`.html` y `.css`), como los genera el CLI.

### Nombres

- **Archivos sin sufijo**, como genera Angular 22: `estudiantes-lista.ts`, no `estudiantes-lista.component.ts`.
- **Carpetas y archivos en minúscula con guiones** (`periodos-inscripcion/`). Clases en PascalCase (`EstudiantesLista`).
- **El nombre de una feature es el del recurso de su URL** en la API: `/api/estudiante/inscripciones-final` → `estudiante/inscripciones-final/`.
- **Clientes HTTP:** `xxx-api.ts` con la clase `XxxApi`.
- **Tipos de los DTO:** interfaces con el mismo nombre que el record del backend (`EstudianteListadoResponse`, `AltaEstudianteRequest`).
- **Selectores** con el prefijo `app-`.

### Terminología

Siempre "estudiante" y "docente", nunca "alumno" ni "profesor": en código, rutas, textos de pantalla y comentarios.

## 6. Estilos y diseño responsive

Se usa **CSS propio**, sin librerías de estilos ni preprocesadores: nada de Tailwind, Bootstrap, Angular Material ni Sass.

### Dónde va cada estilo

- **`src/styles.css`** tiene sólo lo global: las variables de `:root`, un reset básico y los estilos de los elementos base (`body`, `a`, `button`, `input`).
- **Todo lo demás va en el `.css` de cada componente.** Angular encapsula esos estilos, así que no afectan al resto de la app.
- **No se usa `::ng-deep` ni `!important`.** Si un componente necesita cambiar cómo se ve otro, el otro lo permite con un `input()` o una variable CSS.
- **Un archivo de estilos de componente no debería pasar los 4 kB.** `angular.json` avisa con 4 kB y la compilación falla con 8 kB. Si un componente llega a ese tamaño, hay que partirlo.

### Variables

Colores, espacios, tamaños de letra, bordes y sombras se definen una sola vez en `:root`, dentro de `styles.css`, y se usan con `var(--...)`. En un componente nunca se escribe un color ni un espacio a mano.

```css
:root {
  --color-primario: ...;
  --color-texto: ...;
  --color-fondo: ...;
  --color-error: ...;

  --espacio-1: 0.25rem;
  --espacio-2: 0.5rem;
  --espacio-3: 1rem;
  --espacio-4: 1.5rem;
  --espacio-5: 2rem;

  --texto-chico: 0.875rem;
  --texto-base: 1rem;
  --texto-grande: 1.25rem;

  --radio: 0.5rem;
}
```

### Responsive

1. **Mobile first.** Los estilos base son los del celular, y se amplían con `@media (min-width: ...)`. En las media queries no se usa `max-width`.
2. **Hay sólo dos puntos de corte**, y no se usan otros:

   | Nombre | Desde | Para |
   |---|---|---|
   | tablet | `768px` | tablets y celulares en horizontal |
   | escritorio | `1024px` | notebooks y monitores |

   Las variables CSS no funcionan dentro de `@media`, así que el número se escribe cada vez.
3. **Primero, que el diseño se acomode solo.** Flexbox y Grid, por ejemplo `grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr))`. Las media queries quedan para los cambios de disposición, como el menú lateral que pasa a ser un menú hamburguesa.
4. **Los componentes de `shared/ui` usan container queries** (`container-type: inline-size` y `@container`), para que se adapten al espacio que les toca y no al ancho de la pantalla.
5. **Unidades:**
   - `rem` para textos y espacios.
   - `%`, `fr` y `max-width` para anchos.
   - `clamp()` para tamaños que se adaptan.
   - `px` sólo para bordes y sombras. Nunca anchos fijos en `px`.
6. **Las tablas usan siempre `shared/ui/tabla`, y en celular cada fila se muestra como una tarjeta.**
   - Se resuelve sólo con CSS, sin otra plantilla. Siguiendo mobile first, los estilos base muestran las tarjetas: filas y celdas con `display: block`, el encabezado oculto y cada celda con el título de su columna (`[attr.data-label]` y `content: attr(data-label)`). Desde `768px` vuelve a verse como tabla.
   - La primera columna hace de título de la tarjeta. Los botones de acción van al pie.
   - Ninguna pantalla arma sus propias tarjetas para un listado: si hace falta algo distinto, se agrega como opción de `shared/ui/tabla`.
7. **Pantallas táctiles:**
   - Botones y links de al menos 44 × 44 px.
   - Ninguna información que sólo aparezca con `:hover`.
   - Los `input` con letra de al menos `1rem`, para que iOS no haga zoom al tocarlos.
8. **Imágenes** con `max-width: 100%` y `height: auto`.
9. **Si el TypeScript tiene que cambiar según el tamaño** (por ejemplo, para mostrar otro componente), se usa `window.matchMedia` con los mismos puntos de corte. Es la excepción: casi siempre alcanza con CSS.
10. **Prioridad por actor:**
    - Las pantallas de estudiante y docente se diseñan pensando primero en el celular.
    - Las de admin se optimizan para escritorio, pero tienen que funcionar en celular.

### Cómo se prueba

Con el modo dispositivo de las herramientas del navegador (F12), en anchos de `360px`, `768px`, `1024px` y `1440px`. Ninguna pantalla puede tener scroll horizontal en `360px`.

## 7. Verificación del código: ESLint y Sheriff

- **ESLint** (`eslint.config.js`, configurado con `angular-eslint`) detecta errores probables y hace cumplir convenciones de Angular, como el prefijo `app-` en los selectores y la accesibilidad en las plantillas.
- **Sheriff** (`sheriff.config.ts`) es un plugin de ESLint que verifica las reglas de dependencia de la sección 4, como Spring Modulith en el backend.

### Comandos

| Comando | Qué hace |
|---|---|
| `npm run lint` | Corre ESLint, con las reglas de Sheriff incluidas |
| `npx sheriff verify` | Busca imports que rompen las reglas de dependencia |
| `npx sheriff list` | Muestra los módulos que encontró Sheriff y sus etiquetas |

### Reglas

1. **`npm run lint` tiene que pasar sin errores** antes de abrir un pull request. Los PR que lo rompen no se aprueban.
2. **Un error de Sheriff se corrige moviendo el código, no cambiando `sheriff.config.ts`.** Si una feature necesita algo de otro actor, ese algo va a `shared/`. Si necesita algo de otra feature del mismo actor que no está en `data/`, hay que revisar dónde está ubicado.
3. **`sheriff.config.ts` sólo se modifica cuando cambia la estructura**, por ejemplo si se suma un actor o una carpeta nueva al primer nivel de `src/app/`. Cualquier cambio en `depRules` tiene que reflejarse en el ADR 0002.
4. **No se usa `eslint-disable`** para esconder un error. Si una regla no tiene sentido para el proyecto, se apaga en `eslint.config.js` para todos y se explica por qué.
5. **En `eslint.config.js`, `sheriff.configs.all` tiene un `@ts-expect-error`** porque los tipos de Sheriff no coinciden con los de ESLint 10, aunque funciona bien. Si una versión nueva de Sheriff los corrige, el editor marca el comentario como innecesario y hay que borrarlo.

### Cómo se leen las etiquetas de Sheriff

Cada carpeta de feature tiene dos etiquetas, `actor:<actor>` y `tipo:feature`. Su `data/` tiene `actor:<actor>` y `tipo:data`. Un import sólo es válido si **todas** las etiquetas del módulo que importa lo permiten. Por ejemplo:

```
module /src/app/admin/prueba cannot access /src/app/docente/prueba.
Tag actor:admin has no clearance for tags actor:docente, tipo:feature
```

Significa que un módulo de admin intentó importar algo de docente, y la etiqueta `actor:admin` no lo permite.
