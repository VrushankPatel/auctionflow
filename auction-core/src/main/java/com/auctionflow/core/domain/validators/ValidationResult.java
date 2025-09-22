package com.auctionflow.core.domain.validators;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private static final ThreadLocal<ValidationResult> POOL = ThreadLocal.withInitial(ValidationResult::new);

    private final List<String> errors;

    public ValidationResult() {
        this.errors = new ArrayList<>();
    }

    /**
     * Returns a thread-local ValidationResult instance, cleared for reuse.
     * Reduces allocation in hot paths.
     */
    public static ValidationResult getInstance() {
        ValidationResult result = POOL.get();
        result.errors.clear();
        return result;
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public void addError(String error) {
        errors.add(error);
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}