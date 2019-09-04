package fi.peltodata.service;

import fi.mml.map.mapwindow.service.db.OskariMapLayerGroupService;
import fi.mml.map.mapwindow.service.db.OskariMapLayerGroupServiceIbatisImpl;
import fi.nls.oskari.annotation.Oskari;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceMybatisImpl;
import fi.nls.oskari.service.OskariComponent;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.util.PropertyUtil;
import fi.peltodata.config.PeltodataConfig;
import fi.peltodata.domain.*;
import fi.peltodata.geoserver.GeoserverClient;
import fi.peltodata.geoserver.GeoserverException;
import fi.peltodata.repository.PeltodataRepository;
import fi.peltodata.repository.PeltodataRepositoryImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Oskari
public class PeltodataServiceMybatisImpl extends OskariComponent implements PeltodataService {
    private static final Logger LOG = LogFactory.getLogger(PeltodataRepositoryImpl.class);
    private PeltodataRepository peltodataRepository;
    private static OskariMapLayerGroupService oskariMapLayerGroupService = new OskariMapLayerGroupServiceIbatisImpl();
    private static OskariLayerService oskariLayerService = new OskariLayerServiceMybatisImpl();
    private Executor executor;

    private GeoserverClient geoserverClient;

    public PeltodataServiceMybatisImpl() throws ServiceException {
        UserService userService = UserService.getInstance();
        PeltodataRepository peltodataRepository = new PeltodataRepositoryImpl(userService, new OskariLayerServiceMybatisImpl());
        this.peltodataRepository = peltodataRepository;
        executor = Executors.newFixedThreadPool(3);
        geoserverClient = new GeoserverClient();
    }

    @Override
    public Farmfield findFarmfield(long id) {
        return peltodataRepository.findFarmfield(id);
    }

    @Override
    public List<Farmfield> findAllFarmfields() {
        return peltodataRepository.findAllFarmFields();
    }

    @Override
    public List<Farmfield> findAllFarmfieldsByUser(long userId) {
        return peltodataRepository.findAllFarmfieldsByUser(userId);
    }

    @Override
    public boolean farmfieldBelongsToUser(long farmfieldId, long userId) {
        return peltodataRepository.farmfieldBelongsToUser(farmfieldId, userId);
    }

    @Override
    public void updateFarmfield(final Farmfield farmfield) {
        peltodataRepository.updateFarmfield(farmfield);
    }

    @Override
    public synchronized long insertFarmfield(final Farmfield farmfield) {
        return peltodataRepository.insertFarmfield(farmfield);
    }

    @Override
    public void deleteFarmfield(long id) {
        peltodataRepository.deleteFarmfield(id);
    }

    @Override
    public void deleteFarmfield(Farmfield farmfield) {
        deleteFarmfield(farmfield.getId());
    }

    private boolean fileExists(String filePathString) {
        File f = new File(filePathString);
        return f.exists() && !f.isDirectory();
    }

    @Override
    public boolean fileExists(long farmfieldId, FarmfieldFileDataType dataType, String filename) {
        String fileTimestamp = filename.split(".")[0];
        int year = LocalDateTime.parse(fileTimestamp).getYear();
        String uploadPath = getUploadPath(year, farmfieldId, dataType);
        String filePathString = uploadPath + filename;
        return fileExists(filePathString);
    }

    @Override
    public List<String> findAllFarmfieldFiles(long farmfieldId) {
        try {
            Path farmfieldRootPath = Paths.get(getFarmUploadRootPath(farmfieldId));
            if (Files.exists(farmfieldRootPath)) {
                List<String> fileList = Files.walk(Paths.get(getFarmUploadRootPath(farmfieldId)))
                        .filter(Files::isRegularFile)
                        .map(this::getRelativeFarmfieldFilePath)
                        .collect(Collectors.toList());
                return fileList;
            }
        } catch (IOException e) {
            LOG.error(e, "Error while traversing path: " + getFarmUploadRootPath(farmfieldId));
        }
        return Collections.emptyList();
    }

    private String getFarmUploadRootPath(long farmfieldId) {
        String uploadRootDir = PropertyUtil.get(PeltodataConfig.PROP_UPLOAD_ROOT_DIR_PATH, "." + File.separator + "geoserver_data");
        String farmUploadPath = uploadRootDir
                + File.separator + "farms"
                + File.separator + farmfieldId
                + File.separator;
        return farmUploadPath;
    }

    private String getUploadPath(int year, long farmfieldId, FarmfieldFileDataType dataType) {
        String farmUploadPath = getFarmUploadRootPath(farmfieldId);
        String uploadPath = farmUploadPath
                + year
                + File.separator + dataType.getFolderName()
                + File.separator;
        return uploadPath;
    }

    private String getRelativeFarmfieldFilePath(Path fullPath) {
        return fullPath.getParent().getParent().toFile().getName()
                + File.separator + fullPath.getParent().toFile().getName()
                + File.separator + fullPath.getFileName();
    }

    @Override
    public String uploadLayerData(long farmfieldId, InputStream inputStream, FarmfieldFileDataType dataType, String filename) {
        int year = Year.now().getValue();
        String uploadPath = getUploadPath(year, farmfieldId, dataType);
        String filePathString = uploadPath + filename;
        LOG.info("about upload file : " + filePathString);
        boolean success = false;
        Path newFile = Paths.get(filePathString);
        LOG.info("full path " + FileSystems.getDefault().getPath(newFile.toString()).normalize().toAbsolutePath());
        try {
            Files.createDirectories(newFile.getParent());
            Files.copy(inputStream, newFile);
            success = true;
        } catch (IOException e) {
            LOG.error(e, "Error occured while writing file: " + filePathString);
        }
        return success ? getRelativeFarmfieldFilePath(newFile) : null;
    }

    @Override
    public void createFarmfieldLayer(long farmfieldId, String inputFilepath,
                                     FarmfieldFileDataType inputDataType, FarmfieldFileDataType outputDataType) {
        LOG.info("createFarmfieldLayer id={} inputfile={} inputtype={} outputtype={}",
                farmfieldId, inputFilepath, inputDataType.toString(), outputDataType.toString());
        String fullPath = getFarmUploadRootPath(farmfieldId) + inputFilepath;
        Farmfield farmfield = findFarmfield(farmfieldId);


        Path absolutePath = FileSystems.getDefault().getPath(fullPath).normalize().toAbsolutePath();

        startAsyncFarmfieldLayerConversionCreation(farmfield, absolutePath, outputDataType);

        // String geoserverLayerName = createFarmfieldGeoserverLayer(farmfield, inputFilepath, outputDataType);
        //create geoserver datastore + publish + wms layer (srs?)
        // investigate if web-api or !!!! { fi.nls.oskari.control.layer.SaveLayerHandler.saveLayer } could be used instead (--> decoupling out of this service)
        // minimum inputs (layername, groups, permissions, srs?, force-proxy ?)
        //  --- create layer
        //  --- assign permissions for user 2
        //add peltodata_field_layer row
    }

    protected void startAsyncFarmfieldLayerConversionCreation(Farmfield farmfield, Path inputFilepath, FarmfieldFileDataType outputDataType) {

        // ....\2019\crop_estimation_raw\20190904125547.tiff -> ....\2019
        Path root = inputFilepath.getParent().getParent();
        Path outputFilePath = Paths.get(root.toString(), outputDataType.getFolderName(), inputFilepath.getFileName().toString());
        try {
            Files.createDirectories(outputFilePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directory" + outputFilePath.getParent().toString(), e);
        }

        LOG.info("Starting async conversion of {} to {} type={}", inputFilepath.toString(), outputFilePath.toString(), outputDataType.toString());

        switch (outputDataType) {
            case CROP_ESTIMATION_RAW_DATA:
                createFarmfieldGeoserverLayer(farmfield, inputFilepath);
                break;
            case CROP_ESTIMATION_DATA:
                CropEstimationTask cropEstimationTask = new CropEstimationTask(this, farmfield, inputFilepath, outputFilePath);
                executor.execute(cropEstimationTask);
                break;
            case YIELD_RAW_DATA:
                break;
            case YIELD_DATA:
                YieldImageTask yieldImageTask = new YieldImageTask(this, farmfield, inputFilepath, outputFilePath);
                executor.execute(yieldImageTask);
                break;
        }
    }

    public String createFarmfieldGeoserverLayer(Farmfield farmfield, Path absolutePath) {
        if (!Files.exists(absolutePath)) {
            throw new IllegalArgumentException("absolutePath does not exist " + absolutePath);
        }

        // check if layer exists ? throw, null or update if exists ??
        String wmsBaseUrl = geoserverClient.getWMSBaseUrl();
        String filename = absolutePath.getFileName().toString();
        filename = filename.substring(0, filename.indexOf('.'));
        String description = getCleanedUpDescription(farmfield);
        String maplayerName = description + "_" + filename;
        List<OskariLayer> layers = oskariLayerService.findByUrlAndName(wmsBaseUrl, maplayerName);
        if (layers != null && layers.size() > 0) {
            throw new RuntimeException("layer exists");
        }

        try {
            geoserverClient.saveTiffAsDatastore(maplayerName, absolutePath);
            return maplayerName;
        } catch (GeoserverException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FarmfieldExecution> findAllFarmfieldExecutionsForUser() {
        return peltodataRepository.findAllFarmfieldExecutionsForUser();
    }

    private String getCleanedUpDescription(Farmfield farmfield) {
        String description = farmfield.getDescription().toLowerCase();
        char[] forbidden = new char[]{'ä', 'ö', ' '};
        char[] converted = new char[]{'a', 'o', '_'};
        for (int i = 0; i < description.length(); i++) {
            for (int j = 0; j < forbidden.length; j++) {
                if (description.charAt(i) == forbidden[j]) {
                    description = description.replace(forbidden[j], converted[j]);
                }
            }
        }
        return description;
    }

}
