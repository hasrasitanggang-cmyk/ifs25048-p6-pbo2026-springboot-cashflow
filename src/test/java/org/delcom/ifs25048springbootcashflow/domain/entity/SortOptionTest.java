package org.delcom.ifs25048springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SortOptionTest {

    private List<Transaction> createTransactions() {

        Transaction first = new Transaction(
            1, 1, "Gaji", 1000000L,
            TransactionType.INCOME, null,
            LocalDateTime.of(2026, 11, 25, 10, 0)
        );

        Transaction second = new Transaction(
            2, 1, "Buku", 75000L,
            TransactionType.EXPENSE, null,
            LocalDateTime.of(2026, 11, 26, 10, 0)
        );

        return new ArrayList<>(List.of(first, second));
    }

    @Test
    void testNewestFirst() {

        var transactions = createTransactions();

        transactions.sort(SortOption.NEWEST_FIRST.comparator());

        assertEquals(2, transactions.get(0).getId());
    }

    @Test
    void testOldestFirst() {

        var transactions = createTransactions();

        transactions.sort(SortOption.OLDEST_FIRST.comparator());

        assertEquals(1, transactions.get(0).getId());
    }

    @Test
    void testHighestAmount() {

        var transactions = createTransactions();

        transactions.sort(SortOption.HIGHEST_AMOUNT.comparator());

        assertEquals(1000000L, transactions.get(0).getAmount());
    }

    @Test
    void testLowestAmount() {

        var transactions = createTransactions();

        transactions.sort(SortOption.LOWEST_AMOUNT.comparator());

        assertEquals(75000L, transactions.get(0).getAmount());
    }

    @Test
    void testDescriptionAscending() {

        var transactions = createTransactions();

        transactions.sort(SortOption.DESCRIPTION_ASC.comparator());

        assertEquals("Buku", transactions.get(0).getDescription());
    }

    @Test
    void testDescriptionDescending() {

        var transactions = createTransactions();

        transactions.sort(SortOption.DESCRIPTION_DESC.comparator());

        assertEquals("Gaji", transactions.get(0).getDescription());
    }

    @Test
    void testLabels() {

        for (SortOption option : SortOption.values()) {

            assertNotNull(option.label());

            assertFalse(option.label().isBlank());

            assertNotNull(option.comparator());
        }
    }
}