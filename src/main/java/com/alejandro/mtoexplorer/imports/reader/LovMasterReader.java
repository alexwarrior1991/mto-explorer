package com.alejandro.mtoexplorer.imports.reader;

import com.alejandro.mtoexplorer.entity.Lov;
import com.alejandro.mtoexplorer.imports.excel.MasterWorkbook;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/** Lee lov-master.xlsx: la hoja LOVS y su NO_RECONOCIDO. */
@ApplicationScoped
public class LovMasterReader implements MasterReader<LovMaster> {

    public static final String MASTER = "LOV";

    @Override
    public LovMaster read(MasterWorkbook workbook) {
        List<Lov> lovs = new ArrayList<>();
        workbook.forEachRow("LOVS", r -> {
            Lov lov = new Lov();
            lov.entity = r.string("ENTIDAD");
            lov.code = r.string("CODIGO");
            lov.descriptionEn = r.string("DESCRIPCION_EN");
            lov.descriptionEs = r.string("DESCRIPCION_ES");
            lov.type = r.string("TIPO");
            lov.planNumber = r.integer("N_PLANO");
            lov.enabled = r.yesNo("ENABLED");
            lov.origin = r.string("ORIGEN");
            lov.boqCategories = r.string("CATEGORIAS_BOQ");
            lov.eps = r.string("EPS");
            lov.usesInTracks = r.integer("USOS_EN_TRACKS");
            lov.toReview = r.yesNo("REVISAR");
            lovs.add(lov);
        });
        return new LovMaster(lovs, UnrecognizedSheet.read(workbook, MASTER));
    }
}
