package fi.peltodata.domain;

import java.util.Date;

public class FarmfieldExecution {
    // 0 = started, 10 = success, -10 = error
    private int state;
    private String outputType;
    private Date executionStartedAt;
    private long farmfieldId;
}
