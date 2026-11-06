package io.github.anamespace.userservice.exception;

public class RoleConflictException extends RuntimeException {

    public RoleConflictException(String message) {
        super(message);
    }
}
