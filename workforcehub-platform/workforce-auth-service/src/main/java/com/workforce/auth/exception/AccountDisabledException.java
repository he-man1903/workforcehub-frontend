package com.workforce.auth.exception;

/** Thrown when a user's account has been disabled by an admin. */
public class AccountDisabledException extends UserDisabledException {
    public AccountDisabledException(String message) {
        super(message);
    }
}
