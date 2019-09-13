package fi.peltodata.service;

import fi.mml.map.mapwindow.service.db.OskariMapLayerGroupService;
import fi.mml.map.mapwindow.service.db.OskariMapLayerGroupServiceIbatisImpl;
import fi.nls.oskari.annotation.Oskari;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.DataProvider;
import fi.nls.oskari.domain.map.MaplayerGroup;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.DataProviderService;
import fi.nls.oskari.map.layer.DataProviderServiceMybatisImpl;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceMybatisImpl;
import fi.nls.oskari.service.OskariComponent;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.util.PropertyUtil;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.domain.FarmfieldExecution;
import fi.peltodata.domain.FarmfieldFile;
import fi.peltodata.domain.FarmfieldFileDataType;
import fi.peltodata.geoserver.GeoserverClient;
import fi.peltodata.geoserver.GeoserverException;
import fi.peltodata.repository.PeltodataRepository;
import fi.peltodata.repository.PeltodataRepositoryImpl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Oskari
public class PeltodataServiceMybatisImpl extends OskariComponent implements PeltodataService {
    private static final Logger LOG = LogFactory.getLogger(PeltodataRepositoryImpl.class);
    private PeltodataRepository peltodataRepository;
    private OskariMapLayerGroupService oskariMapLayerGroupService;
    private DataProviderService dataProviderService;
    private OskariLayerService oskariLayerService;
    private Executor executor;
    private UserService userService;
    private GeoserverClient geoserverClient;

    public PeltodataServiceMybatisImpl() throws ServiceException {
        this(UserService.getInstance(), new GeoserverClient(), new OskariMapLayerGroupServiceIbatisImpl(), new DataProviderServiceMybatisImpl());
    }

    private PeltodataServiceMybatisImpl(UserService userService, GeoserverClient geoserverClient,
                                          OskariMapLayerGroupService oskariMapLayerGroupService,
                                          DataProviderService dataProviderService) throws ServiceException {
        this.executor = Executors.newFixedThreadPool(3);
        this.oskariLayerService =  new OskariLayerServiceMybatisImpl();
        this.geoserverClient = geoserverClient;
        this.oskariMapLayerGroupService = oskariMapLayerGroupService;
        this.dataProviderService = dataProviderService;
        this.userService = userService;
        this.peltodataRepository =  new PeltodataRepositoryImpl(userService, new OskariLayerServiceMybatisImpl());
    }

    protected PeltodataServiceMybatisImpl(OskariMapLayerGroupService oskariMapLayerGroupService, DataProviderService dataProviderService) throws ServiceException {
        this(UserService.getInstance(), new GeoserverClient(), oskariMapLayerGroupService, dataProviderService);
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
        Farmfield originalFarmfield = peltodataRepository.findFarmfield(farmfield.getId());
        if (!originalFarmfield.getDescription().equals(farmfield.getDescription())) {
            MaplayerGroup group = oskariMapLayerGroupService.findByName(originalFarmfield.getDescription());
            if (group != null) {
                Map<String, String> names = getLanguageNameMap();
                for (String language : names.keySet()) {
                    names.put(language, farmfield.getDescription());
                }
                group.setNames(names);
                oskariMapLayerGroupService.update(group);
            }
        }

        peltodataRepository.updateFarmfield(farmfield);
    }

    private Map<String,String> getLanguageNameMap() {
        Set<String> languages = new HashSet<>();
        languages.add(PropertyUtil.getDefaultLanguage());
        languages.addAll(Arrays.asList(PropertyUtil.getSupportedLanguages()));
        Map<String, String> names = new HashMap<>();
        for (String language : languages) {
            names.put(language, "");
        }
        return names;
    }

    @Override
    public synchronized long insertFarmfield(final Farmfield farmfield) {
        long farmfieldId = peltodataRepository.insertFarmfield(farmfield);
        // farmfield created ok, next check if group and dataprovider exists
            User user = farmfield.getUser();
            if (user == null) {
                Long userId = farmfield.getUserId();
                try {
                    user = userService.getUser(userId);
                } catch (ServiceException e) {
                    throw new RuntimeException("Farm created but failed get user", e);
                }
                farmfield.setUser(user);
            }
            // check if dataprovider / a.k.a. organization exists
            ensureDataProviderForUser(user);

            // create group / a.k.a. theme always when new farm is created
        ensureGroupForField(farmfield);
        return farmfieldId;
    }

    private MaplayerGroup ensureGroupForField(Farmfield farmfield) {
        String farmfieldDescription = farmfield.getDescription();

        MaplayerGroup group = oskariMapLayerGroupService.findByName(farmfieldDescription);
        if (group == null) {
            group = new MaplayerGroup();
            Map<String, String> names = getLanguageNameMap();
            for (String language : names.keySet()) {
                names.put(language, farmfieldDescription);
            }
            group.setNames(names);
            group.setParentId(-1);
            group.setSelectable(true);
            oskariMapLayerGroupService.insert(group);
        }
        return group;
    }

    private DataProvider ensureDataProviderForUser(User user) {
        String userLoginName = user.getScreenname();
        DataProvider userProvider = dataProviderService.findByName(userLoginName);
        if (userProvider == null) {
            Map<String, String> names = getLanguageNameMap();
            for (String language : names.keySet()) {
                names.put(language, userLoginName);
            }
            DataProvider dataProvider = new DataProvider();
            dataProvider.setNames(names);
            dataProviderService.insert(dataProvider);
            return dataProvider;
        }
        return userProvider;
    }

    @Override
    public void deleteFarmfield(long id) {
        peltodataRepository.deleteFarmfield(id);
    }

    @Override
    public void deleteFarmfield(Farmfield farmfield) {
        deleteFarmfield(farmfield.getId());
    }

    @Override
    public boolean fileExists(long farmfieldId, FarmfieldFileDataType dataType, String filename) {
        return FileService.fileExists(farmfieldId, dataType, filename);
    }

    @Override
    public List<String> findAllFarmfieldFiles(long farmfieldId) {
        try {
            Path farmfieldRootPath = Paths.get(FileService.getFarmUploadRootPath(farmfieldId));
            if (Files.exists(farmfieldRootPath)) {
                List<String> fileList = Files.walk(Paths.get(FileService.getFarmUploadRootPath(farmfieldId)))
                        .filter(Files::isRegularFile)
                        .map(FileService::getRelativeFarmfieldFilePath)
                        .collect(Collectors.toList());
                return fileList;
            }
        } catch (IOException e) {
            LOG.error(e, "Error while traversing path: " + FileService.getFarmUploadRootPath(farmfieldId));
        }
        return Collections.emptyList();
    }

    @Override
    public FarmfieldFile uploadLayerData(long farmfieldId, InputStream inputStream, FarmfieldFileDataType dataType, String originalFilename, String filename) {
        int year = Year.now().getValue();
        String uploadPath = FileService.getUploadPath(year, farmfieldId, dataType);
        String filePathString = uploadPath + filename;
        LOG.info("about upload file : " + filePathString);
        boolean success = false;
        Path newFile = Paths.get(filePathString);
        String fullPath = FileSystems.getDefault().getPath(newFile.toString()).normalize().toAbsolutePath().toString();
        LOG.info("full path " + fullPath);
        try {
            Files.createDirectories(newFile.getParent());
            Files.copy(inputStream, newFile);
            FarmfieldFile farmfieldFile = new FarmfieldFile();
            farmfieldFile.setFarmfieldId(farmfieldId);
            farmfieldFile.setFileDate(new Date());
            farmfieldFile.setOriginalFilename(originalFilename);
            farmfieldFile.setFullPath(fullPath);
            farmfieldFile.setType(dataType.getTypeId());
            long id = peltodataRepository.insertFarmfieldFile(farmfieldFile);
            return peltodataRepository.findFarmfieldFile(id);
        } catch (IOException e) {
            LOG.error(e, "Error occured while writing file: " + filePathString);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createFarmfieldLayer(long farmfieldId, long farmfieldFileId,
                                     FarmfieldFileDataType inputDataType, FarmfieldFileDataType outputDataType, User user) {
        LOG.info("createFarmfieldLayer id={} inputfile={} inputtype={} outputtype={}",
                farmfieldId, farmfieldFileId, inputDataType.toString(), outputDataType.toString());
        FarmfieldFile farmfieldFile = peltodataRepository.findFarmfieldFile(farmfieldFileId);
        Farmfield farmfield = findFarmfield(farmfieldId);

        if (farmfieldId != farmfieldFile.getFarmfieldId()) {
            throw new RuntimeException("invalid farmfield for file");
        }

        startAsyncFarmfieldLayerConversionCreation(farmfield, farmfieldFile, outputDataType, user);
    }

    protected void startAsyncFarmfieldLayerConversionCreation(Farmfield farmfield, FarmfieldFile farmfieldFile, FarmfieldFileDataType outputDataType, User user) {
        Path outputFilePath = getOutputFilePath(Paths.get(farmfieldFile.getFullPath()), outputDataType);
        try {
            Files.createDirectories(outputFilePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directory" + outputFilePath.getParent().toString(), e);
        }

        LOG.info("Starting async conversion of {} to {} type={}", farmfieldFile.getFullPath(), outputFilePath.toString(), outputDataType.toString());

        switch (outputDataType) {
            case CROP_ESTIMATION_RAW_DATA:
                RawConversionTask conversionTask = new RawConversionTask(this, farmfield, farmfieldFile, outputFilePath, outputDataType, user);
                executor.execute(conversionTask);
                break;
            case CROP_ESTIMATION_DATA:
                CropEstimationTask cropEstimationTask = new CropEstimationTask(this, farmfield, farmfieldFile, outputFilePath, outputDataType, user);
                executor.execute(cropEstimationTask);
                break;
            case YIELD_RAW_DATA:
                break;
            case YIELD_DATA:
                YieldImageTask yieldImageTask = new YieldImageTask(this, farmfield, farmfieldFile, outputFilePath, outputDataType, user);
                executor.execute(yieldImageTask);
                break;
        }
    }

    /**
     * Returns absolute and full path where output file should be stored
     * @param inputFilepath
     * @return
     */
    private Path getOutputFilePath(Path inputFilepath, FarmfieldFileDataType outputDataType) {
        Path root = inputFilepath.getParent();
        String timestamp = inputFilepath.getFileName().toString().split("__")[0];
        return Paths.get(root.toString(), timestamp + "__" + outputDataType.getTypeId() + "." + outputDataType.getDataFormat());
    }

    public String createFarmfieldGeoserverLayer(Farmfield farmfield, Path absolutePath, String outputType) {
        if (!Files.exists(absolutePath)) {
            throw new IllegalArgumentException("absolutePath does not exist " + absolutePath);
        }

        // check if layer exists ? throw, null or update if exists ??
        String wmsBaseUrl = geoserverClient.getWMSBaseUrl();
        String filename = absolutePath.getFileName().toString();
        filename = filename.substring(0, filename.indexOf('.'));
        String description = getCleanedUpDescription(farmfield);
        String mapLayerName = String.format("%s_%s_%s", description, filename, outputType);
        List<OskariLayer> layers = oskariLayerService.findByUrlAndName(wmsBaseUrl, mapLayerName);
        if (layers != null && layers.size() > 0) {
            throw new RuntimeException("layer exists");
        }

        try {
            String createdLayerName = geoserverClient.saveTiffAsDatastore(mapLayerName, absolutePath);
            return createdLayerName;
        } catch (GeoserverException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FarmfieldExecution> findAllFarmfieldExecutionsForUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userid cannot be null");
        }
        return peltodataRepository.findAllFarmfieldExecutionsForUser(userId);
    }

    @Override
    public List<FarmfieldExecution> findAllFarmfieldExecutions() {
        return peltodataRepository.findAllFarmfieldExecutions();
    }

    @Override
    public FarmfieldExecution farmfieldExecutionStarted(FarmfieldFile farmfieldFile, Path outputFilePath, String outputType) {
        FarmfieldExecution execution = new FarmfieldExecution();
        execution.setState(0);
        execution.setOutputType(outputType);
        execution.setFarmfieldId(farmfieldFile.getFarmfieldId());
        execution.setOutputFilename(outputFilePath.toString());
        execution.setFarmfieldFileId(farmfieldFile.getId());
        peltodataRepository.insertFarmfieldExecution(execution);
        return peltodataRepository.findFarmfieldExecution(execution.getId());
    }

    @Override
    public void farmfieldExecutionCompleted(FarmfieldExecution execution) {
        execution.setState(10);
        peltodataRepository.updateFarmfieldExecution(execution);
    }

    @Override
    public void farmfieldExecutionFailed(FarmfieldExecution execution) {
        execution.setState(-10);
        peltodataRepository.updateFarmfieldExecution(execution);
    }

    @Override
    public String getInputFilename(FarmfieldFileDataType dataType) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "__" + dataType.getFolderName() + "." + dataType.getDataFormat();
    }

    @Override
    public void addWMSLayerFromGeoserver(Farmfield farmfield, FarmfieldFile farmfieldFile, String layerName, FarmfieldFileDataType dataType, User user) {
        PeltodataOskariLayerService peltodataOskariLayerService = new PeltodataOskariLayerServiceImpl();

        LOG.info("finding provider for user " + user.getScreenname());
        DataProvider dataProvider = ensureDataProviderForUser(user);
        LOG.info("finding group for field " + farmfield.getDescription());
        MaplayerGroup maplayerGroup = ensureGroupForField(farmfield);

        LOG.info("add wmslayer from geoserver", layerName);

        try {
            User adminUser = userService.getUser("admin");
            // Set layer description to "12.09.2019 - <TYPE>"
            String layerDescription = String.format("%s - %s", new SimpleDateFormat("dd.MM.YYYY").format(farmfieldFile.getFileDate()), convertTypeToFinnishDescription(dataType));
            peltodataOskariLayerService.addWMSLayerFromGeoserver(layerDescription, maplayerGroup.getId(), dataProvider.getId(), layerName, geoserverClient.getWMSBaseUrl(), adminUser);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean farmfieldFileBelongsToFarmAndUser(Long fileId, Long farmFieldId, User user) {
        Farmfield farmfield = peltodataRepository.findFarmfield(farmFieldId);
        FarmfieldFile file = peltodataRepository.findFarmfieldFile(fileId);
        return file.getFarmfieldId() == farmfield.getId() && user.getId() == farmfield.getUserId();
    }

    @Override
    public FarmfieldFile findFarmfieldFile(Long fileId) {
        return peltodataRepository.findFarmfieldFile(fileId);
    }

    @Override
    public void updateFarmfieldFile(FarmfieldFile file) {
        peltodataRepository.updateFarmfieldFile(file);
    }

    private String convertTypeToFinnishDescription(FarmfieldFileDataType dataType) {
        switch (dataType) {
            case CROP_ESTIMATION_ORIGINAL_DATA:
            case CROP_ESTIMATION_RAW_DATA:
                return "Alkuperäinen";
            case CROP_ESTIMATION_DATA:
                return "Satoennuste";
            case YIELD_RAW_DATA:
                return "";
            case YIELD_DATA:
                return "Satokartta";
        }

        throw new RuntimeException("Missing datatype translation for " + dataType.getTypeId());
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
