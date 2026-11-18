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
(`src/test/java/.../imports/MasterFixtures.java`).

## Plan

Cada fase deja algo funcionando y con tests. La guía de cada una, paso a paso, está en `docs/`.

| Fase | Qué | Qué se aprende | Guía |
|---|---|---|---|
| 1 | Cargar los Excel en PostgreSQL y servirlos por REST | Dev Services, `@ConfigMapping`, Flyway, Panache reactivo, Mutiny (`Uni`), REST, validación, OpenAPI, `@QuarkusTest` | [fase-1.md](docs/fase-1.md) |
| 2 | Caché en Redis y avisos en directo de las importaciones | `@CacheResult`, invalidación con eventos CDI, `Multi` caliente, SSE | [fase-2.md](docs/fase-2.md) |
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
  ├── model/      lo que devuelven (Page, ProfileDetail, EpQuality…)
  ├── cache/      caché en Redis: nombres, de qué maestro depende cada una y cuándo se vacían
  └── exception/  excepciones de negocio (ResourceNotFoundException)
             │
  repository/     consultas, un repositorio Panache por entidad
  ├── criteria/   filtros de búsqueda (ProfileSearch, LovSearch)
  ├── projection/ resultados de agregados (EpCount, LovFamilyCount)
  └── support/    QueryFilter: el WHERE con filtros opcionales y sus parámetros
             │
  entity/         las tablas (BaseEntity + 11 entidades con sus relaciones, sin lógica)

  imports/        la importación de los maestros
  ├── excel/      abrir un .xlsx en streaming y leer filas por nombre de columna
  ├── reader/     hojas → entidades (código puro, sin base de datos)
  ├── store/      sustituir un maestro en la base de datos, en una transacción
  ├── event/      el evento MastersImported y el canal en directo (ImportFeed) para el SSE
  └── MasterImportService   qué importar y cuándo; puente bloqueante → reactivo

  config/  configuración tipada (mto.*)      kp/    puntos kilométricos
  grpc/    fase 3                            jobs/  fase 4          ai/  fase 5
```

`src/main/resources/db/migration/` tiene las migraciones Flyway, y `docs/maestros.md` las hojas y
columnas de los maestros.

### Relaciones

```
execution_package ─┬─< station ─┐
                   │            ├─< track_station >─ track      @ManyToMany ordenada (@OrderColumn)
                   └─< track ───┘
track ─< profile ─< cantilever                                  @ManyToOne / @OneToMany
profile ─< disconnector (su poste, opcional)
station ─< section_insulator ─< section_insulator_switch
```

Todas son `LAZY`, y en Hibernate Reactive una relación `LAZY` **no se carga sola al tocarla**:
leer `profile.track.name` sin haberla cargado falla con `LazyInitializationException`. Hay dos formas
de cargarlas, y el proyecto usa las dos:

- **`join fetch` en la consulta**, cuando se sabe de antemano qué hace falta: los listados
  (`ProfileRepository.search`, `TrackRepository.listByEp`, `SectionInsulatorRepository.search`…).
- **`Mutiny.fetch(...)`**, para cargar una colección de una entidad ya leída:
  `ProfileService.get` pide así las ménsulas y los seccionadores del perfil.

`ProfileMasterReader` enlaza los objetos al leer el Excel (cada hijo apunta al objeto de su padre) y
Hibernate escribe las claves foráneas al guardar. Un hijo cuyo padre no existe hace fallar la
importación con la fila y la clave. El poste de un seccionador se resuelve con `PoleResolver`
(PROFILE_ID, después KP_POSTE, después la vía), las mismas reglas que `mto-configuration`.

### Caché en Redis

Se cachean con `@CacheResult` los agregados que recorren tablas enteras y solo cambian al importar:

| Caché | Método | Clave | Se vacía al importar |
|---|---|---|---|
| `ep-overview` | `ExecutionPackageService.overview()` | (ninguna) | PROFILE |
| `quality` | `QualityService.perExecutionPackage()` | (ninguna) | LOV y PROFILE |
| `quality-by-ep` | `QualityService.ofExecutionPackage(ep)` | el paquete | LOV y PROFILE |
| `lov-families` | `LovService.families()` | (ninguna) | LOV |

- **Solo datos planos** (records sin entidades): Redis guarda JSON, y una entidad de Hibernate con
  relaciones `LAZY` no se puede convertir a JSON y volver.
- **Invalidación por eventos.** `MasterImportService` publica el evento CDI `MastersImported` y
  `CacheInvalidator` lo escucha (`@Observes`) y vacía solo las cachés de los maestros que se han
  cargado. Al arrancar se vacían todas: Redis sobrevive a un reinicio y la base de datos ha podido
  cambiar mientras tanto.
- **`prefix: mto-explorer:{cache-name}`** en `application.yml`. Sin `{cache-name}` todas las cachés
  comparten claves en Redis, y una lee los datos de otra.

### Importaciones en directo (SSE)

`GET /api/loads/events` deja la conexión abierta y manda un evento `import` por cada fichero
importado, venga de donde venga la importación (el arranque, `POST /api/loads` o, en la fase 4, el
Scheduler). Además manda `connected` al conectar y un comentario `: ping` cada 25 s.

```bash
curl -N localhost:8090/api/loads/events      # en una terminal
curl -X POST localhost:8090/api/loads         # en otra
```

### Dónde hay `Multi`

- `GET /api/profiles/stream`: los perfiles en streaming (NDJSON). `ProfileService.stream` pide
  páginas con `Multi.createBy().repeating()` hasta la primera vacía y emite sus perfiles uno a uno.
- `GET /api/eps/overview`: `ExecutionPackageService.overview` pasa la lista de paquetes a un `Multi`,
  lanza los recuentos de cada uno con `transformToUniAndConcatenate` (uno detrás de otro: una sesión
  reactiva no admite dos consultas a la vez) y lo junta de nuevo con `collect().asList()`.
- `GET /api/loads/events`: `ImportFeed` es un `Multi` **caliente** (`BroadcastProcessor`): emite
  aunque no haya nadie escuchando, y cada cliente recibe lo que pasa mientras está conectado.
  `LoadResource.events` lo junta con el evento `connected` y el latido con
  `Multi.createBy().merging()`. Los dos anteriores son **fríos**: cada cliente recibe todo desde el
  principio.

### Tests

| Nivel | Ejemplos | Qué necesita |
|---|---|---|
| Unitario | `KpTest`, `MasterWorkbookTest`, `*MasterReaderTest` | Nada: JUnit a secas |
| Servicio con mocks | `QualityServiceTest` | Mockito; sin Quarkus ni base de datos |
| Repositorio | `ProfileRepositoryTest` | `@QuarkusTest` + `@RunOnVertxContext` + `UniAsserter` |
| Importación | `MasterImportServiceTest`, `MissingDataDirTest` | `@QuarkusTest`, sin HTTP |
| Caché | `CacheBehaviourTest` (y `CacheNamesTest`, unitario) | `@QuarkusTest` + Redis; cambia la base de datos por debajo para saber si la respuesta sale de Redis |
| Multi caliente | `ImportFeedTest` | `AssertSubscriber` de Mutiny |
| API | `ExplorerApiTest`, `SectioningApiTest`, `ProblemDetailsTest`, `ProfileStreamTest` | `@QuarkusTest` + RestAssured |
| SSE | `ImportEventsSseTest` | `HttpClient` de Java leyendo el stream línea a línea |

## Imagen nativa

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Los `Dockerfile` de `src/main/docker` los generó code.quarkus.io.
