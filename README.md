# alquiler-eventos-ms-alquiler

Microservicio de **Ordens de Alquiler** del sistema de alquiler de equipos para
eventos (FIUNI - Sistemas Web y Distribuidos, Entrega 2).

Rol del usuario que atiende: **Encargado de Depósito / Admin**.

Se conecta a la misma base PostgreSQL remota (Supabase) que `ms-compras` y consume
el JAR compartido `alquiler-eventos-common` (entidades, repositorios y DTOs).

---

## Stack y tecnologías

| Tecnología | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.5 |
| Spring Data JPA + Hibernate | incluido (ddl-auto: validate) |
| PostgreSQL | driver (Supabase) |
| SpringDoc OpenAPI (Swagger UI) | 2.8.5 |
| Lombok | incluido |
| Maven | 3.9+ |

Dependencia principal:

```xml
<dependency>
    <groupId>com.alquilereventos</groupId>
    <artifactId>alquiler-eventos-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

> Requiere tener instalado el JAR de common en el repositorio local de Maven
> (`mvn clean install` dentro del repo `alquiler-eventos-common`).

---

## Estructura del proyecto

```
src/main/java/com/alquilereventos/alquiler/
├── MsAlquilerApplication.java        # Arranque Spring Boot (@EntityScan/@EnableJpaRepositories)
├── controller/
│   ├── ClienteController.java        # /api/clientes (6 endpoints)
│   └── OrdenAlquilerController.java  # /api/ordenes-alquiler (6 + cambio de estado)
├── service/
│   ├── ClienteService.java           # Interfaz
│   ├── ClienteServiceImpl.java
│   ├── OrdenAlquilerService.java     # Interfaz
│   └── OrdenAlquilerServiceImpl.java # Lógica de negocio (stock, totales, historial, mora)
├── mapper/
│   ├── Mapper.java                   # Interfaz genérica <E, D> (jerarquía de clases)
│   ├── ClienteMapper.java
│   └── OrdenAlquilerMapper.java
└── exception/
    ├── ApiExceptionHandler.java      # @RestControllerAdvice central
    ├── RecursoNoEncontradoException.java  # -> HTTP 404
    └── ReglaNegocioException.java         # -> HTTP 400

src/main/resources/
├── application.yml                  # Config 100% por variables de entorno
├── logback-spring.xml               # Logs a consola + archivo en disco
└── (JAR common aporta db/changelog y DTOs)
```

---

## Requisitos previos

- **JDK 21** (si Maven falla con *"release version 21 not supported"*, revisar `JAVA_HOME`).
- **Maven 3.9+**
- JAR `alquiler-eventos-common` instalado en `~/.m2` (ver arriba).
- Acceso a la base remota de Supabase (credenciales en variables de entorno).

## Cómo correr

Compilar y empaquetar:

```bash
mvn clean package -DskipTests
```

Ejecutar (con las variables de entorno ya definidas):

```bash
java -jar target/ms-alquiler-0.0.1-SNAPSHOT.jar
```

O directamente con Maven:

```bash
mvn spring-boot:run
```

La app arranca con Liquibase apagado en este microservicio (`enabled: false`);
Hibernate valida las entidades (`ddl-auto: validate`). La definición de tablas la
**gestiona Liquibase desde el JAR common** al arrancar la primera vez cualquiera
de las aplicaciones que lo consumen.

---

## Variables de entorno

Todo es configurable: nada de valores quemados en el código.

| Variable | Defecto | Descripción |
|---|---|---|
| `DB_URL` | *(obligatoria)* | JDBC URL de Supabase |
| `DB_USER` | *(obligatoria)* | Usuario de la base |
| `DB_PASSWORD` | *(obligatoria)* | Contraseña de la base |
| `SERVER_PORT` | `8081` | Puerto del microservicio |
| `API_CONTEXT_PATH` | `/api` | Prefijo de las rutas |
| `DEFAULT_PAGE_SIZE` | `10` | Registros por página por defecto |
| `MAX_PAGE_SIZE` | `100` | Tope de registros por página |
| `DEFAULT_PAGE` | `0` | Página inicial |
| `LOG_DIR` | `./logs` | Directorio de los logs en disco |
| `LOG_LEVEL_ROOT` | `INFO` | Nivel de log general |
| `LOG_LEVEL_CONTROLLER` | `INFO` | Nivel de log de la capa controller |
| `LOG_LEVEL_SERVICE` | `DEBUG` | Nivel de log de la capa service |
| `LOG_LEVEL_SQL` | `DEBUG` | Nivel de log del SQL de Hibernate |
| `MORA_PORCENTAJE_DIA` | `0.05` | % del total por día de atraso (mora) |

Ejemplo (PowerShell, Windows):

```powershell
$env:DB_URL="jdbc:postgresql://<host>:5432/postgres?sslmode=require"
$env:DB_USER="postgres"
$env:DB_PASSWORD="<password>"
$env:SERVER_PORT="8081"
```

---

## Swagger / OpenAPI

La documentación interactiva de todos los endpoints está en:

```
http://localhost:8081/api/swagger-ui.html
```

OpenAPI JSON: `http://localhost:8081/api/v3/api-docs`

Cada controller tiene `@Tag`, `@Operation`, `@Parameter` y `@ApiResponses` para
documentar nombres, descripciones y códigos HTTP tanto de `/clientes` como de
`/ordenes-alquiler`.

---

## Endpoints

### Clientes — `/api/clientes`

| Método | Ruta | Descripción | Códigos |
|---|---|---|---|
| POST | `/clientes` | Crea un cliente (activo por defecto) | 201 / 400 |
| PUT | `/clientes/{id}` | Actualiza un cliente | 200 / 404 |
| GET | `/clientes/{id}` | Obtiene por id | 200 / 404 |
| GET | `/clientes?page=&size=` | Lista paginada (solo activos) | 200 |
| GET | `/clientes/buscar?nombre=&apellido=&documento=&email=` | Busca con filtros + paginación | 200 |
| DELETE | `/clientes/{id}` | Borrado lógico (`activo = false`) | 200 / 404 |

```bash
curl -X POST http://localhost:8081/api/clientes \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Juan","apellido":"Perez","telefono":"555-1234","email":"juan@example.com","documento":"30222111","direccion":"Av. Siempre Viva 123"}'

curl "http://localhost:8081/api/clientes?page=0&size=10"
curl "http://localhost:8081/api/clientes/buscar?nombre=juan&page=0&size=10"
```

### Ordenes de alquiler — `/api/ordenes-alquiler`

| Método | Ruta | Descripción | Códigos |
|---|---|---|---|
| POST | `/ordenes-alquiler` | Crea la orden con detalles (estado inicial `RESERVA`) | 201 / 400 / 404 |
| PUT | `/ordenes-alquiler/{id}` | Actualiza (solo si está en `RESERVA`) | 200 / 400 / 404 |
| GET | `/ordenes-alquiler/{id}` | Obtiene por id con sus detalles | 200 / 404 |
| GET | `/ordenes-alquiler?estado=&clienteId=&fechaDesde=&fechaHasta=&page=&size=` | Lista/busca paginada | 200 |
| GET | `/ordenes-alquiler/buscar?estado=&clienteId=&fechaDesde=&fechaHasta=&page=&size=` | Igual que el listado con filtros | 200 |
| DELETE | `/ordenes-alquiler/{id}` | Botrado lógico `ANULADA` (solo en `RESERVA`) | 200 / 400 / 404 |
| PUT | `/ordenes-alquiler/{id}/estado` | Cambia estado (`ENTREGA`, `DEVOLUCION`, `MORA`, `ANULADA`) | 200 / 400 / 404 |

**Crear una orden** (el servidor calcula `subtotal = cantidad × precioDia × días`
y el `total`; el cliente NO los manda):

```bash
curl -X POST http://localhost:8081/api/ordenes-alquiler \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "usuarioId": 1,
    "fechaEvento": "2026-09-25",
    "fechaDevolucion": "2026-09-27",
    "detalles": [
      {"equipoId": 1, "cantidad": 2},
      {"equipoId": 3, "cantidad": 1}
    ]
  }'
```

**Cambiar el estado** de la orden `1` a `ENTREGA`:

```bash
curl -X PUT http://localhost:8081/api/ordenes-alquiler/1/estado \
  -H "Content-Type: application/json" \
  -d '{"usuarioId": 1, "estado": "ENTREGA"}'
```

**Buscar con filtros y paginación:**

```bash
curl "http://localhost:8081/api/ordenes-alquiler?estado=RESERVA&clienteId=1&fechaDesde=2026-09-01&fechaHasta=2026-09-30&page=0&size=10"
```

---

## Reglas de negocio (OrdenAlquilerService)

- **No alquilar más stock del disponible**: 400 si `cantidad > equipo.stock`.
- **Subtotal por detalle**: `cantidad × precioDia × días` (días =
  `fechaDevolucion - fechaEvento + 1`; mínimo 1).
- **Total**: suma de los subtotales (calculado por el servidor).
- **Cliente inactivo** (`activo = false`) no puede usarse en órdenes nuevas → 400.
- **Equipo no disponible** (`MANTENIMIENTO`/`BAJA`) no se puede alquilar → 400.
- **Modificar/anular** solo si la orden está en `RESERVA`.
- **Transiciones de estado válidas** (el resto → 400):
  - `RESERVA   -> ENTREGA`   (descuenta stock)
  - `RESERVA   -> ANULADA`   (borrado lógico)
  - `ENTREGA   -> DEVOLUCION`(devuelve stock)
  - `ENTREGA   -> MORA`      (solo si hay atraso; calcula `montoMora`)
  - `MORA      -> DEVOLUCION`(devuelve stock)
- **Mora**: `montoMora = díasDeAtraso × total × MORA_PORCENTAJE_DIA`
  (días de atraso = hoy - `fechaDevolucion`).
- **Todo cambio de estado** queda registrado en `OrdenAlquilerHistorial`
  (herencia de `HistorialEstado` del JAR common).

---

## Manejo de errores

`ApiExceptionHandler` (`@RestControllerAdvice`) centraliza las respuestas con el
DTO `ApiError` del JAR common (`codigo`, `mensaje`, `ruta`, `fecha`):

- `RecursoNoEncontradoException` → **404**
- `ReglaNegocioException` → **400**
- Cualquier error interno → **500** con mensaje genérico (jamás se expone el stacktrace).

---

## Logs

Con `logback-spring.xml` los logs van a:

- **Consola** (desarrollo).
- **Archivo** con rolling diario en `${LOG_DIR}/alquiler-eventos-ms.log`
  (`LOG_DIR` configurable, histórico de 30 días comprimido).

Niveles por capa (configurables con las variables `LOG_LEVEL_*` de la tabla):

- Controller: `INFO`
- Service: `DEBUG` (pasos clave de negocio, cambios de estado, stock)
- Hibernate SQL: `DEBUG`

---

## Workflow git

- Repos de la organización `alquiler-eventos-org`.
- `ms-alquiler` es de autonomía total del encargado de alquiler (commits directos).
- `common` es compartido: **rama `feature/*` + Pull Request + review** del otro
  integrante antes del merge.
- Nunca commitear: `.env`, `target/`, `logs/`.

---

## Checklist de la rúbrica (Sección 7 de la guía)

- [x] Entidad simple con CRUD completo: **Cliente**
- [x] Entidad M:N gestionando la asociación: **OrdenAlquiler <-> Equipo** vía `OrdenAlquilerDetalle`
- [x] Capas estrictas Controller -> Service -> Repository, sin saltos
- [x] Jerarquía de clases: `Mapper<E, D>` genérico + herencia `HistorialEstado` del JAR
- [x] Entities y DTOs del JAR usados sin modificar
- [x] Mapeo bidireccional DTO <-> Entity
- [x] POST 201 / PUT 200 / GET 200-404 / DELETE lógico
- [x] Listado con paginación obligatoria
- [x] Búsqueda con filtros + paginación
- [x] Maven resuelve el JAR common
- [x] Todo configurable (application.yml + variables de entorno)
- [x] Manejo central de excepciones (`@RestControllerAdvice` + `ApiError`)
- [x] Logs en disco por capa (SLF4J/Logback)
- [x] Swagger documentando todos los endpoints
- [x] Conexión a la BD remota compartida (Supabase)
- [x] README con instrucciones de corrida