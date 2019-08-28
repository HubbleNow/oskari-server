package fi.peltodata.geoserver;

import fi.nls.oskari.util.PropertyUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class GeoserverClient {
    private static final String PROP_GS_URL = "geoserver.url";
    private static final String PROP_GS_USER = "geoserver.user";
    private static final String PROP_GS_PASS = "geoserver.password";

    public void saveTiffAsDatastore(String workspace, String datastoreName, Path tiffFile) throws GeoserverException {
        String endPoint = PropertyUtil.get(PROP_GS_URL, "http://localhost:8080/geoserver");
        String user = PropertyUtil.get(PROP_GS_USER, "admin");
        String pass = PropertyUtil.get(PROP_GS_PASS, "geoserver");

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
        HttpPut put = new HttpPut(uri);
        FileEntity fileEntity = new FileEntity(tiffFile.toFile());
        put.setEntity(fileEntity);
        put.setHeader("Content-Type", "image/tiff");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(put)) {}
        } catch (IOException e) {
            throw new GeoserverException(e);
        }
    }
}
