package fi.peltodata.service;

import fi.nls.oskari.domain.User;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.domain.FarmfieldExecution;
import fi.peltodata.domain.FarmfieldFile;
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

    FarmfieldFile uploadLayerData(long farmfieldId, InputStream inputStream, FarmfieldFileDataType dataType, String originalFilename, String filename);
    boolean fileExists(long farmfieldId, FarmfieldFileDataType dataType, String filename);
    List<String> findAllFarmfieldFiles(long farmfieldId);

    void createFarmfieldLayer(long farmfieldId, long farmfieldFileId,
                              FarmfieldFileDataType inputDataType, FarmfieldFileDataType outputDataType, User user);
    String createFarmfieldGeoserverLayer(Farmfield farmfield, Path absolutePath, String outputType);

    List<FarmfieldExecution> findAllFarmfieldExecutionsForUser(Long userId);

    List<FarmfieldExecution> findAllFarmfieldExecutions();

    FarmfieldExecution farmfieldExecutionStarted(FarmfieldFile farmfieldFile, Path outputFilePath, String outputType);

    void farmfieldExecutionCompleted(FarmfieldExecution execution);

    void farmfieldExecutionFailed(FarmfieldExecution execution);

    String getInputFilename(FarmfieldFileDataType dataType);

    void addWMSLayerFromGeoserver(Farmfield farmfield, FarmfieldFile farmfieldFile, String layerName, FarmfieldFileDataType outputType, User user);

    boolean farmfieldFileBelongsToFarmAndUser(Long fileId, Long farmFieldId, User user);

    FarmfieldFile findFarmfieldFile(Long fileId);

    void updateFarmfieldFile(FarmfieldFile file);
}
