package com.alejandro.mtoexplorer.service.model;

import com.alejandro.mtoexplorer.entity.SectionInsulator;
import com.alejandro.mtoexplorer.entity.SectionInsulatorSwitch;

import java.util.List;

/** Un aislador de sección con sus agujas. */
public record InsulatorWithSwitches(SectionInsulator insulator, List<SectionInsulatorSwitch> switches) {
}
