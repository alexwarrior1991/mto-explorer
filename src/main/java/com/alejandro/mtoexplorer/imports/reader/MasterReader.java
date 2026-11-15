package com.alejandro.mtoexplorer.imports.reader;

import com.alejandro.mtoexplorer.imports.excel.MasterWorkbook;

/** Lee un maestro entero. Bloqueante: recorre el Excel. */
public interface MasterReader<M extends ReadMaster> {

    M read(MasterWorkbook workbook);
}
