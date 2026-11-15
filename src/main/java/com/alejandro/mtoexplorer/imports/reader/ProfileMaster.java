package com.alejandro.mtoexplorer.imports.reader;

import com.alejandro.mtoexplorer.entity.Cantilever;
import com.alejandro.mtoexplorer.entity.Disconnector;
import com.alejandro.mtoexplorer.entity.ExecutionPackage;
import com.alejandro.mtoexplorer.entity.Profile;
import com.alejandro.mtoexplorer.entity.SectionInsulator;
import com.alejandro.mtoexplorer.entity.SectionInsulatorSwitch;
import com.alejandro.mtoexplorer.entity.Station;
import com.alejandro.mtoexplorer.entity.Track;
import com.alejandro.mtoexplorer.entity.UnrecognizedValue;

import java.util.List;

/** Lo leído de profile-master.xlsx. Las tres listas del seccionamiento vienen vacías en un maestro antiguo. */
public record ProfileMaster(List<ExecutionPackage> executionPackages, List<Station> stations, List<Track> tracks,
                            List<Profile> profiles, List<Cantilever> cantilevers,
                            List<Disconnector> disconnectors, List<SectionInsulator> sectionInsulators,
                            List<SectionInsulatorSwitch> switches, List<UnrecognizedValue> unrecognized)
        implements ReadMaster {

    @Override
    public int rows() {
        return executionPackages.size() + stations.size() + tracks.size() + profiles.size() + cantilevers.size()
                + disconnectors.size() + sectionInsulators.size() + switches.size() + unrecognized.size();
    }
}
