package fi.peltodata;

import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.util.RequestHelper;

import java.util.*;
import java.util.stream.Collectors;

public class InsertLayerActionParams extends ActionParameters {
    private final int groupId;
    private final int dataProviderId;
    private final String name;
    private final String geoserverLayerName;
    private String geoserverLayerUrl;
    private Map<String, String> objects;

    public InsertLayerActionParams(User user, int groupId, int dataProviderId, String name, String geoserverLayerName, String geoserverLayerUrl) {
        this.groupId = groupId;
        this.dataProviderId = dataProviderId;
        this.name = name;
        this.geoserverLayerName = geoserverLayerName;
        this.geoserverLayerUrl = geoserverLayerUrl;
        this.objects = new HashMap<>();
        this.setUser(user);

        this.setParams();
    }

    @Override
    public String getHttpParam(String key) {
        return RequestHelper.cleanString(this.objects.get(key));
    }

    @Override
    public String getHttpParam(String key, String defaultValue) {
        return RequestHelper.getString(this.objects.get(key), defaultValue);
    }

    @Override
    public Enumeration<String> getHttpParamNames() {
        return Collections.enumeration(this.objects.keySet());
    }

    private void setParams() {
        objects.put(ActionParamsConstants.PARAM_LAYER_ID, "-1");
        objects.put(ActionParamsConstants.PARAM_MAPLAYER_GROUPS, String.valueOf(this.groupId)); // Theme/group
        objects.put(ActionParamsConstants.PARAM_GROUP_ID, String.valueOf(this.dataProviderId)); // Dataprovider
        objects.put(ActionParamsConstants.PARAM_LAYER_TYPE, OskariLayer.TYPE_WMS);
        objects.put(ActionParamsConstants.PARAM_PARENT_ID, "-1");
        objects.put("name_fi", name);
        objects.put("name_sv", name);
        objects.put("name_en", name);
        objects.put("name_es", name);
        objects.put(ActionParamsConstants.PARAM_VERSION, "1.3.0");
        objects.put(ActionParamsConstants.PARAM_IS_BASE, "false");
        objects.put(ActionParamsConstants.PARAM_LAYER_NAME, this.geoserverLayerName);
        objects.put(ActionParamsConstants.PARAM_LAYER_URL, this.geoserverLayerUrl);
        objects.put(ActionParamsConstants.PARAM_OPACITY, "100");
        objects.put(ActionParamsConstants.PARAM_STYLE, "raster");
        objects.put(ActionParamsConstants.PARAM_MIN_SCALE, "-1");
        objects.put(ActionParamsConstants.PARAM_MAX_SCALE, "-1");
        objects.put(ActionParamsConstants.PARAM_LEGEND_IMAGE, null);
        objects.put(ActionParamsConstants.PARAM_METADATA_ID, "");
        objects.put(ActionParamsConstants.PARAM_GFI_CONTENT, "");
        objects.put(ActionParamsConstants.PARAM_USERNAME, "");
        objects.put(ActionParamsConstants.PARAM_PASSWORD, "");
        objects.put(ActionParamsConstants.PARAM_CAPABILITIES_UPDATE_RATE_SEC, "0");
        objects.put(ActionParamsConstants.PARAM_ATTRIBUTES, "{}");
        objects.put(ActionParamsConstants.PARAM_PARAMS, "");
        objects.put(ActionParamsConstants.PARAM_OPTIONS, "{}");
        objects.put(ActionParamsConstants.PARAM_SRS_NAME, "EPSG:3067");
        objects.put(ActionParamsConstants.PARAM_REALTIME, "false");
        objects.put(ActionParamsConstants.PARAM_REFRESH_RATE, "");
        objects.put(ActionParamsConstants.PARAM_XSLT, "");
        objects.put(ActionParamsConstants.PARAM_GFI_TYPE, "text/html");
        objects.put(ActionParamsConstants.PARAM_VIEW_PERMISSIONS, "2,3,");
        objects.put(ActionParamsConstants.PARAM_PUBLISH_PERMISSIONS, "3,");
        objects.put(ActionParamsConstants.PARAM_DOWNLOAD_PERMISSIONS, "3,");
        objects.put(ActionParamsConstants.PARAM_EMBEDDED_PERMISSIONS, "3,");
    }
}
