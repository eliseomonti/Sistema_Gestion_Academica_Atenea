# 5.2 Comunicación: conflictos, escalamiento y decisiones

> Va después de la 5.1.3 (Registro de riesgos). Las filas salen de lo que ya dice el dossier
> (riesgos, desvíos, SC-02, sección 6) y del historial de GitHub. **Lo marcado con [confirmar] lo
> tienen que completar ustedes**: la consigna pide hechos con impacto real, así que no pongan nada que
> no haya pasado.

## Texto

La comunicación del equipo sigue la estrategia de la Tabla Nº7: el seguimiento diario se hace por
WhatsApp y Discord, el trabajo se registra en Jira y GitHub, y el avance se presenta al docente en las
clases. Cuando un tema no se resuelve entre quienes lo trabajan, se escala según su tipo:

1. **Entre los integrantes involucrados:** dudas técnicas y coordinación del día a día, por Discord o
   WhatsApp.
2. **Scrum Master (Eliseo Monti):** impedimentos y conflictos de trabajo entre integrantes, como
   cambios en la integración del código o tareas trabadas.
3. **Product Owner (Federico Solá):** decisiones de alcance y prioridad. Si un cambio afecta una línea
   de base, se registra como solicitud de cambio (sección 6.8).
4. **Docente de la cátedra:** reglas de negocio que el equipo no puede definir solo y cambios en los
   hitos que la cátedra revisa.

Cada decisión que cambia el trabajo del equipo se deja escrita en Jira, en un ADR o en este dossier,
para que no dependa de una conversación. A continuación se registran los conflictos, escalamientos y
decisiones de comunicación que tuvieron impacto real en el proyecto.

### 5.2.1 Conflictos

**Commits directos a main (28/10/2026).** Dos commits del módulo de exámenes entraron directo a la rama
main, sin pull request ni revisión de otro integrante. El Scrum Master lo planteó al equipo por
Discord [confirmar canal] y se acordó que, desde ese momento, todo el código entra a main por pull
request revisado, como exige la Definición de Terminado. Desde el 30/10/2026 no hubo más commits
directos a main (desvío D-03).

### 5.2.2 Escalamiento

**Corrimiento de los hitos del backend (14/10/2026 [confirmar]).** El backend de administrador,
docente y estudiante no llegaba al hito del 14/10. El Scrum Master lo planteó al docente en el
Checkpoint 2 [confirmar] y se decidió extender el Sprint 1 una semana y correr los hitos 2, 3 y 4 al
28/10, sin mover las fechas de entrega. Se actualizó la línea de base temporal (SC-02) y el frontend
quedó con menos tiempo.

**Salida de un integrante ([confirmar fecha]).** Juan Segundo Ibarra no quedó habilitado para cursar
Programación IV, porque no tenía aprobada la correlativa (Programación II). Como el sistema es el
trabajo integrador de esa materia, dejó de participar y el equipo pasó de cuatro a tres integrantes.
El equipo lo informó al docente [confirmar canal] y decidió continuar con tres integrantes,
redistribuyendo sus tareas: los paquetes de documentación y gestión pasaron a Federico Solá. Esto
aumentó la carga de cada integrante y reforzó la priorización MoSCoW para asegurar el MVP (RSK-02).
La portada, el Project Charter y las responsabilidades de la EDT se actualizaron con los tres
integrantes.

**Publicación del sistema para la defensa ([confirmar fecha]).** No estaba definido si la defensa
requiere el sistema publicado en una URL pública o si alcanza con una demostración local (información
pendiente de la sección 1.6). El equipo lo consultó al docente en clase y [confirmar la respuesta y
lo que decidió el equipo] (RSK-03).

### 5.2.3 Decisiones

**Rehacer la base de datos y el backend (agosto de 2026 [confirmar]).** El equipo no estaba conforme
con la base de datos ni con el backend hechos en el primer cuatrimestre. En una reunión por Discord
[confirmar canal] se decidió rehacerlos con una arquitectura documentada (ADR 0001) antes de
construir pantallas. Esto ocupó semanas del cuatrimestre y el frontend empezó recién el 29/10/2026
(RSK-01).

**Sacar los ABM de carreras, planes y materias del MVP (septiembre de 2026 [confirmar]).** El MVP
inicial incluía esos ABM y no entraba en el plazo. El Product Owner decidió sacarlos: quedan de solo
lectura y los datos se cargan por scripts SQL. Así se protegió el tiempo de las funcionalidades
principales (RSK-07, sección 2.12).

**Estructura del frontend (29/10/2026).** Antes de empezar el frontend, el equipo no tenía una regla
común sobre dónde ubicar cada archivo, con riesgo de pisarse al integrar. Se acordó organizarlo por
actor y por feature, y verificar las reglas con Sheriff (ADR 0002). Así cada integrante trabajó en
carpetas separadas y los pull requests se integraron sin conflictos.
