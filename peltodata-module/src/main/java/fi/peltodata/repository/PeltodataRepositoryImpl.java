package fi.peltodata.repository;

import fi.nls.oskari.db.DatasourceHelper;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.mybatis.JSONObjectMybatisTypeHandler;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;
import fi.peltodata.domain.*;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.sql.Date;
import java.util.*;

public class PeltodataRepositoryImpl implements PeltodataRepository {
    private SqlSessionFactory factory;
    private static final Logger LOG = LogFactory.getLogger(PeltodataRepositoryImpl.class);
    private UserService userService;
    private OskariLayerService oskariLayerService;

    public PeltodataRepositoryImpl(UserService userService, OskariLayerService oskariLayerService) {
        this.userService = userService;
        this.oskariLayerService = oskariLayerService;
        final DatasourceHelper helper = DatasourceHelper.getInstance();
        DataSource dataSource = helper.getDataSource();
        if (dataSource == null) {
            dataSource = helper.createDataSource();
        }
        if (dataSource == null) {
            LOG.error("Couldn't get datasource for oskari layer service");
        }
        factory = initializeMyBatis(dataSource);
    }

    private SqlSessionFactory initializeMyBatis(final DataSource dataSource) {
        final TransactionFactory transactionFactory = new JdbcTransactionFactory();
        final Environment environment = new Environment("development", transactionFactory, dataSource);

        final Configuration configuration = new Configuration(environment);
        configuration.getTypeAliasRegistry().registerAlias(Farmfield.class);
        configuration.getTypeAliasRegistry().registerAlias(FarmfieldExecution.class);
        configuration.setLazyLoadingEnabled(true);
        configuration.getTypeHandlerRegistry().register(JSONObjectMybatisTypeHandler.class);
        configuration.addMapper(FarmfieldMapper.class);
        configuration.addMapper(FarmfieldExecutionMapper.class);
        configuration.addMapper(FarmfieldFileMapper.class);

        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private Farmfield mapData(Map<String, Object> data, SqlSession session) {
        if (data == null) {
            return null;
        }
        if (data.get("id") == null) {
            // this will make the keys case insensitive (needed for hsqldb compatibility...)
            final Map<String, Object> caseInsensitiveData = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitiveData.putAll(data);
            data = caseInsensitiveData;
        }
        final Farmfield farmfield = new Farmfield();
        final Long farmfieldId = (Long) data.get("id");
        farmfield.setId(farmfieldId);
        farmfield.setDescription((String) data.get("description"));
        farmfield.setCropType((String) data.get("crop_type"));
        farmfield.setFarmId((String) data.get("farm_id"));
        farmfield.setMapLayerGroupId((Integer) data.get("maplayergroup_id"));
        Date sqlDate = (Date) data.get("sowing_date");
        farmfield.setSowingDate(sqlDate.toLocalDate());
        try {
            int userIdInt = (int) data.get("user_id");
            User user = userService.getUser(userIdInt);
            farmfield.setUserId((long) userIdInt);
            farmfield.setUser(user);
        } catch (ServiceException e) {
            LOG.error("Could not find user with id {}", new Object[]{data.get("user_id")});
        }
        final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
        List<Integer> layerIds = mapper.findFarmLayers(farmfieldId);
        //not testable now on, using hsqldb (due to incompatibility in flyway scripts)
        List<OskariLayer> farmfieldLayers = oskariLayerService.findByIdList(layerIds);
        farmfield.setLayers(new HashSet<>(farmfieldLayers));
        return farmfield;
    }

    private List<Farmfield> mapDataList(final List<Map<String, Object>> list, SqlSession session) {
        final List<Farmfield> farmfields = new ArrayList<>();
        for (Map<String, Object> map : list) {
            final Farmfield farmfield = mapData(map, session);
            if (farmfield != null) {
                farmfields.add(farmfield);
            }
        }
        return farmfields;
    }

    @Override
    public List<Farmfield> findAllFarmFields() {
        long start = System.currentTimeMillis();
        try (SqlSession session = factory.openSession()) {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            List<Map<String, Object>> result = mapper.findAllFarmFields();
            LOG.debug("Find all fields:", System.currentTimeMillis() - start, "ms");
            start = System.currentTimeMillis();
            final List<Farmfield> farmfields = mapDataList(result, session);
            LOG.debug("Parsing all fields:", System.currentTimeMillis() - start, "ms");
            return farmfields;
        } catch (Exception e) {
            LOG.warn(e, "");
        }
        return Collections.emptyList();
    }

    @Override
    public Farmfield findFarmfield(long id) {
        LOG.debug("find by id: " + id);
        try (SqlSession session = factory.openSession()) {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            Map<String, Object> farmField1 = mapper.findFarmField(id);
            Farmfield farmField = mapData(farmField1, session);
            farmField.setUser(userService.getUser(farmField.getUserId()));
            return farmField;
        } catch (Exception e) {
            LOG.warn(e, "Exception when getting farmfield with id: " + id);
        }
        return null;
    }

    @Override
    public List<Farmfield> findAllFarmfieldsByUser(long userId) {
        long start = System.currentTimeMillis();
        try (SqlSession session = factory.openSession()) {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            List<Map<String, Object>> result = mapper.findAllFarmFieldsByUserId(userId);
            LOG.debug("Find all fields by user:", System.currentTimeMillis() - start, "ms");
            start = System.currentTimeMillis();
            final List<Farmfield> farmfields = mapDataList(result, session);
            LOG.debug("Parsing all fields by user:", System.currentTimeMillis() - start, "ms");
            return farmfields;
        } catch (Exception e) {
            LOG.warn(e, "");
        }
        return Collections.emptyList();
    }

    @Override
    public boolean farmfieldBelongsToUser(long farmfieldId, long userId) {
        LOG.debug("check if farmfield belongs to user");
        boolean belongsToUser;
        try (SqlSession session = factory.openSession()) {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            Map<String, Object> farmFieldData = mapper.findFarmField(farmfieldId);
            Farmfield farmfield = mapData(farmFieldData, session);
            belongsToUser = farmfield.getUserId().equals(userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check", e);
        }
        return belongsToUser;
    }

    @Override
    public long insertFarmfield(Farmfield farmfield) {
        LOG.debug("insert new farmfield");
        if (farmfield.getUser() != null) {
            long userId = farmfield.getUser().getId();
            farmfield.setUserId(userId);
        }
        try (SqlSession session = factory.openSession()) {
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
        }
        return farmfield.getId();
    }

    @Override
    public void updateFarmfield(Farmfield farmfield) {
        LOG.debug("update farmfield");
        try (SqlSession session = factory.openSession()) {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            mapper.updateFarmfield(farmfield);
            session.commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update", e);
        }
    }

    @Override
    public void deleteFarmfield(long id) {
        LOG.debug("delete farmfield with id: " + id);
        try (SqlSession session = factory.openSession()) {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            mapper.deleteFarmfield(id);
            session.commit();
        } catch (Exception e) {
            LOG.error(e, "Couldn't delete with id:", id);
        }
    }

    @Override
    public List<FarmfieldExecution> findAllFarmfieldExecutionsForUser(Long userId) {
        try (final SqlSession session = factory.openSession()) {
            final FarmfieldExecutionMapper mapper = session.getMapper(FarmfieldExecutionMapper.class);
            List<FarmfieldExecution> executions = mapper.findAllFarmFieldExecutionsForUser(userId);
            return executions;
        }
    }

    @Override
    public List<FarmfieldExecution> findAllFarmfieldExecutions() {
        try (final SqlSession session = factory.openSession()) {
            final FarmfieldExecutionMapper mapper = session.getMapper(FarmfieldExecutionMapper.class);
            List<FarmfieldExecution> executions = mapper.findAllFarmfieldExecutions();
            return executions;
        }
    }

    @Override
    public void insertFarmfieldExecution(FarmfieldExecution execution) {
        try (final SqlSession session = factory.openSession()) {
            final FarmfieldExecutionMapper mapper = session.getMapper(FarmfieldExecutionMapper.class);
            mapper.insertFarmfieldExecution(execution);
            session.commit();
        }
    }

    @Override
    public void updateFarmfieldExecution(FarmfieldExecution execution) {
        try (final SqlSession session = factory.openSession()) {
            final FarmfieldExecutionMapper mapper = session.getMapper(FarmfieldExecutionMapper.class);
            mapper.updateFarmfieldExecution(execution);
            session.commit();
        }
    }

    @Override
    public FarmfieldExecution findFarmfieldExecution(long id) {
        try (final SqlSession session = factory.openSession()) {
            final FarmfieldExecutionMapper mapper = session.getMapper(FarmfieldExecutionMapper.class);
            return mapper.findFarmfieldById(id);
        }
    }

    @Override
    public long insertFarmfieldFile(FarmfieldFile farmfieldFile) {
        try (final SqlSession session = factory.openSession()) {
            final FarmfieldFileMapper mapper = session.getMapper(FarmfieldFileMapper.class);
            mapper.insertFarmfieldFile(farmfieldFile);
            session.commit();
            return farmfieldFile.getId();
        }
    }

    @Override
    public FarmfieldFile findFarmfieldFile(long id) {
        try (final SqlSession session = factory.openSession()) {
            final FarmfieldFileMapper mapper = session.getMapper(FarmfieldFileMapper.class);
            return mapper.findFarmfieldFileById(id);
        }
    }

    @Override
    public void updateFarmfieldFile(FarmfieldFile file) {
        try (final SqlSession session = factory.openSession()) {
            final FarmfieldFileMapper mapper = session.getMapper(FarmfieldFileMapper.class);
            mapper.udpateFarmfieldFile(file);
            session.commit();
        }
    }

    @Override
    public void insertFarmFieldLayer(Long farmfieldId, int oskariLayerId) {
        try (final SqlSession session = factory.openSession()) {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            mapper.insertFarmFieldMapLayer(farmfieldId, oskariLayerId);
            session.commit();
        }
    }

    @Override
    public void deleteFarmfieldFilesForField(long farmfieldId) {
        try (SqlSession session = factory.openSession()) {
            final FarmfieldExecutionMapper executionMapper = session.getMapper(FarmfieldExecutionMapper.class);
            List<FarmfieldExecution> executions = executionMapper.findAllFarmFieldExecutionsForFarm(farmfieldId);
            executions.stream().map(FarmfieldExecution::getId).forEach(executionMapper::deleteFarmfieldExecution);

            final FarmfieldFileMapper fileMapper = session.getMapper(FarmfieldFileMapper.class);
            List<FarmfieldFile> files = fileMapper.findFarmfieldFileForFarm(farmfieldId);
            files.stream().map(FarmfieldFile::getId).forEach(fileMapper::deleteFarmfieldFile);


            session.commit();
        }
    }
}
