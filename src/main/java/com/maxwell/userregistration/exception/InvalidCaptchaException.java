package com.maxwell.userregistration.exception;

public class InvalidCaptchaException extends RuntimeException {

    public InvalidCaptchaException(String message) {
        super(message);
    }
}
