package fi.peltodata.service;

import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.layer.SaveLayerHandler;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceMybatisImpl;
import fi.peltodata.InsertLayerActionParams;

public class PeltodataOskariLayerServiceImpl implements PeltodataOskariLayerService {
    private OskariLayerService oskariLayerService;

    public PeltodataOskariLayerServiceImpl() {
        oskariLayerService = new OskariLayerServiceMybatisImpl();
    }

    @Override
    public OskariLayer addWMSLayerFromGeoserver(String name, int groupId, int dataProviderId, String geoserverLayerName, String geoserverUrl, User user) {
        ActionParameters actionParams = new InsertLayerActionParams(user, groupId, dataProviderId, name, geoserverLayerName, geoserverUrl);
        SaveLayerHandler saveLayerHandler = new SaveLayerHandler();
        try {
            SaveLayerHandler.SaveResult saveResult = saveLayerHandler.saveLayer(actionParams);
            OskariLayer oskariLayer = oskariLayerService.find((int) saveResult.getLayerId());
            return oskariLayer;
        } catch (ActionException e) {
            throw new RuntimeException("Layer add failed", e);
        }
    }
}
