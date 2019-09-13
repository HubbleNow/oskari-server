package fi.peltodata.controller.request;

import java.time.LocalDate;
import java.util.Date;

public class FarmfieldFileDateUpdateRequest {
    private LocalDate date;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
