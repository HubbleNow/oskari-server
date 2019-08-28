package fi.peltodata.controller.response;

import fi.peltodata.domain.Farmfield;

import java.io.Serializable;

public class UserFarmfieldResponse implements Serializable {
    private Long farmfieldId;
    private Long userId;
    private String farmfieldDescription;

    public UserFarmfieldResponse() {
    }

    public UserFarmfieldResponse(Farmfield farmfield) {
        farmfieldId = farmfield.getId();
        userId = farmfield.getUserId();
        farmfieldDescription = farmfield.getDescription();
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
}
