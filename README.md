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

```
src/main/java/com/alejandro/mtoexplorer/
├── config/   fase 1 · configuración tipada (mto.*)
├── kp/       fase 1 · puntos kilométricos (17858.5 ↔ 17+858.5)
├── entity/   fase 1 · entidades Panache reactivas
├── load/     fase 1 · lectura de Excel y carga
├── api/      fase 1 · recursos REST y DTOs
├── grpc/     fase 3 · servicios gRPC (los .proto van en src/main/proto)
├── jobs/     fase 4 · tareas programadas
└── ai/       fase 5 · asistente LangChain4j
src/main/resources/db/migration/   migraciones Flyway
docs/maestros.md                   hojas y columnas de los maestros
```

## Imagen nativa

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Los `Dockerfile` de `src/main/docker` los generó code.quarkus.io.
