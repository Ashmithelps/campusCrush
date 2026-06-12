package com.example.campuscrush.error;

import lombok.Getter;

/**
 * A rate/cap violation carrying a machine-readable code. The global error
 * config strips exception messages (`include-message=never`), so the code is
 * the only thing the client receives — the frontend owns the copy per code.
 */
@Getter
public class LimitExceededException extends RuntimeException {

    public static final String HOURLY_CAP = "HOURLY_CAP";
    public static final String DAILY_CAP = "DAILY_CAP";
    public static final String PENDING_CAP = "PENDING_CAP";

    private final String code;

    public LimitExceededException(String code) {
        super(code);
        this.code = code;
    }
}
