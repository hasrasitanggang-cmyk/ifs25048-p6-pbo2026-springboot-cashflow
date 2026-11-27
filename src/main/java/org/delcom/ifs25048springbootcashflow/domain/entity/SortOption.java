package org.delcom.ifs25048springbootcashflow.domain.entity;

import java.util.Comparator;

public enum SortOption {

    NEWEST_FIRST(
        "Terbaru",
        Comparator.comparing(Transaction::getCreatedAt)
            .thenComparing(Transaction::getId)
            .reversed()
    ),

    OLDEST_FIRST(
        "Terlama",
        Comparator.comparing(Transaction::getCreatedAt)
            .thenComparing(Transaction::getId)
    ),

    HIGHEST_AMOUNT(
        "Jumlah terbesar",
        Comparator.comparingLong(Transaction::getAmount)
            .reversed()
    ),

    LOWEST_AMOUNT(
        "Jumlah terkecil",
        Comparator.comparingLong(Transaction::getAmount)
    ),

    DESCRIPTION_ASC(
        "Keterangan A-Z",
        Comparator.comparing(
            Transaction::getDescription,
            String.CASE_INSENSITIVE_ORDER
        )
    ),

    DESCRIPTION_DESC(
        "Keterangan Z-A",
        Comparator.comparing(
            Transaction::getDescription,
            String.CASE_INSENSITIVE_ORDER
        ).reversed()
    );

    private final String label;
    private final Comparator<Transaction> comparator;

    SortOption(
            String label,
            Comparator<Transaction> comparator
    ) {
        this.label = label;
        this.comparator = comparator;
    }

    public String label() {
        return label;
    }

    public Comparator<Transaction> comparator() {
        return comparator;
    }
}