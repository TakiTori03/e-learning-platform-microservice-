package com.hust.commonlibrary.exception;

import com.hust.commonlibrary.constant.MessageKeys;
import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.exception.payload.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {

    /**
     * Fallback for all unhandled Runtime Exceptions (500)
     */
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<Void>> handlingException(Exception exception) {
        log.error("Unhandled Exception: ", exception);
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message(String.valueOf(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode()))
                .error(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }

    /**
     * Catch-all for our custom application exceptions
     */
    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message(String.valueOf(errorCode.getCode()))
                .error(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    /**
     * Validation errors (@Valid)
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handlingValidation(MethodArgumentNotValidException exception) {
        String enumKey = Objects.requireNonNull(exception.getBindingResult().getFieldError()).getDefaultMessage();
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        
        try {
            errorCode = ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException e) {
            // Log if we missed an error code mapping
        }

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message(String.valueOf(errorCode.getCode()))
                .error(errorCode.getMessage())
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    /**
     * Auth and Refresh Token Errors
     */
    @ExceptionHandler({ AccessDeniedException.class, RefreshTokenException.class })
    public ResponseEntity<ApiResponse<Void>> handlingAccessDeniedException(Exception exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message(String.valueOf(errorCode.getCode()))
                .error(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    /**
     * Login / Token Verification Errors
     */
    @ExceptionHandler({ AuthenticationException.class, VerificationException.class, ExpiredTokenException.class })
    public ResponseEntity<ApiResponse<Void>> authenticationException(Exception e) {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message(String.valueOf(errorCode.getCode()))
                .error(e.getMessage() != null ? e.getMessage() : errorCode.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResponse);
    }

    /**
     * Entity specific exceptions
     */
    @ExceptionHandler(value = { ResourceNotFoundException.class, InvalidParamException.class })
    public ResponseEntity<ApiResponse<Void>> handleSpecificExceptions(Exception e) {
        HttpStatus status = (e instanceof ResourceNotFoundException) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message(String.valueOf(status.value()))
                .error(e.getMessage() != null ? e.getMessage() : MessageKeys.ERROR_MESSAGE)
                .build();
        return ResponseEntity.status(status).body(apiResponse);
    }
}