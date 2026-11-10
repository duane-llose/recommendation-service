package com.vinsguru.netflux.recommendation.exceptions;

public class MissingUserIdException extends RuntimeException {

    public MissingUserIdException() {
        super("Missing required header: X-User-Id");
    }
}
