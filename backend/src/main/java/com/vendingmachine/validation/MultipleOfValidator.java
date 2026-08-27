package com.vendingmachine.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MultipleOfValidator implements ConstraintValidator<MultipleOf, Integer> {

    private int divisor;

    @Override
    public void initialize(MultipleOf constraintAnnotation) {
        divisor = constraintAnnotation.value();
        if (divisor <= 0) {
            throw new IllegalArgumentException("MultipleOf value must be positive");
        }
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value == null || value % divisor == 0;
    }
}
