# mto-explorer

Explorador de los maestros de infraestructura de MTO (`lov-master.xlsx` y `profile-master.xlsx`),
hecho con **Quarkus** para aprenderlo con datos reales.

**No depende del ecosistema MTO**: no habla con `mto-configuration`, ni con RabbitMQ, ni con
Keycloak. Lee los mismos Excel que genera `mto-configuration/data/tools` y los sirve para
consultarlos.

## Requisitos

- Java 21 o superior.
- Docker arrancado: Dev Services levanta PostgreSQL y Redis en desarrollo y en los tests.
- Nada más: Maven viene con el wrapper (`./mvnw`).

## Arrancar

```bash
MTO_DATA_DIR=/ruta/a/mto-configuration/data ./mvnw quarkus:dev
```

- Dev UI: <http://localhost:8090/q/dev-ui>
- Swagger UI: <http://localhost:8090/q/swagger-ui>
- Salud: <http://localhost:8090/q/health>

Tests: `./mvnw verify`.

## Los datos no van al repositorio

Los maestros son de ingeniería. El `.gitignore` excluye cualquier `.xlsx`, y la aplicación los lee
de la carpeta de `MTO_DATA_DIR`. Los tests usan unos maestros pequeños e inventados
(`src/test/java/.../load/MasterFixtures.java`).

## Plan

Cada fase deja algo funcionando y con tests. La guía de cada una, paso a paso, está en `docs/`.

| Fase | Qué | Qué se aprende | Guía |
|---|---|---|---|
| 1 | Cargar los Excel en PostgreSQL y servirlos por REST | Dev Services, `@ConfigMapping`, Flyway, Panache reactivo, Mutiny (`Uni`), REST, validación, OpenAPI, `@QuarkusTest` | [fase-1.md](docs/fase-1.md) |
| 2 | Caché en Redis y streaming con `Multi` | `@CacheResult`, invalidación, `Multi`, SSE | pendiente |
| 3 | Servicio gRPC (unario y streaming) y cliente | `.proto`, `@GrpcService`, stubs Mutiny, `grpcurl` | pendiente |
| 4 | Recargar cuando cambia un Excel; foto diaria de calidad | `@Scheduled`, cron, ejecución concurrente | pendiente |
| 5 | Asistente para preguntar por los maestros en lenguaje natural | LangChain4j, `@RegisterAiService`, `@Tool`, Ollama en local | pendiente |

Las dependencias de todas las fases ya están en el `pom.xml`. LangChain4j está apagado en
`application.yml` hasta la fase 5.

## Estructura

Por capas: cada una solo conoce la de debajo.

```
            HTTP
             │
  api/            recursos REST: leen y validan parámetros, llaman a un servicio, devuelven DTO
  ├── dto/        el contrato JSON (records)
  ├── mapper/     entidad / modelo → DTO (formato: PK "17+858.5", tangente "1:9"…)
  └── error/      excepciones → application/problem+json (RFC 9457)
             │
  service/        lógica de negocio; abren la sesión reactiva (@WithSession)
  ├── model/      lo que devuelven (Page, ProfileWithCantilevers, EpQuality…)
  └── exception/  excepciones de negocio (ResourceNotFoundException)
             │
  repository/     consultas, un repositorio Panache por entidad
  ├── criteria/   filtros de búsqueda (ProfileSearch, LovSearch)
  ├── projection/ resultados de agregados (EpCount, LovFamilyCount)
  └── support/    QueryFilter: el WHERE con filtros opcionales y sus parámetros
             │
  entity/         las tablas (BaseEntity + 11 entidades, sin lógica)

  imports/        la importación de los maestros
  ├── excel/      abrir un .xlsx en streaming y leer filas por nombre de columna
  ├── reader/     hojas → entidades (código puro, sin base de datos)
  ├── store/      sustituir un maestro en la base de datos, en una transacción
  └── MasterImportService   qué importar y cuándo; puente bloqueante → reactivo

  config/  configuración tipada (mto.*)      kp/    puntos kilométricos
  grpc/    fase 3                            jobs/  fase 4          ai/  fase 5
```

`src/main/resources/db/migration/` tiene las migraciones Flyway, y `docs/maestros.md` las hojas y
columnas de los maestros.

### Dónde hay `Multi`

- `GET /api/profiles/stream`: los perfiles en streaming (NDJSON). `ProfileService.stream` pide
  páginas con `Multi.createBy().repeating()` hasta la primera vacía y emite sus perfiles uno a uno.
- `GET /api/eps/overview`: `ExecutionPackageService.overview` pasa la lista de paquetes a un `Multi`,
  lanza los recuentos de cada uno con `transformToUniAndConcatenate` (uno detrás de otro: una sesión
  reactiva no admite dos consultas a la vez) y lo junta de nuevo con `collect().asList()`.

### Tests

| Nivel | Ejemplos | Qué necesita |
|---|---|---|
| Unitario | `KpTest`, `MasterWorkbookTest`, `*MasterReaderTest` | Nada: JUnit a secas |
| Servicio con mocks | `QualityServiceTest` | Mockito; sin Quarkus ni base de datos |
| Repositorio | `ProfileRepositoryTest` | `@QuarkusTest` + `@RunOnVertxContext` + `UniAsserter` |
| Importación | `MasterImportServiceTest`, `MissingDataDirTest` | `@QuarkusTest`, sin HTTP |
| API | `ExplorerApiTest`, `SectioningApiTest`, `ProblemDetailsTest`, `ProfileStreamTest` | `@QuarkusTest` + RestAssured |

## Imagen nativa

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Los `Dockerfile` de `src/main/docker` los generó code.quarkus.io.
