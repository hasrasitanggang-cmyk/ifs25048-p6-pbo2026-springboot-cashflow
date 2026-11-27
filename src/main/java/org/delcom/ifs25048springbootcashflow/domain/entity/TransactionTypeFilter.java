package org.delcom.ifs25048springbootcashflow.domain.entity;

public enum TransactionTypeFilter {

    ALL("Semua tipe"),
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran");

    private final String label;

    TransactionTypeFilter(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean matches(TransactionType type) {
        return switch (this) {
            case ALL -> true;
            case INCOME -> type == TransactionType.INCOME;
            case EXPENSE -> type == TransactionType.EXPENSE;
        };
    }
}