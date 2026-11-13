# Sección 4: lo que falta

> Las tablas nuevas van como **Tabla X**: numérenlas cuando pasen todo el documento a "Tabla N"
> correlativa (la consigna lo exige), así no renumeran dos veces.

---

## A) Agregar al final de la 4.8 (después del Cuadro de sobrecarga)

**Duración y costo según PERT**

- **Esfuerzo esperado:** 1.856,9 horas, que equivalen a 11,6 personas-mes (a 160 horas por persona-mes).
- **Duración:** el equipo son tres integrantes con dedicación parcial, de unas 15 horas semanales cada
  uno (45 horas por semana en total). Con ese ritmo, el proyecto lleva 1.856,9 / 45 = 41,3 semanas,
  es decir, unos **9,5 meses**. Con el rango al 95 % (1.689 a 2.025 horas con sobrecarga), la duración
  va de 8,7 a 10,4 meses. El resultado coincide con el ciclo de vida del proyecto (enero a noviembre
  de 2026, sección 1.3). A tiempo completo, los mismos tres integrantes lo harían en 3,9 meses.
- **Costo:** 1.856,9 horas × USD 7,94 por hora (USD 1.270 por persona-mes / 160 horas) = USD 14.739
  de personal, más USD 350 de infraestructura (USD 35 por mes durante 10 meses): **USD 15.089**.
  Con el rango al 95 %, el costo va de USD 13.756 a USD 16.422.

---

## B) 4.10 Actividades, dependencias, responsables y recursos

Cada actividad corresponde a un paquete de trabajo de la EDT (sección 3.2). El esfuerzo es el
estimado con PERT, incluida la sobrecarga (Tabla de la sección 4.8). Las dependencias son de tipo
fin-inicio: una actividad empieza cuando terminan sus predecesoras.

*Tabla X
**Actividades del proyecto**

| ID | Actividad (paquete EDT) | Predecesoras | Responsable | Esfuerzo (h) | Período |
|---|---|---|---|---|---|
| A01 | 1.1.1 Iniciación y requisitos | — | Solá | 36,5 | Ene – Feb |
| A02 | 1.1.2 Arquitectura y decisiones | A01 | Monti | 48,9 | Mar – Abr |
| A03 | 1.7.1 Modelo y esquema SQL | A01 | Silva / Monti | 82,6 | Mar – Abr |
| A04 | 1.2.1 Autenticación JWT | A02, A03 | Monti | 68,5 | May |
| A05 | 1.3.1 Estructura académica | A03 | Silva | 34,1 | May |
| A06 | 1.2.2 Control de acceso por roles | A04 | Monti | 59,4 | Jun |
| A07 | 1.3.2 Personas y roles | A04, A05 | Silva / Monti | 116,2 | Jun – Jul |
| A08 | 1.3.3 Comisiones | A07 | Monti / Silva | 142,1 | Jul – Ago |
| A09 | 1.3.4 Períodos y mesas de final | A05 | Solá / Monti | 116,2 | Ago – Sep |
| A10 | 1.1.3 Estimación y planificación | A02 | Solá | 48,9 | Ago – Sep |
| A11 | 1.4.1 Materias y alumnos | A06, A08 | Silva | 59,4 | Ago |
| A12 | 1.4.2 Evaluaciones y notas | A11 | Silva | 116,2 | Sep |
| A13 | 1.4.3 Cierre de actas | A12 | Silva | 102,0 | Sep – Oct |
| A14 | 1.4.4 Mesas de final (docente) | A09, A12 | Silva | 76,3 | Oct |
| A15 | 1.5.1 Inscripción a cursadas | A08, A09 | Solá | 149,9 | Sep – Oct |
| A16 | 1.5.2 Finales (estudiante) | A09, A13 | Silva | 133,0 | Oct |
| A17 | 1.5.3 Seguimiento académico | A13, A15 | Silva | 102,0 | Oct – Nov |
| A18 | 1.6.1 Interceptor de tokens y sesión | A04 | Monti | 50,4 | Oct |
| A19 | 1.6.2 Errores y pruebas de integración | A14, A16, A17, A18 | Monti / Solá | 118,9 | Oct – Nov |
| A20 | 1.7.2 Datos de demostración | A03, A09 | Silva | 41,4 | Oct – Nov |
| A21 | 1.1.4 Riesgos, seguimiento y cambios | A10 | Solá | 61,3 | Sep – Nov |
| A22 | 1.8.1 README y guía | A19 | Solá | 19,9 | Nov |
| A23 | 1.8.2 Manual de usuario | A19 | Solá | 30,7 | Nov |
| A24 | 1.1.5 Cierre y presentación | A21, A22, A23 | Todos | 42,2 | Nov |
| | **Total** | | | **1.856,9** | |

**Camino crítico:** A01 → A03 → A04 → A07 → A08 → A11 → A12 → A13 → A16 → A19 → A22 → A24. Son las
actividades que no tienen holgura: si una se atrasa, se atrasa la entrega. Por eso los módulos de
administrador y docente se priorizan antes que la documentación y los datos de demostración.

**Recursos:**

- **Humanos:** Federico Solá (Product Owner), Eliseo Monti (Scrum Master) y Joaquín Silva
  (Developer), con dedicación parcial de unas 15 horas semanales cada uno. Los tres desarrollan.
- **Software de desarrollo:** IntelliJ IDEA y VS Code, Java 21 con Spring Boot, Angular 22, MySQL 8 y
  Postman, todos gratuitos o con licencia para estudiantes.
- **Gestión:** Jira (backlog y sprints), GitHub (código y pull requests), draw.io (diagramas) y Google
  Docs (dossier).
- **Servicios pagos (presupuestados en la Tabla Nº2):** herramienta de IA (USD 20 por persona por mes),
  hosting del frontend y backend (USD 20 por mes) y base MySQL administrada (USD 15 por mes).
- **Consulta:** el docente de la cátedra, para validar reglas de negocio en las clases.

---

## C) 4.11 Cronograma y línea de base temporal

*Figura X
**Cronograma del proyecto (diagrama de Gantt)**

| Actividad | Ene | Feb | Mar | Abr | May | Jun | Jul | Ago | Sep | Oct | Nov |
|---|---|---|---|---|---|---|---|---|---|---|---|
| A01 Iniciación y requisitos | ■ | ■ | | | | | | | | | |
| A02 Arquitectura y decisiones | | | ■ | ■ | | | | | | | |
| A03 Modelo y esquema SQL | | | ■ | ■ | | | | | | | |
| A04 Autenticación JWT | | | | | ■ | | | | | | |
| A05 Estructura académica | | | | | ■ | | | | | | |
| A06 Control de acceso por roles | | | | | | ■ | | | | | |
| A07 Personas y roles | | | | | | ■ | ■ | | | | |
| A08 Comisiones | | | | | | | ■ | ■ | | | |
| A09 Períodos y mesas de final | | | | | | | | ■ | ■ | | |
| A10 Estimación y planificación | | | | | | | | ■ | ■ | | |
| A11 Materias y alumnos | | | | | | | | ■ | | | |
| A12 Evaluaciones y notas | | | | | | | | | ■ | | |
| A13 Cierre de actas | | | | | | | | | ■ | ■ | |
| A14 Mesas de final (docente) | | | | | | | | | | ■ | |
| A15 Inscripción a cursadas | | | | | | | | | ■ | ■ | |
| A16 Finales (estudiante) | | | | | | | | | | ■ | |
| A17 Seguimiento académico | | | | | | | | | | ■ | ■ |
| A18 Interceptor y sesión | | | | | | | | | | ■ | |
| A19 Errores y pruebas de integración | | | | | | | | | | ■ | ■ |
| A20 Datos de demostración | | | | | | | | | | ■ | ■ |
| A21 Riesgos, seguimiento y cambios | | | | | | | | | ■ | ■ | ■ |
| A22 README y guía | | | | | | | | | | | ■ |
| A23 Manual de usuario | | | | | | | | | | | ■ |
| A24 Cierre y presentación | | | | | | | | | | | ■ |
| **Hitos** | | | | | | | | | H0 | H1–H4 | H5–H7 |

H0: Checkpoint 1 (16/09). H1 a H4: backend de seguridad, administrador, docente y estudiante (14/10).
H5: integración del frontend (17/11). H6: entrega del dossier (11/11). H7: entrega del código (17/11).

**Línea de base temporal:** la forman este cronograma y las fechas de los hitos aprobadas en el
Checkpoint 1 (16/09/2026). Los paquetes de los módulos incluyen la API y las pantallas: la API se
cierra en el hito de cada módulo y las pantallas en el hito 5. Los cambios aprobados sobre esta línea
de base se registran en la sección 6.8.

*Tabla X
**Línea de base temporal de los hitos**

| Hito | Descripción | Línea de base v1.0 (16/09/2026) | Línea de base v1.1 (SC-02) |
|---|---|---|---|
| H1 | Estructura base y seguridad | 14/10/2026 | 14/10/2026 |
| H2 | Módulo de gestión (administrador) | 14/10/2026 | 28/10/2026 |
| H3 | Módulo docente | 14/10/2026 | 28/10/2026 |
| H4 | Módulo estudiante | 14/10/2026 | 28/10/2026 |
| H5 | Integración frontend | 17/11/2026 | 17/11/2026 |
| H6 | Entrega del dossier de gestión | 11/11/2026 | 11/11/2026 |
| H7 | Entrega del código (Programación IV) | 17/11/2026 | 17/11/2026 |

---

## D) 4.12 Presupuesto y línea de base de costos

El presupuesto se arma a partir de la estimación PERT, paquete por paquete. La hora de trabajo se
valúa en USD 7,94 (USD 1.270 por persona-mes, que suma remuneración, viáticos e IA de la Tabla Nº2,
dividido 160 horas).

*Tabla X
**Presupuesto por grupo de la EDT**

| Grupo EDT | Esfuerzo (h) | Costo (USD) |
|---|---|---|
| 1.1 Gestión del proyecto | 237,7 | 1.887 |
| 1.2 Seguridad y autenticación | 127,9 | 1.015 |
| 1.3 Módulo administrador | 408,7 | 3.244 |
| 1.4 Módulo docente | 353,9 | 2.809 |
| 1.5 Módulo estudiante | 384,9 | 3.055 |
| 1.6 Integración frontend–backend | 169,3 | 1.344 |
| 1.7 Base de datos | 124,0 | 984 |
| 1.8 Documentación técnica y de usuario | 50,6 | 402 |
| **Subtotal personal** | **1.856,9** | **14.739** |
| Infraestructura (hosting y base de datos, 10 meses × USD 35) | | 350 |
| **Costo estimado** | | **15.089** |
| Reserva de contingencia (hasta el máximo del rango al 95 %) | | 1.333 |
| **Línea base de costos** | | **16.422** |
| Reserva de gestión | | 22.028 |
| **Presupuesto total (Tabla Nº2)** | | **38.450** |

**Cómo se relaciona con la Tabla Nº2:** el presupuesto preliminar de la Tabla Nº2 (USD 38.450) supone
tres integrantes durante diez meses completos, es decir, 30 personas-mes. La estimación PERT da 11,6
personas-mes, porque el equipo trabaja con dedicación parcial. Por eso el presupuesto se separa en
tres partes:

- La **línea base de costos** (USD 16.422) es el costo esperado más una reserva de contingencia para
  los riesgos identificados. Contra ella se controla el avance en la sección 6.
- La **reserva de gestión** (USD 22.028) es la diferencia con el presupuesto aprobado. Cubre riesgos
  no identificados y solo se usa con una solicitud de cambio aprobada (sección 6.8).
- El **presupuesto total** se mantiene en USD 38.450, el aprobado en el Project Charter.
