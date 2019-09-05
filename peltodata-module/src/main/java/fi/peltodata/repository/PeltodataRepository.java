package fi.peltodata.repository;

import fi.peltodata.domain.Farmfield;
import fi.peltodata.domain.FarmfieldExecution;

import java.util.List;

public interface PeltodataRepository {
    List<Farmfield> findAllFarmFields();
    Farmfield findFarmfield(long id);
    List<Farmfield> findAllFarmfieldsByUser(long userId);
    boolean farmfieldBelongsToUser(long farmfieldId, long userId);
    long insertFarmfield(final Farmfield farmfield);
    void updateFarmfield(final Farmfield farmfield);
    void deleteFarmfield(final long layerId);

    List<FarmfieldExecution> findAllFarmfieldExecutionsForUser(Long userId);

    List<FarmfieldExecution> findAllFarmfieldExecutions();

    void insertFarmfieldExecution(FarmfieldExecution execution);

    void updateFarmfieldExecution(FarmfieldExecution execution);

    FarmfieldExecution findFarmfieldExecution(long id);
}
