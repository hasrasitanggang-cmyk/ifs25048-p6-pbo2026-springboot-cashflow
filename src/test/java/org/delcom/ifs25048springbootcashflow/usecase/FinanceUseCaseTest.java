package org.delcom.ifs25048springbootcashflow.usecase;

import org.delcom.ifs25048springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25048springbootcashflow.domain.entity.*;
import org.delcom.ifs25048springbootcashflow.domain.exception.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FinanceUseCaseTest {

    private FinanceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FinanceUseCase(
            new TransactionRepository()
        );
    }

    @Test
    void testAddIncome() throws Exception {
        Transaction result = useCase.addIncome(
            1, "Uang bulanan", 1000000, null
        );

        assertNotNull(result.getId());
        assertEquals(TransactionType.INCOME, result.getType());
        assertEquals(1000000L, result.getAmount());
    }

    @Test
    void testAddExpense() throws Exception {
        Transaction result = useCase.addExpense(
            1, "Makan", 25000, null
        );

        assertEquals(TransactionType.EXPENSE, result.getType());
        assertEquals(25000L, result.getAmount());
    }

    @Test
    void testAddTransaction() throws Exception {
        Transaction result = useCase.addTransaction(
            1, "  Buku  ", 75000, TransactionType.EXPENSE
        );

        assertEquals("Buku", result.getDescription());
    }

    @Test
    void testBalance() throws Exception {
        useCase.addIncome(1, "Uang saku", 1000000, null);
        useCase.addExpense(1, "Makan", 25000, null);
        useCase.addExpense(1, "Transportasi", 15000, null);

        assertEquals(1000000L, useCase.getTotalIncome(1));
        assertEquals(40000L, useCase.getTotalExpense(1));
        assertEquals(960000L, useCase.getBalance(1));
        assertEquals(3L, useCase.getTransactionCount(1));
    }

    @Test
    void testEmptyBalance() {
        assertEquals(0L, useCase.getBalance(1));
        assertEquals(0L, useCase.getTransactionCount(1));
    }

    @Test
    void testNegativeBalance() throws Exception {
        useCase.addExpense(1, "Belanja", 50000, null);

        assertEquals(-50000L, useCase.getBalance(1));
    }

    @Test
    void testSearch() throws Exception {
        useCase.addIncome(1, "Gaji", 1000000, null);
        useCase.addExpense(1, "Makan siang", 25000, null);

        var result = useCase.searchTransactions(1, "MAKAN");

        assertEquals(1, result.size());
        assertEquals("Makan siang", result.get(0).getDescription());
    }

    @Test
    void testFilter() throws Exception {
        useCase.addIncome(1, "Gaji", 1000000, null);
        useCase.addExpense(1, "Makan", 25000, null);

        for (TransactionTypeFilter filter :
                TransactionTypeFilter.values()) {

            var result = useCase.listTransactions(
                1, "", filter, SortOption.NEWEST_FIRST
            );

            assertEquals(
                filter == TransactionTypeFilter.ALL ? 2 : 1,
                result.size()
            );
        }
    }

    @Test
    void testSorting() throws Exception {
        useCase.addIncome(1, "Zakat", 1000000, null);
        useCase.addExpense(1, "Alat tulis", 25000, null);

        assertEquals(
            1000000L,
            useCase.sortTransactions(
                1, SortOption.HIGHEST_AMOUNT
            ).get(0).getAmount()
        );

        assertEquals(
            25000L,
            useCase.sortTransactions(
                1, SortOption.LOWEST_AMOUNT
            ).get(0).getAmount()
        );

        assertEquals(
            "Alat tulis",
            useCase.sortTransactions(
                1, SortOption.DESCRIPTION_ASC
            ).get(0).getDescription()
        );

        assertEquals(
            "Zakat",
            useCase.sortTransactions(
                1, SortOption.DESCRIPTION_DESC
            ).get(0).getDescription()
        );
    }

    @Test
    void testDefaultFilterAndSort() throws Exception {
        useCase.addIncome(1, "Gaji", 1000000, null);

        assertEquals(
            1,
            useCase.listTransactions(1, null, null, null).size()
        );
    }

    @Test
    void testUserIsolation() throws Exception {
        useCase.addIncome(1, "Gaji", 1000000, null);
        useCase.addIncome(2, "Bonus", 2000000, null);

        assertEquals(1, useCase.getTransactionsByUser(1).size());
        assertEquals(1000000L, useCase.getBalance(1));
        assertEquals(2000000L, useCase.getBalance(2));
    }

    @Test
    void testDelete() throws Exception {
        Transaction transaction = useCase.addExpense(
            1, "Makan", 25000, null
        );

        useCase.removeTransactionForUser(
            1, transaction.getId()
        );

        assertEquals(0L, useCase.getTransactionCount(1));
    }

    @Test
    void testDeleteOtherUserTransaction() throws Exception {
        Transaction transaction = useCase.addIncome(
            1, "Gaji", 1000000, null
        );

        assertThrows(
            ValidationException.class,
            () -> useCase.removeTransactionForUser(
                2, transaction.getId()
            )
        );
    }

    @Test
    void testDeleteNotFound() {
        assertThrows(
            EntityNotFoundException.class,
            () -> useCase.removeTransactionForUser(1, 999)
        );
    }

    @Test
    void testInvalidTransactionId() {
        assertThrows(
            ValidationException.class,
            () -> useCase.removeTransactionForUser(1, 0)
        );
    }

    @Test
    void testInvalidInput() {
        assertThrows(
            ValidationException.class,
            () -> useCase.addIncome(0, "Gaji", 1000, null)
        );

        assertThrows(
            ValidationException.class,
            () -> useCase.addIncome(1, null, 1000, null)
        );

        assertThrows(
            ValidationException.class,
            () -> useCase.addIncome(1, "", 1000, null)
        );

        assertThrows(
            ValidationException.class,
            () -> useCase.addIncome(1, "Gaji", 0, null)
        );

        assertThrows(
            ValidationException.class,
            () -> useCase.addExpense(1, "Makan", -1000, null)
        );

        assertThrows(
            ValidationException.class,
            () -> useCase.addTransaction(1, "Test", 1000, null)
        );
    }
}