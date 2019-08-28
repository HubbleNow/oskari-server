package fi.peltodata.controller.request;

public class UserFarmfieldCreateRequest {
    private String farmfieldDescription;
    private Long userId;//optional for admin only

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
}
