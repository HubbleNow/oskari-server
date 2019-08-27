package fi.peltodata.geoserver;

public class GeoserverException extends Exception {
    public GeoserverException() {
        super();
    }

    public GeoserverException(String message) {
        super(message);
    }

    public GeoserverException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeoserverException(Throwable cause) {
        super(cause);
    }

    protected GeoserverException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
