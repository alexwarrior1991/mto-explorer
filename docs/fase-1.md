# Fase 1: cargar los maestros y servirlos por REST

**Objetivo:** al arrancar, la aplicación lee `lov-master.xlsx` y `profile-master.xlsx`, los guarda
en PostgreSQL y los sirve por una API REST: paquetes, vías, perfiles por tramo de PK, ficha de
perfil con sus ménsulas, catálogo de LOVs y un panel de calidad.

**Qué aprendes:** Dev Mode, Dev Services, `@ConfigMapping`, Flyway, Panache con Hibernate
Reactive, Mutiny (`Uni`), `@Observes StartupEvent`, Quarkus REST, validación, OpenAPI y
`@QuarkusTest`.

Las columnas de cada hoja están en [maestros.md](maestros.md). Haz los pasos en orden: cada uno
deja algo que se puede probar. Cuando acabes uno, pídeme que lo revise.

---

## Paso 0. Arrancar en Dev Mode

```bash
./mvnw quarkus:dev
```

- Mira en el log cómo Dev Services levanta PostgreSQL y Redis (`docker ps`).
- Abre la Dev UI (<http://localhost:8090/q/dev-ui>): extensiones, configuración, Dev Services.
- Con la consola de Dev Mode: `r` lanza los tests, `s` reinicia y `h` muestra la ayuda.

**Hecho cuando:** <http://localhost:8090/q/health/ready> responde `UP`.

## Paso 1. Configuración tipada — paquete `config`

Crea `ExplorerConfig`, una interfaz con `@ConfigMapping(prefix = "mto")` que lea las claves
`mto.*` de `application.yml`:
- `Optional<Path> dataDir()`: sin ella la aplicación arranca vacía.
- `lovFile()`, `profileFile()`, `loadOnStartup()`, con `@WithDefault`.

Pista: en YAML `data-dir` se mapea al método `dataDir()` sin hacer nada.

**Hecho cuando:** al inyectarla en algún bean y escribir sus valores en el log, salen bien. Si
arrancas con `MTO_DATA_DIR=/algo`, `dataDir()` lo trae.

📖 <https://quarkus.io/guides/config-mappings>

## Paso 2. Puntos kilométricos — paquete `kp`

Una clase `Kp` con dos métodos estáticos, en Java puro:
- `String format(Double metros)`: `17858.0` da `"17+858"`, `17858.5` da `"17+858.5"`,
  `12.3` da `"0+012.3"`, `-250.0` da `"-0+250"` y `null` da `null`.
- `double parse(String texto)`: acepta `"12+345"`, `"12+345.5"`, `"12+345,5"` y metros a secas
  (`"12345"`). Si no es ninguna de esas formas, lanza `IllegalArgumentException` con un mensaje
  claro.

Escribe `KpTest`, un JUnit normal **sin** `@QuarkusTest`, con esos casos.

**Hecho cuando:** `KpTest` pasa (en Dev Mode, pulsa `r`).

## Paso 3. El esquema — `src/main/resources/db/migration/V1__esquema_inicial.sql`

Tablas: `execution_package`, `station`, `track`, `profile`, `cantilever`, `lov`,
`unrecognized_value` y `master_load` (una fila por fichero cargado: nombre, fecha de
modificación, tamaño, cuándo se cargó, filas y duración).

Pistas:
- `PanacheEntity` pone un `id BIGINT` generado con una secuencia `<tabla>_seq` de
  `INCREMENT BY 50`. Créala tú en la migración.
- Con la estrategia de nombres configurada, `sortOrder` en Java es `sort_order` en SQL. Ojo:
  `order` es palabra reservada, no la uses como columna.
- Pon índices para las consultas que vas a hacer: perfiles por `(ep, track, kp)` y por
  `(ep, track, sort_order)`, y ménsulas por `(ep, track, profile_order)`.

**Hecho cuando:** al arrancar, Flyway aplica la `V1` sin errores (lo verás en el log).

📖 <https://quarkus.io/guides/flyway>

## Paso 4. Entidades — paquete `entity`

Extiende `io.quarkus.hibernate.reactive.panache.PanacheEntity` (**la de `reactive`**, no la de
`orm`), con campos `public` (patrón *active record*).

- `ExecutionPackage`, `Station`, `Track`, `Profile`, `Cantilever`, `Lov`, `UnrecognizedValue` y
  `MasterLoad`.
- Añade métodos estáticos de consulta en la propia entidad. Por ejemplo,
  `Profile.findByTrackAndOrder(ep, track, order)` devuelve `Uni<Profile>`.

Lo nuevo frente a Spring Data: **todo devuelve `Uni`**. `Profile.listAll()` no es una lista,
sino la promesa de una lista.

**Hecho cuando:** arranca sin errores de `schema-management: validate`. Si falla, el mensaje
dice qué columna no cuadra entre tu entidad y tu SQL.

📖 <https://quarkus.io/guides/hibernate-reactive-panache>

## Paso 5. Leer Excel en streaming — paquete `load`

`profile-master.xlsx` tiene hojas de más de 40 000 filas: no lo cargues entero en memoria. Usa
`excel-streaming-reader`, que ya está en el `pom.xml`:

```java
try (Workbook wb = StreamingReader.builder().rowCacheSize(200).bufferSize(8192).open(file.toFile())) {
    Sheet sheet = wb.getSheet("PROFILES");
    for (Row row : sheet) { ... }   // en streaming, cada hoja se recorre una sola vez
}
```

Sugerencia de diseño:
- `MasterWorkbook(Path)` con `int forEachRow(String hoja, Consumer<SheetRow> accion)`: lee la
  cabecera, salta las filas vacías y devuelve cuántas ha procesado.
- `SheetRow`, que lee **por nombre de columna**: `string("VIA")`, `decimal("KP")`,
  `integer("ORDEN")`, `yesNo("REVISAR")` y `date("START_DATE")`. Si la columna no existe,
  lanza una excepción propia (`MasterFormatException`) que diga columna y fila.
- Tipos de celda: `NUMERIC`, `STRING`, `BLANK` y `FORMULA` (de esta, usa
  `getCachedFormulaResultType()`). Para las fechas, `getLocalDateTimeCellValue()`.

**Hecho cuando:** un test (ver paso 8) lee los maestros inventados y cuenta las filas
esperadas.

## Paso 6. Cargar en la base de datos — `MasterLoader`

Por cada fichero (primero el de LOVs y luego el de perfiles):
1. Si no existe, devuelve `MISSING`.
2. Si su fecha de modificación y su tamaño coinciden con su último `MasterLoad`, devuelve
   `UNCHANGED` y no toca nada.
3. Si no, **en una sola transacción**: borra lo de ese maestro, inserta lo que trae el Excel y
   guarda un `MasterLoad`. Devuelve `LOADED` con las filas y los milisegundos.

Lo delicado es **mezclar código bloqueante (leer el Excel) con código reactivo (Hibernate
Reactive)**:
- Hibernate Reactive necesita ejecutarse en un contexto de Vert.x. Desde un hilo normal (el de
  arranque, un hilo *worker*, más adelante el del Scheduler) se entra así:
  ```java
  VertxContextSupport.subscribeAndAwait(() ->
          Panache.withTransaction(() -> Lov.deleteAll().chain(() -> Lov.persist(lovs))));
  ```
- Así que `MasterLoader.reload(boolean force)` puede ser un método **bloqueante** normal, que
  lee el Excel en una lista y la guarda con `subscribeAndAwait`. Nunca lo llames desde el event
  loop.
- La fecha de modificación de un fichero tiene nanosegundos y PostgreSQL guarda microsegundos.
  Trúncala a milisegundos o la comparación del punto 2 nunca coincidirá.

📖 <https://quarkus.io/guides/vertx-reference> ·
<https://quarkus.io/guides/hibernate-reactive-panache#transactions>

## Paso 7. Cargar al arrancar

Un bean que observe `StartupEvent` y, si `loadOnStartup()` está activo, llame a
`reload(false)`.

**Hecho cuando:**
```bash
MTO_DATA_DIR=/ruta/a/mto-configuration/data ./mvnw quarkus:dev
```
escribe en el log algo como `lov-master.xlsx: 763 filas` y `profile-master.xlsx: 28817 filas`.
Al reiniciar (`s`) dice que no han cambiado.

📖 <https://quarkus.io/guides/lifecycle>

## Paso 8. Tests con los maestros inventados

`src/test/java/.../load/MasterFixtures.java` ya está hecho: escribe dos maestros pequeños con
la misma forma que los reales. Su Javadoc describe qué contienen.

Escribe `MasterFixturesResource implements QuarkusTestResourceLifecycleManager`. En
`start()` crea una carpeta temporal, llama a `MasterFixtures.writeAll(dir)` y devuelve
`Map.of("mto.data-dir", dir.toString())`. Luego, en cada test:

```java
@QuarkusTest
@WithTestResource(MasterFixturesResource.class)
class ExplorerApiTest { ... }
```

📖 <https://quarkus.io/guides/getting-started-testing>

## Paso 9. La API — paquete `api`

Los métodos devuelven `Uni<...>`. Las lecturas van con `@WithSession`; si ves
`No current Mutiny.Session found`, te falta.

| Método | Ruta | Qué devuelve |
|---|---|---|
| GET | `/api/eps` | Paquetes |
| GET | `/api/eps/{ep}/stations` · `/api/eps/{ep}/tracks` | Estaciones y vías (404 si el paquete no existe). Las estaciones de una vía, como lista |
| GET | `/api/profiles?ep=&track=&kpFrom=&kpTo=&toReview=&page=0&size=50` | Perfiles paginados: `{items, page, size, total}`. `ep` obligatorio; el PK acepta `12+345` o metros; `size` como mucho 500 |
| GET | `/api/profiles/{ep}/{track}/{order}` | Ficha del perfil con sus ménsulas ordenadas por `slot` |
| GET | `/api/lovs?entity=&q=` | LOVs paginadas; `q` busca en el código y las descripciones |
| GET | `/api/lovs/entities` | Por familia: número de códigos y cuántos por revisar |
| GET | `/api/quality` | Por paquete: perfiles, perfiles por revisar, ménsulas, ménsulas por revisar y no reconocidos |
| GET | `/api/quality/unrecognized?master=&ep=` | La hoja `NO_RECONOCIDO` |
| GET | `/api/loads` | Últimas cargas |
| POST | `/api/loads?force=true` | Recargar. **Es bloqueante** (lee Excel): márcalo con `@Blocking` |

Pistas:
- Devuelve **DTOs** (`record`), no entidades: el `id` cambia en cada carga y no le sirve a
  nadie. Añade `kpText` (`17+858.5`) junto a `kp`.
- Validación: `@NotBlank`, `@Min` y `@Max` en los `@QueryParam`. Hibernate Validator responde
  400 solo.
- Errores en `application/problem+json`, como en `mto-configuration`, con
  `@ServerExceptionMapper` (`IllegalArgumentException` → 400; `MasterFormatException` → 422).
- Paginación con Panache: `find(...).page(page, size).list()` y `.count()`. Son dos `Uni`;
  combínalos con `Uni.combine().all().unis(a, b).asTuple()`.

**Hecho cuando:** puedes probarlo todo desde Swagger UI (<http://localhost:8090/q/swagger-ui>).

📖 <https://quarkus.io/guides/rest> · <https://quarkus.io/guides/validation>

## Paso 10. Tests de la API

Con RestAssured y los maestros inventados. Algunos casos que merecen test:
- `GET /api/eps` devuelve `EP1` y `EP2`, y la fecha de inicio de EP1 es `2020-01-15`.
- Perfiles de EP1, TRACK 1, entre el `12+050` y el `12+200`: los de orden 2 y 3, y el primero
  con `kpText` igual a `12+054.5`.
- La ficha de `EP1/TRACK 1/2` trae dos ménsulas (slots 1 y 2), y la de `EP1/TRACK 1/3` ninguna,
  aunque comparten `PROFILE_ID`.
- `kpFrom=doce` da 400 con `application/problem+json`.
- `/api/quality` para EP1: 5 perfiles, 1 por revisar, 3 ménsulas, 1 por revisar y 2 no
  reconocidos.
- `POST /api/loads?force=false` da `UNCHANGED` en los dos ficheros, y con `force=true` da
  `LOADED`.

---

## Fase 1 terminada cuando
- `./mvnw verify` pasa.
- Con los maestros reales, todas las consultas responden bien desde Swagger UI.
- Has hecho commit de cada paso.

Siguiente: **fase 2**, caché en Redis (`@CacheResult` en las consultas, que se invalida al
recargar) y sacar más partido a Mutiny (`Multi`).
