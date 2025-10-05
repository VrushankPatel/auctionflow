package com.auctionflow.api.config;

import com.auctionflow.common.dtos.ErrorResponse;
import com.auctionflow.common.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = "com.auctionflow.api.controllers")
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuctionClosedException.class)
    public ResponseEntity<ErrorResponse> handleAuctionClosedException(AuctionClosedException ex) {
        logger.warn("Auction closed exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            "AUCTION_CLOSED",
            ex.getMessage(),
            Instant.now(),
            MDC.get("correlationId")
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(BidderNotEligibleException.class)
    public ResponseEntity<ErrorResponse> handleBidderNotEligibleException(BidderNotEligibleException ex) {
        logger.warn("Bidder not eligible exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            "BIDDER_NOT_ELIGIBLE",
            ex.getMessage(),
            Instant.now(),
            MDC.get("correlationId")
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(InsufficientBidException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBidException(InsufficientBidException ex) {
        logger.warn("Insufficient bid exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            "INSUFFICIENT_BID",
            ex.getMessage(),
            Instant.now(),
            MDC.get("correlationId")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockException(OptimisticLockException ex) {
        logger.warn("Optimistic lock exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            "CONCURRENT_MODIFICATION",
            "The resource was modified by another request. Please try again.",
            Instant.now(),
            MDC.get("correlationId")
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        logger.warn("Validation exception: {}", message);
        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR",
            message,
            Instant.now(),
            MDC.get("correlationId")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected exception: ", ex);
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            Instant.now(),
            MDC.get("correlationId")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}