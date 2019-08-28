package fi.peltodata.service;

import fi.peltodata.domain.Farmfield;

import java.util.List;

public interface PeltodataService {
    Farmfield findFarmfield(long id);
    List<Farmfield> findAllFarmfields();
    List<Farmfield> findAllFarmfieldsByUser(long userId);
    boolean farmfieldBelongsToUser(long farmfieldId, long userId);
    long insertFarmfield(final Farmfield farmfield);
    void updateFarmfield(final Farmfield farmfield);
    void deleteFarmfield(final long layerId);
    void deleteFarmfield(Farmfield farmfield);
}
