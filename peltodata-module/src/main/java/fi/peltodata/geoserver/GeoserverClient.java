package fi.peltodata.geoserver;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.PropertyUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Path;

public class GeoserverClient {
    private static final Logger LOG = LogFactory.getLogger(GeoserverClient.class);

    private static final String PROP_GS_URL = "geoserver.url";
    private static final String PROP_GS_USER = "geoserver.user";
    private static final String PROP_GS_PASS = "geoserver.password";
    private static final String PROP_GS_PELTODATA_WS_NAME = "geoserver.peltodata.workspace";//"raster" in prod

    public void saveTiffAsDatastore(String datastoreName, Path tiffFile) throws GeoserverException {
        saveTiffAsDatastore(getDefaultWorkspaceName(), datastoreName, tiffFile, true);
    }

    public void saveTiffAsDatastore(String workspace, String datastoreName, Path tiffFile, boolean useExistingFile) throws GeoserverException {
        String endPoint = getBaseUrl();
        String user = PropertyUtil.get(PROP_GS_USER, "admin");
        String pass = PropertyUtil.get(PROP_GS_PASS, "geoserver");
        HttpPut put;
        if (!useExistingFile) {
            String path = String.format("/rest/workspaces/%s/coveragestores/%s/file.geotiff", workspace, datastoreName);
            String baseUri = endPoint + path;
            URI uri;
            try {
                uri = new URIBuilder(baseUri)
                        .setUserInfo(user, pass)
                        .build();
            } catch (URISyntaxException e) {
                throw new GeoserverException(e);
            }
            put = new HttpPut(uri);
            FileEntity fileEntity = new FileEntity(tiffFile.toFile());
            put.setEntity(fileEntity);
            put.setHeader("Content-Type", "image/tiff");
        } else {
            String path = null;
            try {
                path = String.format("/rest/workspaces/%s/coveragestores/%s/external.geotiff?filename=%s", workspace, datastoreName, URLEncoder.encode(tiffFile.toFile().toURI().toString(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new GeoserverException(e);
            }
            String baseUri = endPoint + path;
            URI uri;
            try {
                uri = new URIBuilder(baseUri)
                        .setUserInfo(user, pass)
                        .build();
            } catch (URISyntaxException e) {
                throw new GeoserverException(e);
            }
            put = new HttpPut(uri);
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse response = client.execute(put);
            LOG.debug("response", response);
        } catch (IOException e) {
            throw new GeoserverException(e);
        }
    }

    public String getBaseUrl() {
        return PropertyUtil.get(PROP_GS_URL, "http://localhost:8080/geoserver");
    }

    public String getDefaultWorkspaceName () {
        return PropertyUtil.get(PROP_GS_PELTODATA_WS_NAME, "oskari");
    }

    public String getWMSBaseUrl () {
        return getBaseUrl() + "/" + getDefaultWorkspaceName() + "/wms";
    }
}
