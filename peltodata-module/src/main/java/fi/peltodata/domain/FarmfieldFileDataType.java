package fi.peltodata.domain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum FarmfieldFileDataType {
    CROP_ESTIMATION_ORIGINAL_DATA (
            10, Types.CROP_ESTIMATION_ORIGINAL_ID, "tiff", Types.CROP_ESTIMATION_RAW_ID, Types.CROP_ESTIMATION_ID),
    CROP_ESTIMATION_RAW_DATA(
            11, Types.CROP_ESTIMATION_RAW_ID, "tiff", Types.CROP_ESTIMATION_RAW_ID),
    CROP_ESTIMATION_DATA(
            12, Types.CROP_ESTIMATION_ID, "tiff", Types.CROP_ESTIMATION_ID),
    YIELD_RAW_DATA(
            20, Types.YIELD_RAW_ID, "tiff", Types.YIELD_ID),
    YIELD_DATA(
            21, Types.YIELD_ID, "tiff", Types.YIELD_ID);

    public static class Types {
        private static String CROP_ESTIMATION_ORIGINAL_ID = "crop_estimation_original";
        private static String CROP_ESTIMATION_RAW_ID = "crop_estimation_raw";
        private static String CROP_ESTIMATION_ID = "crop_estimation";
        private static String YIELD_RAW_ID = "yield_raw";
        private static String YIELD_ID = "yield";
    }

    private int id;
    private String typeId;
    private String dataFormat;
    private Set<String> allowedOutputTypeIds = new HashSet<>();

    FarmfieldFileDataType(int id, String typeId, String dataFormat, String ... allowedOutputTypeIds) {
        this.id = id;
        this.typeId = typeId;
        this.dataFormat = dataFormat;
        if (allowedOutputTypeIds != null && allowedOutputTypeIds.length > 0) {
            this.allowedOutputTypeIds.addAll(Arrays.asList(allowedOutputTypeIds));
        }
    }

    public static FarmfieldFileDataType fromValue(int id) {
        for (FarmfieldFileDataType type : FarmfieldFileDataType.values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return null;
    }

    public static FarmfieldFileDataType fromString(String typeId) {
        for (FarmfieldFileDataType type : FarmfieldFileDataType.values()) {
            if (type.getTypeId().equalsIgnoreCase(typeId)) {
                return type;
            }
        }
        return null;
    }

    public static FarmfieldFileDataType fromPathString(String pathString) {
        for (FarmfieldFileDataType type : FarmfieldFileDataType.values()) {
            if (pathString.toLowerCase().contains(type.getTypeId().toLowerCase())) {
                return type;
            }
        }
        return null;
    }

    public String toValue() {
        return this.getTypeId();
    }

    public String getDataFormat() {
        return dataFormat;
    }

    public String getFolderName() {
        return typeId;
    }

    public int getId() {
        return id;
    }

    public String getTypeId() {
        return typeId;
    }

    public Set<String> getAllowedOutputTypeIds() {
        return allowedOutputTypeIds;
    }

    public boolean isAllowedOutputTypeId(FarmfieldFileDataType wantedOutputType) {
        if (this.allowedOutputTypeIds.size() > 0) {
            String typeId = wantedOutputType.getTypeId();
            return allowedOutputTypeIds.contains(typeId);
        }
        return false;
    }
}
