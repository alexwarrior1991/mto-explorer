# Fase 2: caché en Redis y avisos en directo de las importaciones

**Objetivo:** las consultas que recorren tablas enteras (el resumen de paquetes, la calidad, las
familias de LOVs) se sirven desde Redis y se recalculan solo cuando se importa el maestro del que
dependen. Y cualquiera puede quedarse escuchando `GET /api/loads/events` para enterarse en directo de
cada importación.

**Qué aprendes:** `@CacheResult` con métodos reactivos, cachés con clave, la caché en Redis y su
configuración, invalidación programática con `CacheManager`, eventos CDI (`Event.fire` y
`@Observes`, con orden), un `Multi` **caliente** con `BroadcastProcessor`, Server-Sent Events con
eventos con nombre, `Multi.createBy().merging()`, `Multi.createFrom().ticks()` y cómo probar todo eso.

El código de esta fase está en `master`. Esta guía explica el porqué de cada pieza y las trampas que
salieron al probarla; úsala para repasar o para rehacerlo a mano.

---

## Paso 1. Qué cachear (y qué no)

Una caché solo compensa si la consulta es cara y su resultado cambia poco. Aquí los datos **solo
cambian al importar**, así que todo lo que se lee podría cachearse… pero no todo debe:

| Sí | No |
|---|---|
| Agregados que recorren tablas enteras: `overview`, `quality`, `lov-families` | Listados y fichas de entidades: son consultas por índice que PostgreSQL resuelve en milisegundos |
| Resultados en **datos planos** (records) | **Entidades**: Redis guarda JSON, y una entidad con relaciones `LAZY` y proxies de Hibernate no se puede convertir a JSON y volver |

Por eso `EpOverview` deja de llevar la entidad `ExecutionPackage` y pasa a ser
`record EpOverview(String code, String name, boolean enabled, long tracks, long profiles)`.

**Hecho cuando:** sabes decir, para cada método de servicio, si se cachea y por qué.

📖 <https://quarkus.io/guides/cache>

## Paso 2. Configurar Redis — `application.yml`

```yaml
quarkus:
  cache:
    type: redis
    redis:
      prefix: mto-explorer:{cache-name}
      expire-after-write: 24H
```

- `type: redis`: las cachés de `@CacheResult` van a Redis. Dev Services lo levanta en desarrollo y en
  los tests, como PostgreSQL.
- `expire-after-write`: una red de seguridad. Lo normal es que se vacíen al importar (paso 5); esto
  solo evita que una entrada viva para siempre si algo cambiara la base de datos por otro camino.

> ⚠️ **La trampa del prefijo.** `prefix: mto-explorer` a secas **sustituye** el prefijo por defecto,
> que es el que lleva el nombre de la caché. Entonces todas las cachés comparten claves en Redis
> (las que no tienen parámetros, la misma: `mto-explorer:default-cache-key`) y una lee los datos de
> otra: salían respuestas con todos los campos a `null`. El prefijo tiene que llevar `{cache-name}`.

**Hecho cuando:** después de llamar a un endpoint cacheado, en la Dev UI (Redis) o con
`redis-cli --scan --pattern 'mto-explorer*'` ves claves como `mto-explorer:quality:default-cache-key`.

📖 <https://quarkus.io/guides/cache-redis-reference>

## Paso 3. Los nombres de las cachés — `service/cache/CacheNames.java`

Una constante por caché y un mapa con **de qué maestro depende cada una**:

| Caché | Depende de |
|---|---|
| `ep-overview` | PROFILE |
| `quality`, `quality-by-ep` | LOV y PROFILE (cuenta perfiles y ménsulas, y los no reconocidos de los dos maestros) |
| `lov-families` | LOV |

Pista: tener los nombres en constantes evita que un `@CacheResult(cacheName = "qualty")` cree en
silencio una caché nueva que nadie vacía.

## Paso 4. Cachear los métodos — `@CacheResult`

```java
@CacheResult(cacheName = CacheNames.QUALITY)
@WithSession
public Uni<List<EpQuality>> perExecutionPackage() { ... }
```

- Con un método que devuelve `Uni`, Quarkus guarda **el valor que emite**, no el `Uni`. Y **no guarda
  los fallos**: un 404 no se queda en la caché.
- La extensión de Redis deduce el tipo del valor de lo que devuelve el método, también los genéricos
  (`Uni<List<EpQuality>>`). Por eso no hay que configurar `value-type`. A cambio, **una caché no se
  puede usar en dos métodos que devuelvan tipos distintos**: el build falla.
- **Clave**: sin parámetros hay una sola entrada. `ofExecutionPackage(String ep)` tiene una por paquete,
  porque el parámetro es la clave. Con varios parámetros, la clave son todos juntos, o solo los
  marcados con `@CacheKey`.

**Hecho cuando:** la segunda llamada a `/api/quality` tarda una fracción de la primera. Con los
maestros reales, de ~250 ms a ~15 ms.

## Paso 5. Vaciar al importar — eventos CDI

El importador no debería saber que existe una caché (ni un canal SSE, ni lo que venga en la fase 4).
Así que **avisa** y quien lo necesite escucha:

1. `FileImportResult` lleva el maestro (`LOV` o `PROFILE`) de cada fichero.
2. `MasterImportService` inyecta `Event<MastersImported>` y, al terminar, hace
   `imported.fire(new MastersImported(Instant.now(), results))`.
3. `CacheInvalidator` lo escucha con `void onImport(@Observes MastersImported event)` y vacía, con
   `CacheManager`, solo las cachés de los maestros con estado `LOADED`. Un `UNCHANGED` no cambia nada.

Por qué `CacheManager` y no `@CacheInvalidateAll`: qué cachés se vacían depende del evento, y una
anotación es fija.

**Al arrancar se vacía todo.** Redis no se borra al parar la aplicación, y mientras estaba parada la
base de datos ha podido cambiar (otra versión, otras migraciones…). `CacheInvalidator.onStart` observa
`StartupEvent` con `@Priority(1)`: un número más bajo se ejecuta antes, así que vacía la caché antes de
que `StartupImport` importe.

Pista: `invalidateAll()` devuelve un `Uni`, y aquí se espera con `await().indefinitely()`. Se puede,
porque los dos observadores se ejecutan en hilos que pueden bloquearse: el de arranque y el del
importador. En el event loop no se puede bloquear nunca.

**Hecho cuando:** en el log ves `Cachés vaciadas: [...]` al arrancar y después de cada importación que
carga algo; tras importar, `redis-cli --scan` no encuentra claves.

📖 <https://quarkus.io/guides/cdi-reference>

## Paso 6. Un `Multi` caliente — `imports/event/ImportFeed.java`

Hasta ahora todos los `Multi` del proyecto eran **fríos**: cada suscriptor recibe todo desde el
principio (el streaming de perfiles hace sus consultas para cada cliente). Este es **caliente**: los
eventos pasan aunque no haya nadie escuchando, y cada suscriptor recibe los que ocurren mientras está
suscrito.

- Un `BroadcastProcessor<ImportEvent>` es un `Multi` en el que se emite a mano y que reparte cada
  elemento a todos sus suscriptores.
- Se emite por `processor.serialized()`, un `SerializedProcessor` que ordena las emisiones si llegan
  desde varios hilos a la vez. Los clientes se suscriben al procesador, no al serializado
  (`serialized()` devuelve un `Processor`, no un `Multi`).
- `onOverflow().drop()`: si un cliente lee más despacio de lo que llegan los eventos, se le descartan
  los que no da abasto a leer, en lugar de acumularlos en memoria.

**Hecho cuando:** `ImportFeedTest` pasa: dos suscriptores reciben los mismos eventos, y uno que se
suscribe tarde no recibe lo de antes.

📖 <https://smallrye.io/smallrye-mutiny/latest/guides/hot-streams/>

## Paso 7. Server-Sent Events — `LoadResource.events`

Un endpoint que devuelve un `Multi` con `@Produces(MediaType.SERVER_SENT_EVENTS)` ya es SSE. Aquí se
devuelven `OutboundSseEvent`, construidos con `@Context Sse`, para poder **ponerles nombre**: en el
navegador se escuchan por separado con `eventSource.addEventListener("import", ...)`.

Son tres flujos juntos con `Multi.createBy().merging().streams(imports, connected, heartbeat)`, que se
suscribe a los tres a la vez:

- **`import`**: un evento por fichero importado, del `ImportFeed`.
- **`connected`**: uno nada más conectar. ⚠️ **Quarkus no envía las cabeceras HTTP hasta el primer
  evento**: sin este, quien se conecta no sabe si está conectado hasta la primera importación (el
  test que esperaba las cabeceras se quedaba esperando). Va después de `imports` en la lista, así que
  cuando el cliente lo recibe ya está suscrito al canal y no se pierde nada.
- **Latido**: un comentario SSE (`: ping`) cada 25 s, con
  `Multi.createFrom().ticks().startingAfter(...).every(...)`. Los proxies cortan las conexiones que
  llevan un rato sin tráfico; un comentario no es un evento para el cliente. Sin `startingAfter`, el
  primero sale nada más conectar.

**Hecho cuando:** con `curl -N localhost:8090/api/loads/events` en una terminal y
`curl -X POST localhost:8090/api/loads` en otra, ves `event:connected` y luego un `event:import` por
fichero.

📖 <https://quarkus.io/guides/rest>

## Paso 8. La calidad de un paquete — `GET /api/quality/{ep}`

El ejemplo de caché con clave (paso 4), con su 404 si el paquete no tiene datos.
`/api/quality/unrecognized` sigue funcionando: JAX-RS prefiere una ruta literal a una plantilla
`{ep}`.

## Paso 9. Tests

| Test | Qué demuestra | La técnica |
|---|---|---|
| `CacheNamesTest` | Qué cachés vacía cada maestro | JUnit a secas |
| `CacheBehaviourTest` | Que la respuesta sale de Redis, que se vacía al invalidar, que un evento LOV no vacía `ep-overview`, que `quality-by-ep` tiene su entrada por paquete y que un 404 no se cachea | **Cambiar la base de datos por debajo** (sin importar, así que nada se vacía): si la respuesta no cambia, viene de Redis. `@AfterEach` vuelve a importar para dejarlo todo como estaba |
| `ImportFeedTest` | Que cada suscriptor recibe un evento por fichero, y que uno tardío no recibe lo de antes | `AssertSubscriber` de Mutiny |
| `ImportEventsSseTest` | El SSE por HTTP, como un cliente real | `HttpClient` de Java leyendo línea a línea en un hilo virtual |

> ⚠️ **La trampa del `HttpClient`.** Con `try (HttpClient client = ...)` el test se queda colgado para
> siempre: `close()` espera a que terminen las peticiones en curso, y un SSE no termina nunca. Hay que
> cortarlo con `client.shutdownNow()` en un `finally`.

---

## Fase 2 terminada cuando
- `./mvnw verify` pasa (78 tests).
- Con los maestros reales, la segunda llamada a `/api/quality` o a `/api/eps/overview` sale de Redis
  y, tras `POST /api/loads`, vuelve a calcularse.
- `curl -N .../api/loads/events` muestra las importaciones en directo.

Siguiente: **fase 3**, gRPC: un servicio con llamadas unarias y en streaming, y su cliente.
