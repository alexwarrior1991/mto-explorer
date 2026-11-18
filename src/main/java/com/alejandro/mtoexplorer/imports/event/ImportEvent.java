package com.alejandro.mtoexplorer.imports.event;

import com.alejandro.mtoexplorer.imports.FileImportResult;

import java.time.Instant;

/** Un aviso del canal en directo: lo que pasó con un fichero en una importación, y cuándo. */
public record ImportEvent(Instant at, FileImportResult result) {
}
