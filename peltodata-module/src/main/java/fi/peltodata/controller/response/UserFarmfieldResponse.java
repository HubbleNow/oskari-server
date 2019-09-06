package fi.peltodata.controller.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fi.peltodata.domain.Farmfield;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class UserFarmfieldResponse implements Serializable {
    private Long farmfieldId;
    private Long userId;
    private String farmfieldDescription;
    private String farmfieldIdString;
    private String farmfieldCropType;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "YYYY-MM-dd")
    private LocalDate farmfieldSowingDate;

    public UserFarmfieldResponse() {
    }

    public UserFarmfieldResponse(Farmfield farmfield) {
        farmfieldId = farmfield.getId();
        farmfieldIdString = farmfield.getFarmId();
        userId = farmfield.getUserId();
        farmfieldDescription = farmfield.getDescription();
        farmfieldSowingDate = farmfield.getSowingDate();
        farmfieldCropType = farmfield.getCropType();
    }

    public Long getFarmfieldId() {
        return farmfieldId;
    }

    public void setFarmfieldId(Long farmfieldId) {
        this.farmfieldId = farmfieldId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFarmfieldDescription() {
        return farmfieldDescription;
    }

    public void setFarmfieldDescription(String farmfieldDescription) {
        this.farmfieldDescription = farmfieldDescription;
    }

    public String getFarmfieldCropType() {
        return farmfieldCropType;
    }

    public void setFarmfieldCropType(String farmfieldCropType) {
        this.farmfieldCropType = farmfieldCropType;
    }

    public LocalDate getFarmfieldSowingDate() {
        return farmfieldSowingDate;
    }

    public void setFarmfieldSowingDate(LocalDate farmfieldSowingDate) {
        this.farmfieldSowingDate = farmfieldSowingDate;
    }

    public String getFarmfieldIdString() {
        return farmfieldIdString;
    }

    public void setFarmfieldIdString(String farmfieldIdString) {
        this.farmfieldIdString = farmfieldIdString;
    }
}
