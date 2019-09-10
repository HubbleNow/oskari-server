package fi.peltodata.service;

import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.OskariLayer;

public interface PeltodataOskariLayerService {
    OskariLayer addWMSLayerFromGeoserver(String name, int groupId, int dataProviderId, String geoserverLayerName, String geoserverUrl, User user);
}
