# Fase 3: un servicio gRPC (unario, streaming y bidireccional) y su cliente

**Objetivo:** los mismos datos que la API REST, ahora por gRPC: la ficha de un paquete y de un perfil
(unarias), los perfiles de una búsqueda en streaming, una consulta bidireccional (el cliente manda
claves y recibe un perfil por cada una) y las importaciones en directo, como el SSE.

**Qué aprendes:** el contrato `.proto` y las clases que Quarkus genera, `@GrpcService` con Mutiny,
los cuatro tipos de llamada, `optional` y `oneof` en proto3, los errores como `Status` con un
`ExceptionHandlerProvider`, un `@GlobalInterceptor`, `@GrpcClient` en los tests, y `grpcurl` con el
servicio de reflexión.

El código de esta fase está en `master`. Esta guía explica el porqué de cada pieza y las trampas que
salieron al probarla.

---

## Paso 1. El contrato — `src/main/proto/infrastructure.proto`

Quarkus compila todo lo que hay en `src/main/proto` en `./mvnw compile` (sin plugins) y deja en
`target/generated-sources/grpc` los mensajes, el stub de grpc-java y la interfaz de Mutiny
`Infrastructure`, que es la que se implementa.

| rpc | Tipo | Qué hace |
|---|---|---|
| `GetExecutionPackage` | unaria | un paquete con sus recuentos, de la caché de `ep-overview` |
| `GetProfile` | unaria | la ficha de un perfil con sus ménsulas y sus seccionadores |
| `StreamProfiles` | streaming del servidor | los perfiles de una búsqueda, según llegan de la base de datos |
| `LookupProfiles` | bidireccional | una respuesta por cada clave que manda el cliente, en el mismo orden |
| `WatchImports` | streaming del servidor, sin fin | un aviso por fichero importado |

- **Los números de campo (`= 1`, `= 2`…) son el contrato**, no los nombres: lo que viaja es el
  número. Un número publicado no se cambia ni se reutiliza; si un campo sobra, se marca `reserved`.
- `package mto.explorer.v1`: el `v1` permite sacar un `v2` sin romper a quien usa el `v1`.
- Los mensajes se llaman `…Info` para no chocar con las entidades (`Profile`, `ExecutionPackage`) en
  los `import`.
- En el enum, el valor 0 es `IMPORT_STATUS_UNSPECIFIED`: es el que tiene un campo que no se ha
  puesto, y no debe significar nada.

**Hecho cuando:** tras `./mvnw compile` existe `target/generated-sources/grpc/.../grpc/v1/Infrastructure.java`.
En IntelliJ, si no la ve: botón derecho en la carpeta → *Mark Directory as → Generated Sources Root*.

📖 <https://quarkus.io/guides/grpc-getting-started> · <https://protobuf.dev/programming-guides/proto3/>

## Paso 2. Null, `optional` y `oneof`

En proto3 un campo normal **no puede faltar**: un `double` que no se manda es `0`, y un `string`, `""`.
Con `optional` se distingue "no viene" (`hasKp()` es `false`) de "vale 0". Por eso todos los campos
que en la base de datos pueden ser null son `optional`.

> ⚠️ **Un setter de protobuf no admite null.** `builder.setKp(null)` lanza `NullPointerException`.
> `ProtoMapper.ifPresent(valor, builder::setX)` solo pone el campo si hay valor.

`ProfileLookup` usa `oneof result { ProfileInfo profile; string error; }`: cada respuesta lleva una
de las dos cosas, y el cliente pregunta cuál con `getResultCase()`.

## Paso 3. Configuración — `application.yml`

```yaml
quarkus:
  grpc:
    server:
      use-separate-server: false       # gRPC en el 8090, con HTTP, en vez de abrir el 9000
      enable-reflection-service: true  # para grpcurl y la Dev UI
    clients:
      infrastructure:                  # @GrpcClient("infrastructure")
        host: localhost
        port: 8090
```

gRPC va sobre HTTP/2, y el servidor HTTP de Quarkus (Vert.x) lo entiende, así que REST y gRPC
comparten puerto. En los tests HTTP arranca en un puerto aleatorio (`test-port: 0`) y el cliente lo
encuentra solo: Quarkus lo dice en el log (`LaunchMode TEST detected. Overriding existing port
configuration for gRPC client`).

## Paso 4. El servicio — `grpc/InfrastructureGrpcService.java`

`@GrpcService` + `implements Infrastructure` (la interfaz de Mutiny). Cada rpc es un método que
devuelve un `Uni` (una respuesta) o un `Multi` (un stream); el bidireccional además **recibe** un
`Multi`.

Es la misma capa que un recurso REST: traduce la petición, llama a un servicio y convierte el
resultado. **Los servicios no cambian nada para gRPC**: `ProfileService.get`, `ProfileService.stream`
e `ImportFeed.events()` son los mismos que usa REST. El único método nuevo es
`ExecutionPackageService.overview(ep)`, que filtra el resumen cacheado.

- **`Uni.createFrom().deferred(...)` / `Multi.createFrom().deferred(...)`**: la validación de la
  petición corre al suscribirse, dentro del `Uni` o el `Multi`. Así su fallo viaja por el flujo y lo
  convierte `GrpcExceptionMapper`, en vez de salir como una excepción del propio método.
- **Campos obligatorios.** En proto3 un string que no se manda llega como `""`, nunca como null.
  `requireNotBlank` lanza `Status.INVALID_ARGUMENT…asRuntimeException()`, lo que en REST hace
  `@NotBlank`.
- **Bidireccional** con `transformToUniAndConcatenate`: una consulta detrás de otra (una sesión
  reactiva no admite dos a la vez) y las respuestas en el orden de las claves. Una clave que no existe
  no corta el stream: `onFailure(ResourceNotFoundException.class).recoverWithItem(...)` responde con
  `error` y sigue.
- **`WatchImports`** es el mismo `Multi` caliente que el SSE. No termina nunca: acaba cuando el
  cliente cancela.
- **`overview(ep)` llama a `overview()` del mismo bean, y la caché funciona.** Quarkus intercepta con
  subclases generadas, así que también intercepta las llamadas de un bean a sí mismo. En Spring, esa
  llamada se saltaría la caché.

Los métodos de Mutiny corren en el event loop con un contexto de Vert.x, así que `@WithSession`
funciona igual que en REST.

📖 <https://quarkus.io/guides/grpc-service-implementation>

## Paso 5. Los errores — `GrpcExceptionMapper` y `GrpcExceptionHandler`

El `ProblemMappers` de gRPC. Por defecto Quarkus convierte en `UNKNOWN` todo lo que no sea un
`StatusException`; un bean que implementa `ExceptionHandlerProvider` sustituye al de Quarkus.

| Excepción | Status | En REST |
|---|---|---|
| `ResourceNotFoundException` | `NOT_FOUND` | 404 |
| `InvalidKpException` | `INVALID_ARGUMENT` | 400 |
| `StatusException` / `StatusRuntimeException` | el suyo | |
| cualquier otra | `INTERNAL`, sin detalle, y al log | 500 |

Tiene dos entradas:
- **`transform(Throwable)`**: los fallos de los `Uni` y `Multi` del servicio. Es la que se usa casi
  siempre.
- **`createHandler(...)`**: devuelve un `GrpcExceptionHandler`, que cierra la llamada cuando la
  excepción salta fuera de ellos (al recibir el mensaje, en un interceptor…). Si la llamada ya estaba
  cerrada, `close` lanza `IllegalStateException`: se ignora, porque ya no hay a quién avisar.

📖 <https://quarkus.io/guides/grpc-service-implementation#exception-handling>

## Paso 6. Un interceptor — `grpc/GrpcLoggingInterceptor.java`

Un `ServerInterceptor` envuelve cada llamada antes de que llegue al servicio, como un filtro HTTP.
Con `@GlobalInterceptor` se aplica a todos los servicios (sin ella, a cada uno con
`@RegisterInterceptor`). Escribe en el log una línea por llamada:

```
gRPC mto.explorer.v1.Infrastructure/StreamProfiles: OK, 617 mensajes en 199 ms
gRPC mto.explorer.v1.Infrastructure/WatchImports: cancelada por el cliente tras 15008 ms
```

Se envuelven las dos direcciones de la llamada:
- **`ServerCall`**, lo que el servidor envía: `sendMessage` (para contar) y `close` (estado y tiempo).
- **`ServerCall.Listener`**, lo que llega del cliente: `onCancel`. Cuando el cliente corta un stream,
  el servidor no llama a `close`.

## Paso 7. Probarlo a mano

- **Dev UI** (<http://localhost:8090/q/dev-ui>): la tarjeta gRPC tiene un formulario para cada método.
- **grpcurl** (`brew install grpcurl`, o
  `go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest`), en `-plaintext` (sin TLS):

```bash
grpcurl -plaintext localhost:8090 list
grpcurl -plaintext localhost:8090 describe mto.explorer.v1.Infrastructure

grpcurl -plaintext -d '{"code":"EP11A"}' localhost:8090 mto.explorer.v1.Infrastructure/GetExecutionPackage
grpcurl -plaintext -d '{"ep":"EP11A","track":"TRACK 1","order":3}' localhost:8090 mto.explorer.v1.Infrastructure/GetProfile
grpcurl -plaintext -d '{"ep":"EP11A","track":"TRACK 1","kpFrom":"17+900","kpTo":"18+100"}' \
  localhost:8090 mto.explorer.v1.Infrastructure/StreamProfiles

# bidireccional: varios mensajes seguidos
grpcurl -plaintext -d '{"ep":"EP11A","track":"TRACK 1","order":1} {"ep":"EP11A","track":"TRACK 1","order":9999}' \
  localhost:8090 mto.explorer.v1.Infrastructure/LookupProfiles

# en directo: esto en una terminal, y en otra  curl -X POST localhost:8090/api/loads
grpcurl -plaintext -d '{}' localhost:8090 mto.explorer.v1.Infrastructure/WatchImports
```

> ⚠️ **`"tracks": "6"` sale entre comillas.** No es un fallo: la conversión estándar de protobuf a
> JSON escribe los `int64` como texto, porque JavaScript no puede representar todos los enteros de 64
> bits. `duration_ms` igual.

## Paso 8. Tests

| Test | Qué demuestra | La técnica |
|---|---|---|
| `InfrastructureGrpcServiceTest` | Las cinco llamadas por la red, sus `NOT_FOUND` e `INVALID_ARGUMENT`, que una clave que no existe no corta el bidireccional y que `WatchImports` avisa de una importación | `@GrpcClient("infrastructure")` inyecta el cliente de Mutiny; `AssertSubscriber` para el stream sin fin |
| `GrpcExceptionMapperTest` | Qué `Status` sale de cada excepción, y que un `INTERNAL` no enseña el detalle | JUnit a secas |
| `ProtoMapperTest` | Que un null no se pone, que un 0 sí (y se distingue de "no viene"), los `optional` de la búsqueda, los nanosegundos del `Timestamp`, un valor del enum por estado | JUnit a secas |

El cliente recibe los errores como `StatusRuntimeException`, con `getStatus().getCode()`.

> ⚠️ **`WatchImports` no tiene un "conectado".** En el SSE hay un evento `connected`; aquí el
> cliente no sabe cuándo se ha suscrito el servidor al canal, y lo que se importe antes se pierde. El
> test importa y espera hasta un segundo, y repite si no ha llegado nada (casi siempre llega a la
> primera).

---

## Fase 3 terminada cuando
- `./mvnw verify` pasa (96 tests).
- `grpcurl -plaintext localhost:8090 list` enseña `mto.explorer.v1.Infrastructure`.
- `WatchImports` muestra las dos importaciones al hacer `POST /api/loads`.

Siguiente: **fase 4**, el Scheduler: recargar cuando cambia un Excel y una foto diaria de calidad.
