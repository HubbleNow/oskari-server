package fi.peltodata.service;

import fi.peltodata.domain.Farmfield;
import fi.peltodata.domain.FarmfieldFileDataType;

import java.io.InputStream;
import java.nio.file.Path;
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

    String uploadLayerData(long farmfieldId, InputStream inputStream, FarmfieldFileDataType dataType, String filename);
    boolean fileExists(long farmfieldId, FarmfieldFileDataType dataType, String filename);
    List<String> findAllFarmfieldFiles(long farmfieldId);

    void createFarmfieldLayer(long farmfieldId, String inputFilepath,
                              FarmfieldFileDataType inputDataType, FarmfieldFileDataType outputDataType);
    String createFarmfieldGeoserverLayer(Farmfield farmfield, Path absolutePath);

}
