package fi.peltodata.service;

import fi.peltodata.domain.Farmfield;

import java.util.List;

public interface PeltodataService {
    Farmfield find(long id);
    List<Farmfield> findAll();
    long insert(final Farmfield farmfield);
    void update(final Farmfield farmfield);
    void delete(final long layerId);
}
