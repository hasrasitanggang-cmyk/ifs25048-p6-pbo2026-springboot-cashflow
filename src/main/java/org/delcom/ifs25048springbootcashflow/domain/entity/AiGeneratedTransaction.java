package org.delcom.ifs25048springbootcashflow.domain.entity;

public record AiGeneratedTransaction(
        String description,
        long amount,
        TransactionType type
) {
}