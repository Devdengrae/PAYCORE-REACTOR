package com.example.paycore.api

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import reactor.core.publisher.Mono

data class ApiError(val message: String)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badRequest(error: IllegalArgumentException): Mono<ApiError> =
        Mono.just(ApiError(error.message ?: "Invalid request"))

    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun conflict(error: IllegalStateException): Mono<ApiError> =
        Mono.just(ApiError(error.message ?: "Conflict"))
}
