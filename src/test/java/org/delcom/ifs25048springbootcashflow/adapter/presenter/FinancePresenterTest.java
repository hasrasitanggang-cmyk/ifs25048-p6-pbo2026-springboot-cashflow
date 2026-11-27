package org.delcom.ifs25048springbootcashflow.adapter.presenter;

import org.delcom.ifs25048springbootcashflow.domain.entity.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FinancePresenterTest {

    private final FinancePresenter presenter =
        new FinancePresenter();

    @Test
    void testAmount() {
        assertEquals(
            "Rp 1,000,000.00",
            presenter.formatAmount(1000000)
        );

        assertEquals(
            "Rp 25,000.00",
            presenter.formatCurrency(25000)
        );

        assertEquals(
            "Rp -50,000.00",
            presenter.formatAmount(-50000)
        );
    }

    @Test
    void testDateTime() {
        assertEquals(
            "26/11/2026 13:00",
            presenter.formatDateTime(
                LocalDateTime.of(2026, 11, 26, 13, 0)
            )
        );

        assertEquals("-", presenter.formatDateTime(null));
    }

    @Test
    void testType() {
        assertEquals(
            "Pemasukan",
            presenter.formatType(TransactionType.INCOME)
        );

        assertEquals(
            "Pengeluaran",
            presenter.formatType(TransactionType.EXPENSE)
        );

        assertEquals("-", presenter.formatType(null));
    }

    @Test
    void testNotifications() {
        var transaction = new Transaction(
            1, "Gaji", 1000000,
            TransactionType.INCOME, null
        );

        transaction.setId(1);

        assertTrue(
            presenter.formatTransaction(transaction)
                .contains("Gaji")
        );

        assertTrue(
            presenter.successAdd(transaction)
                .contains("Berhasil")
        );

        assertTrue(
            presenter.successDelete(1)
                .contains("[1]")
        );

        assertTrue(
            presenter.successGenerate(3)
                .contains("3")
        );

        assertEquals(
            "Saldo: Rp 1,000,000.00",
            presenter.formatBalance(1000000)
        );
    }

    @Test
    void testErrorMessage() {
        assertEquals(
            "Terjadi kesalahan",
            presenter.errorMessage(null)
        );

        assertEquals(
            "Terjadi kesalahan",
            presenter.errorMessage(new Exception())
        );

        assertEquals(
            "Test error",
            presenter.errorMessage(
                new Exception("Test error")
            )
        );
    }
}