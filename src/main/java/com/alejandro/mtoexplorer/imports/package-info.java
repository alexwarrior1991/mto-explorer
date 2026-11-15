/**
 * Importación de los maestros Excel, en cuatro piezas con una responsabilidad cada una:
 * <ul>
 *   <li>{@code excel}: abrir un .xlsx en streaming y leer filas por nombre de columna.
 *   <li>{@code reader}: traducir las hojas de cada maestro a entidades, sin tocar la base de datos.
 *   <li>{@code store}: sustituir en la base de datos lo de un maestro por lo leído (reactivo).
 *   <li>{@link com.alejandro.mtoexplorer.imports.MasterImportService}: decidir qué se importa y
 *       en qué orden, y unir el mundo bloqueante (Excel) con el reactivo (Hibernate Reactive).
 * </ul>
 */
package com.alejandro.mtoexplorer.imports;
