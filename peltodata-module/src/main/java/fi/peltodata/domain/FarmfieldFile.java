package fi.peltodata.domain;

import java.time.LocalDate;
import java.util.Date;

public class FarmfieldFile {
    private long id;
    private long farmfieldId;
    private String originalFilename;
    private String fullPath;

    // For example picture taken date
    private LocalDate fileDate;
    private String type;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFarmfieldId() {
        return farmfieldId;
    }

    public void setFarmfieldId(long farmfieldId) {
        this.farmfieldId = farmfieldId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public LocalDate getFileDate() {
        return fileDate;
    }

    public void setFileDate(LocalDate fileDate) {
        this.fileDate = fileDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
