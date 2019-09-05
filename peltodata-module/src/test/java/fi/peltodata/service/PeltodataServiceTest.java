package fi.peltodata.service;

import fi.mml.map.mapwindow.service.db.OskariMapLayerGroupService;
import fi.nls.oskari.db.DBHandler;
import fi.nls.oskari.db.DatasourceHelper;
import fi.nls.oskari.db.FlywaydbMigrator;
import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.DataProvider;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.map.layer.DataProviderService;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.util.DuplicateException;
import fi.nls.oskari.util.PropertyUtil;
import fi.peltodata.domain.Farmfield;
import fi.peltodata.repository.PeltodataRepository;
import fi.peltodata.repository.PeltodataRepositoryImpl;
import org.apache.commons.dbcp2.BasicDataSource;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import javax.naming.NamingException;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class PeltodataServiceTest {

    private PeltodataService peltodataService = null;
    private OskariLayerService oskariLayerService = null;
    private OskariMapLayerGroupService oskariMapLayerGroupService = null;
    private DataProviderService dataProviderService = null;

    public PeltodataServiceTest() throws ServiceException {
    }

    @BeforeClass
    public static void init() throws DuplicateException, NamingException {
        // similar flow like in webapphelper

        // 1. set context
        PropertyUtil.loadProperties("/oskari-ext.properties");
        final DatasourceHelper helper = DatasourceHelper.getInstance();
        BasicDataSource datasource = helper.createDataSource();

        SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
        builder.bind("java:comp/env/jdbc/OskariPool", datasource);// to make user and role-services work
        builder.bind("java:/comp/env/jdbc/OskariPool", datasource);// to make OskariMapLayerGroupService work

        // 2. create content
        DBHandler.createContentIfNotCreated(datasource);
        // 3. call migrations (fails ATM)
        //failures running oskari-migrations with hsql and h2
        // flyway\oskari\V1_31_5_1__fix_sequence_on_portti_bundle.sql (syntax not supported)
        // create-base-tables.sql contains already portti_view.metadata TEXT DEFAULT '{}'
        // flyway/oskari/V1_32_0__new_capabilities_cache.sql contains MATCH (not supported by h2)
        //FlywaydbMigrator.migrate(datasource);

        // 4. call additional migrations
        FlywaydbMigrator.migrate(datasource, "peltodata");
    }

    @Before
    public void setUp() throws ServiceException {
        oskariMapLayerGroupService = Mockito.mock(OskariMapLayerGroupService.class);//cannot use real migrate fails
        dataProviderService = Mockito.mock(DataProviderService.class);//cannot use real migrate fails
        peltodataService = new PeltodataServiceMybatisImpl(oskariMapLayerGroupService, dataProviderService);
    }

    @Test
    public void testAdd() throws ServiceException, JSONException {
        Farmfield farmFieldForNewUser = createFarmFieldForNewUser();
        Farmfield field = peltodataService.findFarmfield(farmFieldForNewUser.getId());
        assertEquals(field.getDescription(), farmFieldForNewUser.getDescription());
        assertEquals(field.getCropType(), farmFieldForNewUser.getCropType());
        assertEquals(field.getSowingDate(), farmFieldForNewUser.getSowingDate());
        assertNotNull(field.getUser().getScreenname());
        // OSKARI_MAPLAYER_GROUP does not exist see #init problems with migrate
/*        assertNotNull(field.getUser().getScreenname());
        MaplayerGroup group = oskariMapLayerGroupService.findByName(field.getDescription().toLowerCase());
        assertNotNull(group);*/

        Mockito.verify(dataProviderService, Mockito.atMost(1)).insert(Mockito.any());
        Mockito.verify(dataProviderService)
                .insert(Mockito.argThat(dataProvider ->
                        dataProvider.getNames().get("fi").equals(farmFieldForNewUser.getUser().getScreenname())));
    }

    @Test
    public void testAddAsIfDataProviderExists() throws ServiceException, JSONException {
        Mockito.doReturn(new DataProvider()).when(dataProviderService).findByName(Mockito.any());

        Farmfield farmFieldForNewUser = createFarmFieldForNewUser();
        Farmfield field = peltodataService.findFarmfield(farmFieldForNewUser.getId());
        assertEquals(field.getDescription(), farmFieldForNewUser.getDescription());
        assertEquals(field.getCropType(), farmFieldForNewUser.getCropType());
        assertEquals(field.getSowingDate(), farmFieldForNewUser.getSowingDate());
        assertNotNull(field.getUser().getScreenname());

        Mockito.verify(dataProviderService, Mockito.never()).insert(Mockito.any());
    }

    @Test
    public void testUpdate() throws ServiceException, JSONException {
        Farmfield farmFieldForNewUser = createFarmFieldForNewUser();
        Farmfield field = peltodataService.findFarmfield(farmFieldForNewUser.getId());
        User user = new User();
        user.setScreenname("new user 123");
        user.addRole(Role.getDefaultUserRole());
        User newUser = UserService.getInstance().createUser(user);

        field.setDescription("Test field name2");
        field.setCropType("oat");
        field.setSowingDate(LocalDate.of(2019, 6, 1));
        field.setUser(newUser);

        peltodataService.updateFarmfield(field);
        Farmfield updatedField = peltodataService.findFarmfield(field.getId());
        assertEquals("Test field name2", updatedField.getDescription());
        assertEquals("oat", updatedField.getCropType());
        assertEquals(LocalDate.of(2019,6,1), updatedField.getSowingDate());
        assertNotNull(updatedField.getUser().getScreenname());
    }

    private Farmfield createFarmFieldForUser(long userId) throws ServiceException, JSONException {
        User user = null;
        int i = new Random().nextInt();
        if (userId == -1) {
            user = new User();
            user.setScreenname("Random user" + i);
            UserService.getInstance().createUser(user);
        } else {
            user = UserService.getInstance().getUser(userId);
        }

        Farmfield farmfield = new Farmfield();
        farmfield.setDescription("Mäkelänvainio " + i);
        farmfield.setUser(user);
        farmfield.setCropType("rye");
        farmfield.setSowingDate(LocalDate.of(2019, 4, 1));

        peltodataService.insertFarmfield(farmfield);

        //farmfield.addLayer(createLayer(i));
        return farmfield;
    }

    private Farmfield createFarmFieldForNewUser() throws ServiceException, JSONException {
        return createFarmFieldForUser(-1);
    }

    /**
     * Cannot be used as hsqldb schema is not up-to-date due flyway scripts of content-resources module
     */
    private OskariLayer createLayer(int i) throws JSONException {
        // Layer for farmfield
        OskariLayer oskariLayer = new OskariLayer();
        oskariLayer.setType("wmslayer");
        oskariLayer.setBaseMap(false);
        oskariLayer.setDataproviderId(2);//demo
        oskariLayer.setName("raakadata_mäkelänvainio " + i);
        oskariLayer.setUrl("http://localhost:8080/geoserver/oskari/wms");
        oskariLayer.setOpacity(100);
        oskariLayer.setStyle("raster");
        oskariLayer.setMinScale(-1d);
        oskariLayer.setMaxScale(-1d);
        oskariLayer.setLegendImage("");
        oskariLayer.setRealtime(false);
        oskariLayer.setRefreshRate(0);
        oskariLayer.setUsername("");
        oskariLayer.setPassword("");
        //oskariLayer.setInternal(false);
        String localeString = "{\"fi\":{\"subtitle\":\"\",\"name\":\"\"raakadata_mäkelänvainio " + i + "\"}}";
        //oskariLayer.setLocale(new JSONObject(localeString));
        String capabilities = "{\"formats\":{\"available\":[" +
                "\"text/html\",\"text/plain\",\"application/vnd.ogc.gml\",\"text/xml\"" +
                "]," +
                "\"value\":\"text/html\"}," +
                "\"srs\":[\"EPSG:3857\"]," +
                "\"isQueryable\":true," +
                "\"styles\":[" +
                "{\"legend\":\"http://localhost:8080/geoserver/oskari/ows?service=WMS&request=GetLegendGraphic&format=image%2Fpng&width=20&height=20&layer=suomi1m_tm35fin\"," +
                "\"name\":\"raster\",\"title\":\"A boring default style\"}" +
                "]," +
                "\"geom\":\"POLYGON ((14.508263845033992 59.24088360176308, 14.508263845033992 70.17412249609963, 33.568614618356975 70.17412249609963, 33.568614618356975 59.24088360176308, 14.508263845033992 59.24088360176308))\"," +
                "\"version\":\"1.3.0\"}";
        oskariLayer.setCapabilities(new JSONObject(capabilities));
        oskariLayerService.insert(oskariLayer);
        return  oskariLayer;
    }

    @Test
    public void testFindAll() throws ServiceException, JSONException {
        createFarmFieldForNewUser();
        createFarmFieldForNewUser();
        createFarmFieldForNewUser();
        List<Farmfield> all = peltodataService.findAllFarmfields();
        assertTrue(all.size() >= 3);
        assertNotNull(all.get(0).getUser().getScreenname());
    }

    @Test
    public void testFindAllByUserId() throws ServiceException, JSONException {
        Farmfield farmFieldForNewUser = createFarmFieldForNewUser();
        long userId = farmFieldForNewUser.getUserId();
        createFarmFieldForUser(userId);
        List<Farmfield> all = peltodataService.findAllFarmfieldsByUser(userId);
        assertTrue("" + all.size(), all.size() == 2);
        assertEquals(userId, all.get(1).getUser().getId());
    }

    @Test
    public void testDelete() throws ServiceException, JSONException {
        Farmfield farmFieldForNewUser = createFarmFieldForNewUser();
        Long id = farmFieldForNewUser.getId();
        peltodataService.deleteFarmfield(farmFieldForNewUser);
        Farmfield farmfield = peltodataService.findFarmfield(id);
        assertNull(farmfield);
    }
}
