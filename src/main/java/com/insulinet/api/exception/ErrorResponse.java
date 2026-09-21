package com.insulinet.api.exception;

/**
 * Formato de erro compativel com o {"detail": "..."} retornado pelo
 * HTTPException do FastAPI no backend Python original.
 */
public record ErrorResponse(Object detail) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message);
    }
}
