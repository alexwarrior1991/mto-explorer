package com.alejandro.mtoexplorer.imports.reader;

import com.alejandro.mtoexplorer.entity.Lov;
import com.alejandro.mtoexplorer.entity.UnrecognizedValue;

import java.util.List;

/** Lo leído de lov-master.xlsx. */
public record LovMaster(List<Lov> lovs, List<UnrecognizedValue> unrecognized) implements ReadMaster {

    @Override
    public int rows() {
        return lovs.size() + unrecognized.size();
    }
}
