package com.alejandro.mtoexplorer.imports.reader;

import com.alejandro.mtoexplorer.imports.excel.MasterFormatException;
import com.alejandro.mtoexplorer.imports.excel.SheetRow;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Lo ya leído de una hoja, por su clave natural, para que las hojas siguientes enlacen con el
 * objeto (no con su clave). Si una fila apunta a un padre que no existe, la importación falla
 * diciendo la columna y la fila: mejor eso que una relación vacía sin avisar.
 *
 * @param <K> la clave natural ((paquete, nombre), (paquete, vía, orden)…)
 * @param <V> la entidad
 */
final class Lookup<K, V> {

    private final String what;
    private final Map<K, V> byKey = new HashMap<>();

    /** {@code what} es cómo se nombra el padre en el mensaje de error: "la vía", "la estación"… */
    Lookup(String what) {
        this.what = what;
    }

    static <K, V> Lookup<K, V> of(String what, Iterable<V> values, Function<V, K> key) {
        Lookup<K, V> lookup = new Lookup<>(what);
        values.forEach(v -> lookup.byKey.put(key.apply(v), v));
        return lookup;
    }

    /** El padre de esa clave; si no existe, MasterFormatException en {@code column} de la fila. */
    V require(K key, SheetRow row, String column) {
        V value = byKey.get(key);
        if (value == null) {
            throw new MasterFormatException(column, row.rowNumber(), "no existe " + what + " " + key);
        }
        return value;
    }

    /** Como {@link #require}, pero una clave null (celda vacía) da null en vez de fallar. */
    V optional(K key, SheetRow row, String column) {
        return key == null ? null : require(key, row, column);
    }
}
