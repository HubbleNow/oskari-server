package fi.peltodata.domain;

import java.util.Date;

public class FarmfieldExecution {
    // 0 = started, 10 = success, -10 = error
    private long id;
    private int state;
    private String outputType;
    private Date executionStartedAt;
    private long farmfieldId;
    private long farmfieldFileId;
    private String outputFilename;

    public long getFarmfieldFileId() {
        return farmfieldFileId;
    }

    public void setFarmfieldFileId(long farmfieldFileId) {
        this.farmfieldFileId = farmfieldFileId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public Date getExecutionStartedAt() {
        return executionStartedAt;
    }

    public void setExecutionStartedAt(Date executionStartedAt) {
        this.executionStartedAt = executionStartedAt;
    }

    public long getFarmfieldId() {
        return farmfieldId;
    }

    public void setFarmfieldId(long farmfieldId) {
        this.farmfieldId = farmfieldId;
    }

    public String getOutputFilename() {
        return outputFilename;
    }

    public void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }
}
