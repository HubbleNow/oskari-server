package fi.peltodata.service;

import fi.mml.map.mapwindow.service.db.OskariMapLayerGroupService;
import fi.mml.map.mapwindow.service.db.OskariMapLayerGroupServiceIbatisImpl;
import fi.nls.oskari.annotation.Oskari;
import fi.nls.oskari.db.DatasourceHelper;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceMybatisImpl;
import fi.nls.oskari.mybatis.JSONObjectMybatisTypeHandler;
import fi.nls.oskari.service.OskariComponent;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.util.PropertyUtil;
import fi.peltodata.config.PeltodataConfig;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.domain.FarmfieldFileDataType;
import fi.peltodata.domain.FarmfieldMapper;
import fi.peltodata.geoserver.GeoserverClient;
import fi.peltodata.geoserver.GeoserverException;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Oskari
public class PeltodataServiceMybatisImpl extends OskariComponent implements PeltodataService  {

    private static final Logger LOG = LogFactory.getLogger(PeltodataServiceMybatisImpl.class);

    private SqlSessionFactory factory;

    private static OskariMapLayerGroupService oskariMapLayerGroupService = new OskariMapLayerGroupServiceIbatisImpl();
    private static OskariLayerService oskariLayerService = new OskariLayerServiceMybatisImpl();
    private UserService userService;

    private GeoserverClient geoserverClient;

    protected PeltodataServiceMybatisImpl(UserService userService, GeoserverClient geoserverClient) throws ServiceException {
        final DatasourceHelper helper = DatasourceHelper.getInstance();
        DataSource dataSource = helper.getDataSource();
        if (dataSource == null) {
            dataSource = helper.createDataSource();
        }
        if (dataSource == null) {
            LOG.error("Couldn't get datasource for oskari layer service");
        }
        factory = initializeMyBatis(dataSource);
        this.userService = userService;
        this.geoserverClient = geoserverClient;
    }

    public PeltodataServiceMybatisImpl() throws ServiceException {
        this(UserService.getInstance(), new GeoserverClient());
    }

    private SqlSessionFactory initializeMyBatis(final DataSource dataSource) {
        final TransactionFactory transactionFactory = new JdbcTransactionFactory();
        final Environment environment = new Environment("development", transactionFactory, dataSource);

        final Configuration configuration = new Configuration(environment);
        configuration.getTypeAliasRegistry().registerAlias(Farmfield.class);
        configuration.setLazyLoadingEnabled(true);
        configuration.getTypeHandlerRegistry().register(JSONObjectMybatisTypeHandler.class);
        configuration.addMapper(FarmfieldMapper.class);

        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private Farmfield mapData(Map<String, Object> data, SqlSession session) {
        if(data == null) {
            return null;
        }
        if(data.get("id") == null) {
            // this will make the keys case insensitive (needed for hsqldb compatibility...)
            final Map<String, Object> caseInsensitiveData = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitiveData.putAll(data);
            data = caseInsensitiveData;
        }
        final Farmfield farmfield = new Farmfield();
        final Long farmfieldId = (Long) data.get("id");
        farmfield.setId(farmfieldId);
        farmfield.setDescription((String) data.get("description"));
        try {
            int userIdInt = (int) data.get("user_id");
            Long userId = new Long(userIdInt);
            User user = userService.getUser(userId);
            farmfield.setUserId(userId);
            farmfield.setUser(user);
        } catch (ServiceException e) {
            LOG.error("Could not find user with id {}", new Object[] { data.get("user_id") });
        }
        final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
        List<Integer> layerIds = mapper.findFarmLayers(farmfieldId);
        //not testable now on, using hsqldb (due to incompatibility in flyway scripts)
        List<OskariLayer> farmfieldLayers = oskariLayerService.findByIdList(layerIds);
        farmfield.setLayers(farmfieldLayers.stream().collect(Collectors.toSet()));
        return farmfield;
    }

    private List<Farmfield> mapDataList(final List<Map<String,Object>> list, SqlSession session) {
        final List<Farmfield> farmfields = new ArrayList<>();
        for(Map<String, Object> map : list) {
            final Farmfield farmfield = mapData(map, session);
            if(farmfield != null) {
                farmfields.add(farmfield);
            }
        }
        return farmfields;
    }

    @Override
    public Farmfield findFarmfield(long id) {
        LOG.debug("find by id: " + id);
        final SqlSession session = factory.openSession();
        try {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            Map<String, Object> farmField1 = mapper.findFarmField(id);
            Farmfield farmField = mapData(farmField1, session);
            farmField.setUser(userService.getUser(farmField.getUserId()));
            return farmField;
        } catch (Exception e) {
            LOG.warn(e, "Exception when getting farmfield with id: " + id);
        } finally {
            session.close();
        }
        return null;
    }

    @Override
    public List<Farmfield> findAllFarmfields() {
        long start = System.currentTimeMillis();
        final SqlSession session = factory.openSession();
        try {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            List<Map<String,Object>> result = mapper.findAllFarmFields();
            LOG.debug("Find all fields:", System.currentTimeMillis() - start, "ms");
            start = System.currentTimeMillis();
            final List<Farmfield> farmfields = mapDataList(result, session);
            LOG.debug("Parsing all fields:", System.currentTimeMillis() - start, "ms");
            return farmfields;
        } catch (Exception e) {
            LOG.warn(e, "");
        } finally {
            session.close();
        }
        return Collections.emptyList();
    }

    @Override
    public List<Farmfield> findAllFarmfieldsByUser(long userId) {
        long start = System.currentTimeMillis();
        final SqlSession session = factory.openSession();
        try {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            List<Map<String,Object>> result = mapper.findAllFarmFieldsByUserId(userId);
            LOG.debug("Find all fields by user:", System.currentTimeMillis() - start, "ms");
            start = System.currentTimeMillis();
            final List<Farmfield> farmfields = mapDataList(result, session);
            LOG.debug("Parsing all fields by user:", System.currentTimeMillis() - start, "ms");
            return farmfields;
        } catch (Exception e) {
            LOG.warn(e, "");
        } finally {
            session.close();
        }
        return Collections.emptyList();
    }

    @Override
    public boolean farmfieldBelongsToUser(long farmfieldId, long userId) {
        LOG.debug("check if farmfield belongs to user");
        final SqlSession session = factory.openSession();
        boolean belongsToUser = false;
        try {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            Map<String, Object> farmFieldData = mapper.findFarmField(farmfieldId);
            Farmfield farmfield = mapData(farmFieldData, session);
            belongsToUser = farmfield.getUserId().equals(userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check", e);
        } finally {
            session.close();
        }
        return belongsToUser;
    }

    @Override
    public void updateFarmfield(final Farmfield farmfield) {
        LOG.debug("update farmfield");
        final SqlSession session = factory.openSession();
        try {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            mapper.updateFarmfield(farmfield);
            session.commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update", e);
        } finally {
            session.close();
        }
    }

    @Override
    public synchronized long insertFarmfield(final Farmfield farmfield) {
        LOG.debug("insert new farmfield");
        if (farmfield.getUser() != null) {
            long userId = farmfield.getUser().getId();
            farmfield.setUserId(userId);
        }
        final SqlSession session = factory.openSession();
        try {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            mapper.insertFarmField(farmfield);
            //id exists now
            Long id = farmfield.getId();
            for (OskariLayer layer : farmfield.getLayers()) {
                mapper.insertFarmFieldMapLayer(id, layer.getId());
            }
            session.commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert", e);
        } finally {
            session.close();
        }
        return farmfield.getId();
    }

    @Override
    public void deleteFarmfield(long id) {
        LOG.debug("delete farmfield with id: " + id);
        final SqlSession session = factory.openSession();
        try {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            mapper.deleteFarmfield(id);
            session.commit();
        } catch (Exception e) {
            LOG.error(e, "Couldn't delete with id:", id);
        } finally {
            session.close();
        }
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
                        .map(a -> getRelativeFarmfieldFilePath(a))
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
        LOG.debug("about upload file : " + filePathString);
        boolean success = false;
        Path newFile = Paths.get(filePathString);
        try {
            Files.createDirectories(newFile.getParent());
            Files.copy(inputStream, newFile);
            success = true;
        } catch (IOException e) {
            LOG.error(e, "Error occured while writing file: " + filePathString);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return success ? getRelativeFarmfieldFilePath(newFile) : null;
    }

    @Override
    public String createFarmfieldLayer(long farmfieldId, String inputFilepath,
                                       FarmfieldFileDataType inputDataType, FarmfieldFileDataType outputDataType) {
        String filePath = getFarmUploadRootPath(farmfieldId) + inputFilepath;//need to use relative to geoserver or not?
        Farmfield farmfield = findFarmfield(farmfieldId);
        String geoserverLayerName = createFarmfieldGeoserverLayer(farmfield, inputFilepath, outputDataType);
        //create geoserver datastore + publish + wms layer (srs?)
        // investigate if web-api or !!!! { fi.nls.oskari.control.layer.SaveLayerHandler.saveLayer } could be used instead (--> decoupling out of this service)
        // minimum inputs (layername, groups, permissions, srs?, force-proxy ?)
        //  --- create layer
        //  --- assign permissions for user 2
        //add peltodata_field_layer row

        return geoserverLayerName;
    }

    protected void startAsyncFarmfieldLayerConversionCreation(long farmfieldId, String inputFilepath, FarmfieldFileDataType outputDataType) {
        // call python (sync??)
        // add to queue (singleton) which is read by
    }

    protected String createFarmfieldGeoserverLayer(Farmfield farmfield, String inputFilepath, FarmfieldFileDataType outputDataType) {
        // check if layer exists ? throw, null or update if exists ??
        String wmsBaseUrl = geoserverClient.getWMSBaseUrl();
        String filename = Paths.get(inputFilepath).getFileName().toString();
        filename = filename.substring(0, filename.indexOf('.'));
        String description = farmfield.getDescription().toLowerCase();
        char[] forbidden = new char[] {'ä', 'ö', ' '};
        char[] converted = new char[] {'a', 'o', '_'};
        for (int i = 0; i < description.length();  i++) {
            for(int j = 0; j < forbidden.length;  j++) {
                if (description.charAt(i) == forbidden[j]) {
                    description = description.replace(forbidden[j], converted[j]);
                }
            }
        }
        String maplayerName = description+"_"+filename;// == datastorename ??
        List<OskariLayer> layers = oskariLayerService.findByUrlAndName(wmsBaseUrl, maplayerName);
        if (layers != null && layers.size() > 0) {
            throw new RuntimeException("layer exists");
        } else {
            String farmUploadPathRelative = "."
                    + File.separator + "farms"
                    + File.separator + farmfield.getId()
                    + File.separator;
            //String fullPath = getFarmUploadRootPath(farmfield.getId()) + inputFilepath;
            String fullPath = farmUploadPathRelative + inputFilepath;
            try {
                geoserverClient.saveTiffAsDatastore(maplayerName, Paths.get(fullPath));
                return maplayerName;
            } catch (GeoserverException e) {
                return null;
            }
        }
    }

}
