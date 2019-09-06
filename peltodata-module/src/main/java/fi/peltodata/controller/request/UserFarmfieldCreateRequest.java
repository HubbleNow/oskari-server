package fi.peltodata.controller.request;

import java.time.LocalDate;

public class UserFarmfieldCreateRequest {
    private String farmfieldDescription;
    private Long userId;//optional for admin only
    private String farmfieldId;
    private LocalDate farmfieldSowingDate;
    private String farmfieldCropType;

    public LocalDate getFarmfieldSowingDate() {
        return farmfieldSowingDate;
    }

    public void setFarmfieldSowingDate(LocalDate farmfieldSowingDate) {
        this.farmfieldSowingDate = farmfieldSowingDate;
    }

    public String getFarmfieldCropType() {
        return farmfieldCropType;
    }

    public void setFarmfieldCropType(String farmfieldCropType) {
        this.farmfieldCropType = farmfieldCropType;
    }

    public String getFarmfieldDescription() {
        return farmfieldDescription;
    }

    public void setFarmfieldDescription(String farmfieldDescription) {
        this.farmfieldDescription = farmfieldDescription;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFarmfieldId() {
        return farmfieldId;
    }

    public void setFarmfieldId(String farmfieldId) {
        this.farmfieldId = farmfieldId;
    }
}
