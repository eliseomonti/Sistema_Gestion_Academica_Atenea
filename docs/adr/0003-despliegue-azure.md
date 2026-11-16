# ADR 0003: Despliegue en Azure con contenedores y CI/CD

- **Estado:** Aceptada
- **Fecha:** 2026-11-16

## Contexto

El sistema tiene que quedar publicado y accesible desde Internet (base de datos, backend y frontend)
como si se entregara a un cliente: con HTTPS, backups, sin secretos en el repositorio y con una forma
repetible de publicar cada cambio. El equipo es de tres personas, el presupuesto es cero y se cuenta
con la suscripción **Azure for Students** (crédito anual, sin tarjeta), que sólo permite crear
recursos en cinco regiones (`brazilsouth`, `canadacentral`, `chilecentral`, `newzealandnorth`,
`northcentralus`).

## Decisión

1. **Todo se empaqueta en contenedores.**
   - `backend/Dockerfile`: compila con Maven y corre sobre JRE 21.
   - `frontend/Dockerfile`: compila Angular y lo sirve con nginx, que además reenvía `/api` al backend.
   - `docker-compose.yml` levanta MySQL, backend y frontend para desarrollo local, y carga los scripts
     de `bdd/` la primera vez.
2. **La configuración entra por variables de entorno.** `application.properties` lee la conexión a la
   base con valores por defecto de desarrollo. El perfil `prod` quita los
   valores por defecto de `DB_PASSWORD` y `JWT_SECRET`: si faltan, la aplicación no arranca.
3. **El frontend llama a la API por la ruta relativa `/api`.** En producción la reenvía nginx y en
   desarrollo `ng serve` (con `proxy.conf.json`). Frontend y backend quedan en el mismo origen, así
   que el backend no necesita configuración de CORS, y la misma compilación de Angular sirve en
   cualquier entorno.
4. **Producción en servicios administrados de Azure**, todo en Chile Central:
   - Frontend y backend en **Azure Container Apps**. El backend tiene ingreso interno, así que sólo
     se llega a él a través del frontend. Azure provee HTTPS.
   - Base en **Azure Database for MySQL – Flexible Server** (B1ms, MySQL 8.4), con backups
     automáticos de 7 días y `time_zone` en `-03:00`.
   - Las credenciales viven como *secretos* de las Container Apps.
5. **Spring detrás de un proxy:** nginx envía `X-Forwarded-Host` y `X-Forwarded-Proto`, y el backend
   los usa (`server.forward-headers-strategy=framework`) para conocer el origen real del pedido.
6. **Imágenes en GitHub Container Registry (GHCR)**, etiquetadas con el hash corto del commit.
7. **CI/CD con GitHub Actions** (`.github/workflows/ci.yml`):
   - En cada PR: tests del backend contra MySQL, y lint, tests y build del frontend.
   - Al mergear a `main`: se repite la verificación, se publican las imágenes, se actualizan las
     Container Apps y se comprueba que la aplicación responda.
   - GitHub entra a Azure por **OIDC** con una identidad que sólo tiene permisos sobre `rg-atenea`.
   - `main` está protegida: sólo acepta PRs con la verificación en verde.

## Alternativas consideradas

| Alternativa | Veredicto |
|---|---|
| **VM gratuita de Oracle Cloud con Docker Compose y Caddy** | Viable y gratuita, pero para un cliente real es inaceptable: Oracle puede recuperar la VM si tiene poco uso. Se conserva como alternativa (`docker-compose.prod.yml`, `Caddyfile`) para una VM o VPS propia. |
| **VPS paga (Hetzner o similar) con Compose** | Lo más económico para un cliente chico, pero obliga a mantener el servidor y a armar los backups a mano. Mismo camino técnico que la alternativa anterior. |
| **PaaS con plan gratuito (Render, Railway, Koyeb)** | Descartadas: no ofrecen MySQL, los servicios se duermen (Spring tarda en volver a arrancar) y dan 512 MB de memoria. |
| **Vercel** | Sólo sirve para el frontend: no corre un servidor Java permanente ni ofrece MySQL. |
| **MySQL en un contenedor dentro de Azure** | Descartada: sin backups administrados y con problemas de rendimiento y bloqueo sobre el almacenamiento de red. |
| **Azure Container Registry** | Descartada: cuesta crédito. GHCR es gratuito y está integrado con GitHub Actions. |
| **Contraseña de Azure guardada en GitHub** | Descartada en favor de OIDC: no hay un secreto que pueda filtrarse ni vencerse. |
| **URL absoluta del backend en Angular, con CORS** | Descartada: una compilación distinta por entorno y CORS que mantener. |
| **Entorno de Container Apps en modo Express** | Descartado: no permite llamadas HTTP internas entre apps y rechaza imágenes del constructor clásico de Docker. Se usa el modo *Workload Profiles* con el perfil *Consumption*. |

## Consecuencias

**Positivas**

- Mergear a `main` publica el cambio en unos 10 minutos, sin pasos manuales, y sólo si pasa la
  verificación.
- Lo que corre en producción es exactamente un commit identificado. Volver atrás es activar una
  revisión anterior o apuntar a la imagen de otro commit.
- No hay secretos en el repositorio ni contraseñas de Azure en GitHub.
- La base tiene backups automáticos sin configuración adicional.
- Las mismas imágenes sirven para Azure, para una VM con Compose y para desarrollo local, así que
  cambiar de proveedor no obliga a rehacer el empaquetado.

**Negativas / costos**

- **Dependencia del crédito de estudiante:** cuando se agote o venza, las Container Apps se detienen.
- **El token de lectura de GHCR vence cada 90 días:** al vencer, los deploys fallan hasta renovarlo.
- **Las migraciones de la base son manuales:** el deploy no modifica el esquema, y un cambio de
  tablas se aplica en Azure antes de mergear. Si no, el backend no arranca (`ddl-auto=validate`).
- **La URL es la que genera Azure:** un nombre propio requiere comprar un dominio.
- Configurar el entorno requirió conocimientos específicos de Azure (regiones permitidas, modo del
  entorno, formato del *subject* de OIDC con los ID del repositorio).

## Cómo se verifica

- El workflow de CI/CD corre en cada PR y en cada merge a `main`. La regla de protección de `main`
  exige que la verificación pase.
- El último paso del deploy comprueba que la página responda 200 y que `/api/health` (backend y base)
  responda 200.

## Revisión

Esta decisión se reevalúa si se agota o vence el crédito de Azure (pasar a la alternativa con VM o
VPS y Compose), si los cambios de esquema se vuelven frecuentes (incorporar una herramienta de
migraciones como Flyway) o si se necesita un dominio propio.
