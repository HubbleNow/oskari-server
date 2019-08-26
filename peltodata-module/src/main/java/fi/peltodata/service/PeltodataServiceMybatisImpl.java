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
import fi.peltodata.domain.Farmfield;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.util.*;

@Oskari
public class PeltodataServiceMybatisImpl extends OskariComponent implements PeltodataService  {

    private static final Logger LOG = LogFactory.getLogger(PeltodataServiceMybatisImpl.class);

    private SqlSessionFactory factory;

    private static OskariMapLayerGroupService oskariMapLayerGroupService = new OskariMapLayerGroupServiceIbatisImpl();
    private static OskariLayerService oskariLayerService = new OskariLayerServiceMybatisImpl();
    private UserService userService;

    public PeltodataServiceMybatisImpl() throws ServiceException {
        final DatasourceHelper helper = DatasourceHelper.getInstance();
        DataSource dataSource = helper.getDataSource();
        if (dataSource == null) {
            dataSource = helper.createDataSource();
        }
        if (dataSource == null) {
            LOG.error("Couldn't get datasource for oskari layer service");
        }
        factory = initializeMyBatis(dataSource);
        userService = UserService.getInstance();
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

    private Farmfield mapData(Map<String, Object> data) {
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
        farmfield.setId((Long) data.get("id"));
        farmfield.setDescription((String) data.get("description"));
        try {
            User user = userService.getUser((int) data.get("user_id"));
            farmfield.setUser(user);
        } catch (ServiceException e) {
            LOG.error("Could not find user with id {}", new Object[] { data.get("user_id") });
        }
        // todo: how to fetch more simpler all fields
        // using OskariLayer layer = oskariLayerService.find((int) data.get("layer_id"));
        return farmfield;
    }

    private List<Farmfield> mapDataList(final List<Map<String,Object>> list) {
        final List<Farmfield> farmfields = new ArrayList<>();
        for(Map<String, Object> map : list) {
            final Farmfield farmfield = mapData(map);
            if(farmfield != null) {
                farmfields.add(farmfield);
            }
        }
        return farmfields;
    }

    @Override
    public Farmfield find(long id) {
        LOG.debug("find by id: " + id);
        final SqlSession session = factory.openSession();
        try {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            // get as list since we might have a collection layer (get sublayers with same query)
            final List<Farmfield> farmfields =  mapDataList(mapper.findFarmField(id));
            if(farmfields != null && !farmfields.isEmpty()) {
                // should we check for multiples? only should have one since sublayers are mapped in mapDataList()
                return farmfields.get(0);
            }
        } catch (Exception e) {
            LOG.warn(e, "Exception when getting layer with id: " + id);
        } finally {
            session.close();
        }
        return null;
    }

    @Override
    public List<Farmfield> findAll() {
        long start = System.currentTimeMillis();
        final SqlSession session = factory.openSession();
        try {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            List<Map<String,Object>> result = mapper.findAllFarmFields();
            LOG.debug("Find all fields:", System.currentTimeMillis() - start, "ms");
            start = System.currentTimeMillis();
            final List<Farmfield> farmfields = mapDataList(result);
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
    public void update(final Farmfield farmfield) {
        LOG.debug("update farmfield");
        final SqlSession session = factory.openSession();
        try {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            mapper.update(farmfield);
            session.commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update", e);
        } finally {
            session.close();
        }
    }

    @Override
    public synchronized long insert(final Farmfield farmfield) {
        LOG.debug("insert new farmfield");
        final SqlSession session = factory.openSession();
        try {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            mapper.insertFarmField(farmfield);
            session.commit();
            //id exists now
            Long id = farmfield.getId();
            for (OskariLayer layer : farmfield.getLayers()) {
                mapper.insertFarmFieldMapLayer(id, layer.getId());
                session.commit();
            }
            // or is one or two commits enough ???
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert", e);
        } finally {
            session.close();
        }
        return farmfield.getId();
    }

    @Override
    public void delete(long id) {
        LOG.debug("delete farmfield with id: " + id);
        final SqlSession session = factory.openSession();
        try {
            final FarmfieldMapper mapper = session.getMapper(FarmfieldMapper.class);
            mapper.delete(id);
            session.commit();
        } catch (Exception e) {
            LOG.error(e, "Couldn't delete with id:", id);
        } finally {
            session.close();
        }
    }
}
