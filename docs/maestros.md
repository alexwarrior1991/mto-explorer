# Los maestros: hojas y columnas

Los genera `mto-configuration/data/tools` (`build_lov_master.py` y `build_profile_master.py`).
Aquí está lo que el explorador lee, con lo que se ha visto en los datos reales.

**Lee las columnas por su nombre en la cabecera (fila 1), no por su posición.** El generador
puede cambiar el orden de las columnas.

Convenciones de todas las hojas:
- `SI` / `NO` → booleano. Vacío cuenta como `NO`.
- Los números pueden venir como entero o como decimal en la misma columna (por ejemplo, `KP` y
  `SPAN`): léelos siempre como `Double`.
- Las celdas vacías son `null`.
- Las hojas suelen acabar con filas vacías con formato. Salta las filas cuya primera celda esté
  vacía.

## lov-master.xlsx

### `LOVS` (~760 filas)
| Columna | Tipo | Ejemplo |
|---|---|---|
| `ENTIDAD` | texto | `Sectioning`, `PoleType` |
| `CODIGO` | texto | `A/S-Diag` |
| `DESCRIPCION_EN` | texto | `Diagonal Anchorage` |
| `DESCRIPCION_ES` | texto | |
| `TIPO` | texto | |
| `N_PLANO` | entero | |
| `ENABLED` | SI/NO | |
| `ORIGEN` | texto | `LEGEND+TRACK` |
| `CATEGORIAS_BOQ` | texto | |
| `EPS` | texto, lista separada por comas | `EP11A, EP4, RUBI` |
| `USOS_EN_TRACKS` | entero | `583` |
| `REVISAR` | SI/NO | |

### `NO_RECONOCIDO`
Lo que el generador no supo mapear. Está en los **dos** maestros, con las mismas columnas.

| Columna | Tipo |
|---|---|
| `TIPO` | texto (`codigo sin Sectioning habilitado`…) |
| `VALOR` | texto |
| `EP` | texto |
| `HOJA` | texto |
| `PRIMERA_FILA` | entero |
| `APARICIONES` | entero |

Hojas que el explorador no lee: `LEEME`, `TIPOS`, `USO_TRACKS`, `DESCARTADOS`.

## profile-master.xlsx

### `EPS` (12 filas): paquetes de ejecución
| Columna | Tipo | Ejemplo |
|---|---|---|
| `EP` | texto | `EP11A` |
| `NOMBRE` | texto | `EP14 Ashkelon Station-Depot` |
| `INITIAL_PACKAGE` | SI/NO | |
| `LENGTH` | entero (metros) | `32168` |
| `START_DATE`, `END_DATE` | fecha | `2018-01-25` |
| `COMPANY_ID_NUMBER` | texto | |
| `ENABLED` | SI/NO | |

### `STATIONS` (~50 filas)
`EP`, `NOMBRE` (`MOM`, `PMO`…).

### `TRACKS` (~180 filas)
| Columna | Tipo | Ejemplo |
|---|---|---|
| `EP` | texto | |
| `NOMBRE` | texto | `TRACK 1`, `TRACK 1 MOM` |
| `ESTACIONES` | texto, separadas por ` \| ` | `MOM \| PMO` |
| `ENABLED` | SI/NO | |
| `HOJA_ORIGEN` | texto | `HR Track 1` |
| `FILA_INICIO`, `FILA_FIN` | siempre vacías hoy | |

### `PROFILES` (~12 900 filas): postes y pórticos
| Columna | Tipo | Notas |
|---|---|---|
| `EP`, `VIA`, `PROFILE_ID` | texto | `VIA` es el nombre de la vía (`TRACK 1`) |
| `KP` | decimal | PK en metros: `17858.5` es el `17+858.5` |
| `ORDEN` | entero | **Identifica el perfil dentro de la vía** |
| `PROFILE_STATUS` | texto | `DEFINITIVE`… |
| `SECTIONING`, `ANCHORAGE`, `ANCHORAGE_FOUNDATION`, `FOUNDATION`, `POLE_TYPE`, `PORTAL`, `RETURN_SUPPORT`, `SECTIONING_FEEDING`, `SUPPORT_TYPE`, `ASSEMBLY_CONFIGURATION` | texto | Códigos del catálogo de LOVs |
| `SPAN` | decimal | |
| `HEIGHT_CANTILEVER_SUPPORT`, `POLE_GAUGE_LOCATION`, `RAIL_POLE_DISTANCE` | decimal | |
| `ENABLED`, `REVISAR` | SI/NO | |
| `HOJA_ORIGEN`, `FILA_ORIGEN` | texto, entero | Dónde estaba en el workbook de ingeniería |

⚠️ **`PROFILE_ID` no es único dentro de una vía**: hay 94 casos repetidos en el maestro real.
Lo que identifica un perfil es **(`EP`, `VIA`, `ORDEN`)**, y es también lo que usan sus ménsulas.

### `CANTILEVERS` (~15 600 filas): ménsulas
| Columna | Tipo | Notas |
|---|---|---|
| `EP`, `VIA`, `PROFILE_ID` | texto | |
| `ORDEN` | entero | El `ORDEN` del perfil al que pertenece |
| `SLOT` | entero | Posición de la ménsula en el perfil (1, 2…) |
| `CANTILEVER_TYPE` | texto | `EMT-2T` |
| `STAGGER` | decimal | Descentramiento, puede ser negativo |
| `CATENARY_HEIGHT`, `CW_ELEVATION`, `CW_HEIGHT`, `WIND_DEFLECTION`, `ARM_ANGLE` | decimal | |
| `STEADY_ARM_TYPE` | texto | |
| `STEADY_ARM_LENGTH` | decimal | |
| `ENABLED`, `REVISAR` | SI/NO | |
| `FILA_ORIGEN` | entero | |

### `NO_RECONOCIDO`
Igual que la del maestro de LOVs.

Hojas que el explorador no lee (por ahora): `LEEME`, `DISCONNECTORS`, `SECTION_INSULATORS`,
`SECTION_INSULATOR_SWITCHES` (hoy vacías), `NO_MAPEADO` (~43 000 filas) y `DESCARTADOS`.
