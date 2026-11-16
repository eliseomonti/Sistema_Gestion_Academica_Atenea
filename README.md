# ATENEA — Sistema de Gestión Académica Universitaria

**ATENEA** es una aplicación web de gestión académica universitaria que centraliza la consulta de planes de estudio y correlatividades, el padrón único de personas con múltiples roles simultáneos, la administración de comisiones, períodos de inscripción y mesas de examen final, la carga de evaluaciones y el cierre inmutable de actas por parte de los docentes, y la autogestión de inscripciones y seguimiento de avance académico por parte de los estudiantes.

Proyecto desarrollado en el marco de las asignaturas **Gestión de Desarrollo de Software** y **Programación IV** de la **Tecnicatura Universitaria en Programación (UTN — Facultad Regional Mar del Plata)**, ciclo lectivo 2026.

## Integrantes

- **Federico Solá** — Product Owner / Desarrollo
- **Eliseo Monti** — Scrum Master / Desarrollo
- **Joaquín Silva** — Developer

---

## Stack tecnológico y arquitectura

| Capa | Tecnología | Detalle arquitectónico |
| :--- | :--- | :--- |
| **Backend** | Java 21, Spring Boot 4.1.1, Spring Security, Spring Data JPA, MapStruct, Lombok, springdoc-openapi | Monolito modular verificado con **Spring Modulith** ([ADR 0001](docs/adr/0001-monolito-modular.md)). Autenticación sin estado mediante JWT (`accessToken` de 15 min + `refreshToken` de 7 días persistido en BD). Respuestas de error estandarizadas con `ProblemDetail` (RFC 9457). |
| **Frontend** | Angular 22, TypeScript 5.9, RxJS, CSS con variables de diseño propias | Componentes *standalone* con `ChangeDetectionStrategy.OnPush` y Signals. Organización por actor (`admin`, `docente`, `estudiante`) y fronteras de importación verificadas con **Sheriff** y **ESLint** ([ADR 0002](docs/adr/0002-estructura-frontend.md)). |
| **Base de datos** | MySQL 8.x / MariaDB 10.6+ | Esquema relacional InnoDB (`utf8mb4`) con integridad referencial, restricciones `CHECK` y datos de demostración basados en planes de estudio reales de la UTN FRMDP. |

---

## Estructura del repositorio

```text
.
├── .github/workflows/
│   └── ci.yml                # CI/CD: verificación en cada PR y deploy automático a Azure al mergear a main
├── backend/                  # API REST en Spring Boot 4.1.1 (Java 21)
│   ├── src/main/java/        # Módulos: compartido, seguridad, carreras, personas,
│   │                         #          comisiones, examenes, inscripciones
│   ├── src/main/resources/   # application.properties y application-prod.properties
│   ├── Dockerfile            # Imagen del backend (compila con Maven y corre sobre JRE 21)
│   └── CLAUDE.md             # Guía de arquitectura, reglas de negocio y convenciones del backend
├── frontend/                 # Aplicación web en Angular 22
│   ├── src/app/              # core/, shared/ui/, features/ (auth, admin, docente, estudiante)
│   ├── sheriff.config.ts     # Reglas de dependencia entre módulos y features
│   ├── proxy.conf.json       # En desarrollo, `ng serve` reenvía /api al backend (localhost:8080)
│   ├── Dockerfile            # Imagen del frontend (compila Angular y lo sirve con nginx)
│   ├── nginx.conf            # Sirve la aplicación y reenvía /api al backend
│   └── CLAUDE.md             # Guía de arquitectura y convenciones del frontend
├── bdd/                      # Scripts SQL de creación de esquema y carga de datos de demostración
│   ├── atenea_db_script.sql  # Esquema completo de la base atenea_db, roles y usuario admin inicial
│   ├── 01-carreras.sql       # 7 carreras, 8 planes de estudio, materias y correlatividades
│   ├── 02-personas.sql       # Padrón de prueba: administradores, 30 docentes, +700 estudiantes
│   └── 03-aulas.sql          # 35 aulas, laboratorios y espacios comunes
├── docs/
│   ├── adr/                  # Architecture Decision Records (ADR 0001 a ADR 0003)
│   └── manual-usuario.md     # Manual de usuario por rol (Administrador, Docente, Estudiante)
├── docker-compose.yml        # Base de datos, backend y frontend en contenedores (desarrollo)
├── docker-compose.prod.yml   # Ajustes para producción: HTTPS con Caddy, secretos y reinicio automático
├── Caddyfile                 # Configuración de Caddy (certificado HTTPS y entrada al frontend)
└── .env.example              # Plantilla de las variables de entorno de producción
```

---

## Requisitos previos

Hay dos formas de levantar el proyecto. Alcanza con cumplir los requisitos de una de ellas.

- **Con Docker (recomendada):** **Docker** y **Docker Compose**. No hace falta instalar Java, Node ni MySQL.
- **Sin Docker:**
  - **JDK 21** instalado y configurado en `JAVA_HOME`.
  - **Node.js 20+** y **npm 10+**.
  - **MySQL 8.0+** o **MariaDB 10.6+** en ejecución en el puerto `3306`.

---

## Puesta en marcha con Docker (recomendada)

Desde la raíz del repositorio:

```bash
docker compose up -d --build
```

Este comando construye las imágenes y levanta tres contenedores:

| Servicio | Qué es | Acceso desde la PC |
| :--- | :--- | :--- |
| `mysql` | MySQL 8.4 con la base `atenea_db` | `localhost:3307` (usuario `root`, contraseña `root`) |
| `backend` | API REST en Spring Boot | `http://localhost:8080` |
| `frontend` | Aplicación Angular servida con nginx | **`http://localhost:8081`** |

- La primera vez, MySQL ejecuta automáticamente los scripts de `bdd/` en orden (esquema, carreras, personas y aulas). El backend espera a que la base esté lista antes de arrancar.
- Los datos se guardan en el volumen `mysql-data`, de modo que se conservan aunque se apaguen o se borren los contenedores.
- MySQL usa el puerto `3307` para no chocar con un MySQL instalado localmente en el `3306`.

Comandos útiles:

```bash
docker compose ps                # estado de los servicios
docker compose logs -f backend   # logs del backend en vivo
docker compose up -d --build     # reconstruir y levantar luego de cambiar código
docker compose stop              # apagar los servicios (los datos se conservan)
docker compose down -v           # borrar contenedores y base; al volver a levantar se recargan los scripts
```

> Los scripts de `bdd/` sólo se ejecutan cuando el volumen de la base está vacío. Si se modifica un script, hay que correr `docker compose down -v` para que se vuelva a cargar.

---

## Puesta en marcha sin Docker

### 1. Crear y poblar la base de datos

Ejecutar los cuatro scripts ubicados en la carpeta `bdd/` en el orden indicado:

```bash
mysql -u root -p < bdd/atenea_db_script.sql
mysql -u root -p < bdd/01-carreras.sql
mysql -u root -p < bdd/02-personas.sql
mysql -u root -p < bdd/03-aulas.sql
```

1. `bdd/atenea_db_script.sql`: crea la base de datos `atenea_db`, todas las tablas del modelo, los tres roles del sistema (`ROLE_ADMIN`, `ROLE_DOCENTE`, `ROLE_ESTUDIANTE`) y el administrador base.
2. `bdd/01-carreras.sql`: carga 7 carreras reales de la UTN FRMDP (incluyendo la Tecnicatura Universitaria en Programación planes 2003 y 2024, Ingeniería Naval, Ingeniería Pesquera, Administración, Tecnologías Ambientales, Interiorismo y Producción Textil) con sus materias y matriz de correlatividades.
3. `bdd/02-personas.sql`: inserta administradores, 30 docentes y más de 700 estudiantes con inscripciones a carreras y casos especiales de prueba.
4. `bdd/03-aulas.sql`: carga 35 aulas, laboratorios y espacios comunes.

### 2. Configurar y levantar el backend

Por defecto, `backend/src/main/resources/application.properties` conecta a `jdbc:mysql://localhost:3306/atenea_db` con usuario `root` y contraseña `root`. Cada valor puede cambiarse con una variable de entorno:

| Variable | Valor por defecto |
| :--- | :--- |
| `DB_URL` | `jdbc:mysql://localhost:3306/atenea_db` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | `root` |
| `JWT_SECRET` | Clave de desarrollo (no usar en producción) |
| `CORS_ORIGINS` | `http://localhost:4200` |

Si el entorno local utiliza otras credenciales o el conector de MariaDB, también puede crearse el archivo `backend/src/main/resources/application-local.properties` (ignorado por Git):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/atenea_db
spring.datasource.username=root
spring.datasource.password=tu_password
```

Iniciar el servidor desde la carpeta `backend/`:

```bash
cd backend
./mvnw spring-boot:run
```

- La API REST quedará disponible en `http://localhost:8080`.
- La documentación interactiva **OpenAPI / Swagger UI** puede consultarse en `http://localhost:8080/swagger-ui/index.html` (esquema JSON en `http://localhost:8080/v3/api-docs`).

### 3. Instalar dependencias y levantar el frontend

En otra terminal, situarse en la carpeta `frontend/`:

```bash
cd frontend
npm install
npm start
```

- La aplicación web quedará disponible en `http://localhost:4200`.
- El frontend llama a la API en la ruta relativa `/api`. En desarrollo, `ng serve` la reenvía a `http://localhost:8080` mediante `proxy.conf.json`, por lo que el backend tiene que estar levantado en ese puerto.

---

## Despliegue en producción

### Producción actual: Azure

La aplicación publicada está en **https://atenea-frontend.calmcliff-ac059761.chilecentral.azurecontainerapps.io**.

```text
Navegador ──HTTPS──▶ atenea-frontend (nginx) ──/api──▶ atenea-backend (Spring Boot, interno)
                                                            └──TLS──▶ Azure Database for MySQL
```

| Recurso (grupo `rg-atenea`, región Chile Central) | Qué es |
| :--- | :--- |
| `atenea-frontend` | Container App con la imagen del frontend. Es la única puerta pública; Azure provee el HTTPS. |
| `atenea-backend` | Container App con la imagen del backend, con ingreso **interno**: sólo la ve el frontend. Corre con el perfil `prod`. |
| `atenea-mysql-e8ff1a` | Azure Database for MySQL 8.4 (Flexible Server, B1ms), con backups automáticos de 7 días. |
| `atenea-env` | Entorno de Container Apps (red interna compartida por las dos apps). |
| `id-atenea-github` | Identidad que usa GitHub Actions para deployar (rol *Contributor* sólo sobre `rg-atenea`). |

Las imágenes se publican en GitHub Container Registry (`ghcr.io/eliseomonti/atenea-backend` y `atenea-frontend`), etiquetadas con el hash corto del commit. Las contraseñas de la base y la clave de los JWT están guardadas como *secretos* de las Container Apps, no en el repositorio.

#### Cómo se deploya

**Mergeando a `main`.** El workflow [`ci.yml`](.github/workflows/ci.yml) hace todo automáticamente:

1. **En cada pull request:** corre los tests del backend (contra un MySQL 8.4 con los scripts de `bdd/`), y el lint, los tests y el build del frontend. Si algo falla, el PR no se puede mergear.
2. **Al mergear a `main`:** vuelve a correr la verificación y, si pasa, construye las dos imágenes, las publica en GHCR, actualiza las Container Apps y comprueba que la aplicación responda.

El historial de deploys se ve en la sección **Deployments → produccion** del repositorio, y el detalle de cada corrida en la pestaña **Actions**. GitHub entra a Azure con **OIDC** (identidad federada): no hay contraseñas de Azure guardadas en GitHub.

#### Volver a una versión anterior

Cada deploy crea una **revisión** nueva de cada Container App. Para volver atrás, en el portal de Azure: *Container App → Revisions* y activar la anterior. O bien, desde la CLI, apuntar a la imagen de un commit previo:

```bash
az containerapp update -n atenea-backend  -g rg-atenea --image ghcr.io/eliseomonti/atenea-backend:<hash>
az containerapp update -n atenea-frontend -g rg-atenea --image ghcr.io/eliseomonti/atenea-frontend:<hash>
```

#### Cambios en el esquema de la base

El deploy **no modifica la base**. Si un cambio agrega o modifica tablas o columnas, el SQL correspondiente se aplica en la base de Azure **antes** de mergear: el backend valida el esquema al arrancar (`ddl-auto=validate`) y no levanta si no coincide.

#### Mantenimiento

- **Token de GHCR:** Azure descarga las imágenes con un token de GitHub de sólo lectura que **vence cada 90 días** (el actual, alrededor del 14/02/2027). Al vencer, la aplicación sigue funcionando pero los deploys nuevos fallan. Se renueva creando un token nuevo (*classic*, scope `read:packages`) y actualizándolo en las dos apps:

  ```bash
  az containerapp registry set -n atenea-backend  -g rg-atenea --server ghcr.io --username eliseomonti --password <token>
  az containerapp registry set -n atenea-frontend -g rg-atenea --server ghcr.io --username eliseomonti --password <token>
  ```

- **Costos:** la base está cubierta por los servicios gratuitos de Azure (750 h/mes de B1ms y 32 GB). Las Container Apps se pagan con el crédito de Azure for Students. Para gastar menos cuando no se usa la aplicación, se puede permitir que se apaguen solas (`--min-replicas 0`), a costa de unos segundos de espera en el primer acceso.

### Alternativa: servidor propio con Docker Compose

Si se despliega en una máquina virtual propia (por ejemplo, Oracle Cloud o una VPS) en lugar de Azure, se usa `docker-compose.prod.yml` encima del compose base. Este archivo:

- Agrega **Caddy**, que obtiene y renueva automáticamente el certificado HTTPS (Let's Encrypt) y redirige `http://` a `https://`.
- Deja expuestos sólo los puertos `80` y `443`. La base, el backend y nginx quedan accesibles únicamente por la red interna de Docker.
- Activa el perfil `prod` del backend: si falta `DB_PASSWORD` o `JWT_SECRET`, la aplicación no arranca, en lugar de usar los valores de desarrollo.
- Reinicia los contenedores automáticamente si se caen o si se reinicia el servidor.

Pasos en el servidor (con Docker y Docker Compose instalados, los puertos `80` y `443` abiertos y un dominio apuntando a su IP):

```bash
git clone <url-del-repositorio> atenea
cd atenea
cp .env.example .env    # completar DOMINIO, DB_PASSWORD y JWT_SECRET
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

- `JWT_SECRET` debe tener al menos 32 caracteres. Puede generarse con `openssl rand -base64 48`.
- `DB_PASSWORD` sólo se aplica la primera vez que se crea la base. Si luego se cambia en el `.env`, MySQL conserva la contraseña anterior y el backend no puede conectarse.
- El archivo `.env` contiene secretos y está ignorado por Git: nunca debe subirse al repositorio.

Para actualizar la aplicación luego de un cambio:

```bash
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

---

## Verificación de calidad y arquitectura

Estas verificaciones corren automáticamente en cada pull request (GitHub Actions, ver [`ci.yml`](.github/workflows/ci.yml)), y la rama `main` no acepta cambios que no las pasen. También pueden correrse localmente:

### Backend (Spring Modulith y pruebas)

Verifica que no existan dependencias cíclicas entre módulos ni accesos directos a paquetes internos (`interno`) de otros módulos:

```bash
cd backend
./mvnw test
```

### Frontend (ESLint + Sheriff)

Verifica las reglas de código y el aislamiento entre `core`, `shared` y cada *feature* (`admin`, `docente`, `estudiante`):

```bash
cd frontend
npm run lint
```

---

## Credenciales de prueba (datos de demostración)

Todos los usuarios generados por `bdd/02-personas.sql` ingresan utilizando su **DNI como nombre de usuario** y la contraseña **`Prueba123`**:

| Perfil / Caso de prueba | Usuario (DNI) | Contraseña | Persona | Roles asignados |
| :--- | :--- | :--- | :--- | :--- |
| **Administrador** | `28031246` | `Prueba123` | Mariela Fontana | `ROLE_ADMIN` |
| **Docente** | `13096033` | `Prueba123` | Soledad Moyano | `ROLE_DOCENTE` |
| **Estudiante** | `40012400` | `Prueba123` | Clara O'Farrell | `ROLE_ESTUDIANTE` |
| **Admin + Docente** | `30410732` | `Prueba123` | Martín Goñi | `ROLE_ADMIN`, `ROLE_DOCENTE` |
| **Docente + Estudiante** | `25601364` | `Prueba123` | Patricia Villalba | `ROLE_DOCENTE`, `ROLE_ESTUDIANTE` |
| **Triple rol** | `33154154` | `Prueba123` | Lucrecia Aramburu | `ROLE_ADMIN`, `ROLE_DOCENTE`, `ROLE_ESTUDIANTE` |
| **Usuario bloqueado** | `47039425` | `Prueba123` | Thiago Romero | `ROLE_ESTUDIANTE` (`account_non_locked = false`) |

---

## Documentación adicional

- **Manual de usuario por rol**: [`docs/manual-usuario.md`](docs/manual-usuario.md)
- **ADR 0001 — Monolito modular en el backend**: [`docs/adr/0001-monolito-modular.md`](docs/adr/0001-monolito-modular.md)
- **ADR 0002 — Estructura del frontend en Angular**: [`docs/adr/0002-estructura-frontend.md`](docs/adr/0002-estructura-frontend.md)
- **ADR 0003 — Despliegue en Azure con contenedores y CI/CD**: [`docs/adr/0003-despliegue-azure.md`](docs/adr/0003-despliegue-azure.md)
