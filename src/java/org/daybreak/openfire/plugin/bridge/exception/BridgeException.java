package org.daybreak.openfire.plugin.bridge.exception;

/**
 * Created by Alan on 2014/3/31.
 */
public class BridgeException extends Exception {

    public static final String INVALID_RESOURCE_OWNER = "invalid_resource_owner";

    private String error;

    private String errorDescription;

    public BridgeException() {
        super();
    }

    public BridgeException(String msg) {
        super(msg);
    }

    public BridgeException(String msg, Throwable e) {
        super(msg, e);
    }

    public BridgeException(String error, String errorDescription) {
        super(error + "(" + errorDescription + ")");
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
}
