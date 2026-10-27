# CLAUDE.md

## Proyecto
`mto-explorer`: explorador de los maestros de infraestructura de MTO (`lov-master.xlsx` y
`profile-master.xlsx`) hecho con Quarkus **para aprender Quarkus**. No depende del ecosistema
MTO: no llama a `mto-configuration`, ni a RabbitMQ, ni a Keycloak; solo lee los Excel.

## Modo de trabajo: el código lo escribe Alejandro
- Claude **explica, da pistas, revisa y propone**. No escribe código de `src/main` salvo que
  se le pida expresamente para algo concreto.
- Al revisar: primero lo que está mal o es frágil, después lo mejorable. Explicar el porqué y
  enlazar la guía de Quarkus correspondiente.
- La guía de cada fase está en `docs/`. Ir paso a paso; no adelantar fases.

## Reglas
- **Ningún Excel al repo** (`.gitignore` los excluye). Se leen de `MTO_DATA_DIR`. Los tests usan
  `MasterFixtures` (maestros inventados).
- Persistencia **reactiva**: Hibernate Reactive + Panache. Flyway migra por JDBC al arrancar.
- LangChain4j con **Ollama en local** (fase 5): los datos de ingeniería no salen de la máquina.
- Columnas de los maestros: `docs/maestros.md`.

## Comandos
- `MTO_DATA_DIR=/ruta/a/mto-configuration/data ./mvnw quarkus:dev`
- `./mvnw verify` (necesita Docker: Dev Services levanta PostgreSQL y Redis)
