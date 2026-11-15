package com.alejandro.mtoexplorer.imports.reader;

import com.alejandro.mtoexplorer.entity.UnrecognizedValue;
import com.alejandro.mtoexplorer.imports.excel.MasterWorkbook;

import java.util.ArrayList;
import java.util.List;

/** La hoja NO_RECONOCIDO, que tienen los dos maestros con las mismas columnas. */
final class UnrecognizedSheet {

    private UnrecognizedSheet() {
    }

    static List<UnrecognizedValue> read(MasterWorkbook workbook, String master) {
        List<UnrecognizedValue> values = new ArrayList<>();
        workbook.forEachRow("NO_RECONOCIDO", r -> {
            UnrecognizedValue u = new UnrecognizedValue();
            u.master = master;
            u.kind = r.string("TIPO");
            u.value = r.string("VALOR");
            u.ep = r.string("EP");
            u.sheet = r.string("HOJA");
            u.firstRow = r.integer("PRIMERA_FILA");
            u.occurrences = r.integer("APARICIONES");
            values.add(u);
        });
        return values;
    }
}
