package fi.peltodata.service;

import fi.nls.oskari.db.DBHandler;
import fi.nls.oskari.db.DatasourceHelper;
import fi.nls.oskari.db.FlywaydbMigrator;
import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceMybatisImpl;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.util.DuplicateException;
import fi.nls.oskari.util.PropertyUtil;
import fi.peltodata.domain.Farmfield;
import org.apache.commons.dbcp2.BasicDataSource;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import javax.naming.NamingException;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class PeltodataServiceTest {

    private PeltodataService peltodataService = null;
    private OskariLayerService oskariLayerService = null;

    public PeltodataServiceTest() throws ServiceException {
    }

    @BeforeClass
    public static void init() throws DuplicateException, NamingException {
        PropertyUtil.loadProperties("/oskari-ext.properties");
        final DatasourceHelper helper = DatasourceHelper.getInstance();
        BasicDataSource datasource = helper.createDataSource();

        SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
        builder.bind("java:comp/env/jdbc/OskariPool", datasource);// to make user and role-services work

        DBHandler.createContentIfNotCreated(datasource);
        FlywaydbMigrator.migrate(datasource, "peltodata");
    }

    @Before
    public void setUp() throws ServiceException {
        peltodataService = new PeltodataServiceMybatisImpl();
        oskariLayerService = new OskariLayerServiceMybatisImpl();
    }

    @Test
    public void testAdd() throws ServiceException, JSONException {
        Farmfield farmFieldForNewUser = createFarmFieldForNewUser();
        Farmfield field = peltodataService.find(farmFieldForNewUser.getId());
        assertEquals(field.getDescription(), farmFieldForNewUser.getDescription());
        assertNotNull(field.getUser().getScreenname());
    }

    @Test
    public void testUpdate() throws ServiceException, JSONException {
        Farmfield farmFieldForNewUser = createFarmFieldForNewUser();
        Farmfield field = peltodataService.find(farmFieldForNewUser.getId());
        User user = new User();
        user.setScreenname("new user 123");
        user.addRole(Role.getDefaultUserRole());
        User newUser = UserService.getInstance().createUser(user);

        field.setDescription("Test field name");
        field.setUser(newUser);

        peltodataService.update(field);
        Farmfield updatedField = peltodataService.find(field.getId());
        assertEquals("Test field name", updatedField.getDescription());
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

        peltodataService.insert(farmfield);

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
        List<Farmfield> all = peltodataService.findAll();
        assertTrue(all.size() >= 3);
        assertNotNull(all.get(0).getUser().getScreenname());
    }

    @Test
    public void testFindAllByUserId() throws ServiceException, JSONException {
        Farmfield farmFieldForNewUser = createFarmFieldForNewUser();
        long userId = farmFieldForNewUser.getUserId();
        createFarmFieldForUser(userId);
        List<Farmfield> all = peltodataService.findAllByUser(userId);
        assertTrue("" + all.size(), all.size() == 2);
        assertEquals(userId, all.get(1).getUser().getId());
    }

    @Test
    public void testDelete() throws ServiceException, JSONException {
        Farmfield farmFieldForNewUser = createFarmFieldForNewUser();
        Long id = farmFieldForNewUser.getId();
        peltodataService.delete(farmFieldForNewUser);
        Farmfield farmfield = peltodataService.find(id);
        assertNull(farmfield);
    }
}
