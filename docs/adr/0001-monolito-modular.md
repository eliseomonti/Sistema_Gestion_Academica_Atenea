# ADR 0001: Monolito modular por dominio en backend

- **Estado:** Aceptada
- **Fecha:** 2026-10-13

## Contexto

Se busca un tipo de arquitectura para el backend del proyecto que genere alta cohesión y bajo acoplamiento, 
y provea una organización optima para el mismo

## Decisión

Se adopta un **monolito modular por dominio** (package-by-feature):

- Una sola aplicación y un solo despliegue.
- El código se divide en **módulos por tema de negocio**. Cada uno tiene adentro capas clásicas:
  `controller`, `service`, `model`, `repository`, `dto` y `mapper`.
- Los módulos se ordenan en **niveles**, y uno sólo puede depender de módulos de nivel menor:

  | Nivel | Módulos |
  |---|---|
  | 0 | `seguridad`, `carreras` |
  | 1 | `personas` |
  | 2 | `comisiones` |
  | 3 | `examenes` |
  | 4 | `inscripciones` |
  | 5 | futuros: `avisos`, `cuotas`, `encuestas`, `tramites` |
  | — | `compartido` (módulo abierto, lo usan todos) |

- **Encapsulamiento:** la raíz de cada módulo es su parte pública (la interfaz `XxxApi` y los records
  y enums que devuelve). Todo lo que está en subpaquetes es interno.
- **Entre módulos se referencia por id** (`Long materiaId`), no con relaciones JPA a entidades ajenas.
- **Las fronteras se verifican con Spring Modulith** (línea 2.x para Boot 4): un test de modularidad
  (`ApplicationModules.of(...).verify()`) falla si un módulo usa algo interno de otro, si hay un ciclo
  o si depende de un módulo que su `package-info.java` no permite (`allowedDependencies`).
- **Ciclo `examenes` ↔ `inscripciones`:** se corta con un puerto (`Inscriptos`) declarado en
  `examenes` e implementado en `inscripciones`.
- **Interfaz o evento:** si un módulo necesita una respuesta, usa la interfaz del otro. Si sólo pasó
  algo y otros pueden reaccionar, se publica un evento (el de abajo publica, el de arriba escucha).

## Alternativas consideradas

| Alternativa | Veredicto |
|---|---|
| **Capas por tipo** | Descartada. Baja cohesión, acoplamiento alto e invisible|
| **Por actor** (admin, docente, estudiante) | Descartada. Duplica reglas entre actores.|
| **Hexagonal completa** | Posible, pero **no recomendada como estructura global** (ver abajo). |
| **Arquitectura limpia / cebolla** | Mismo veredicto que hexagonal, con más ceremonia. |
| **Microservicios** | Descartada. Un equipo chico, una sola base y transacciones que cruzan módulos (inscripción, cupo, notas): el costo operativo no se justifica. |

### Por qué no hexagonal como estructura global

1. Cuesta unos 12 archivos por caso de uso contra unos 7.
2. Parte del dominio vive en la base (CHECK, FK compuestas, triggers), así que un dominio "puro" en
   Java no representaría el sistema completo.
3. No hay infraestructura que intercambiar: una sola base de datos, MySQL.
4. La mayor parte del MVP es ABM y listados, donde la ceremonia no aporta.

De la hexagonal sí se toman **tres ideas puntuales**: la situación académica (correlativas, estado
de materias) como clase en Java puro y con tests; puertos para mail, pago y PDF cuando lleguen; e
inversión de dependencias para cortar ciclos.

## Consecuencias

**Positivas**

- Alta cohesión: cada regla vive en un solo lugar (correlativas, estado de materias y cupo en
  `inscripciones`; docente asignado en `comisiones`; nota vigente en `examenes`).
- Bajo acoplamiento **verificado por un test**, no sólo por convención.
- Un solo despliegue y una sola base: las transacciones que cruzan módulos son simples.
- Se puede construir de abajo hacia arriba, un módulo terminado por vez.
- Si algún módulo creciera mucho, está listo para extraerse, porque ya se comunica por interfaces.

**Negativas / costos**

- Hay que **mantener a mano** las listas `allowedDependencies` cuando un módulo pasa a depender de
  otro. Si resulta una molestia constante, es señal de que los módulos están mal cortados.
- Los niveles son una hipótesis surgida del esquema y del MVP: pueden requerir ajustes.
- Referenciar por id entre módulos implica consultas extra y no se pueden usar joins JPA entre
  módulos.
- Las capas clásicas dentro de cada módulo no impiden por sí solas un service demasiado grande: se
  parte por caso de uso cuando pasa las 250 líneas o las 6 dependencias.
- Es un monolito: un fallo no controlado afecta a toda la aplicación.

## Cómo se verifica

- `ModularityTests` corre con el resto de los tests y tiene que pasar siempre.
- Los pull requests no se aprueban si lo rompen.

## Revisión

Esta decisión se reevalúa si aparece alguno de estos casos: más de la mitad de las dependencias
nuevas obligan a editar `allowedDependencies`; un módulo necesita escalar o desplegarse por separado;
o se incorpora infraestructura que justifique puertos adicionales (mail, pagos, PDF).
