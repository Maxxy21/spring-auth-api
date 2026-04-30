package com.maxwell.userregistration.exception;

public class MfaRequiredException extends RuntimeException {

    private final String tempToken;

    public MfaRequiredException(String tempToken) {
        super("MFA validation required");
        this.tempToken = tempToken;
    }

    public String getTempToken() {
        return tempToken;
    }
}
