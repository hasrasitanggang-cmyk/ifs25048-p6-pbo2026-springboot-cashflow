package org.delcom.ifs25048springbootcashflow.usecase;

import org.delcom.ifs25048springbootcashflow.adapter.ai.InMemoryFinanceAiService;
import org.delcom.ifs25048springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25048springbootcashflow.domain.entity.*;
import org.delcom.ifs25048springbootcashflow.domain.exception.*;
import org.delcom.ifs25048springbootcashflow.domain.repository.IFinanceAiService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FinanceAiUseCaseTest {

    private TransactionRepository repository;
    private InMemoryFinanceAiService aiService;
    private FinanceAiUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new TransactionRepository();
        aiService = new InMemoryFinanceAiService();

        useCase = new FinanceAiUseCase(
            aiService, repository
        );
    }

    @Test
    void testGeneratePreview() throws Exception {
        var result = useCase.generateTransactions(
            "Uang bulanan dan pengeluaran"
        );

        assertEquals(3, result.size());

        // Generate tidak boleh langsung menyimpan.
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void testSaveGeneratedTransactions() throws Exception {
        var generated = useCase.generateTransactions(
            "Catatan keuangan"
        );

        var saved = useCase.saveGeneratedTransactions(
            1, generated
        );

        assertEquals(3, saved.size());
        assertEquals(3, repository.findAll().size());

        FinanceUseCase finance = new FinanceUseCase(repository);

        assertEquals(960000L, finance.getBalance(1));
    }

    @Test
    void testCancelPreview() throws Exception {
        useCase.generateTransactions("Catatan keuangan");

        // Pengguna membatalkan tanpa memanggil save.
        assertEquals(0, repository.findAll().size());
    }

    @Test
    void testEmptyPrompt() {
        assertThrows(
            ValidationException.class,
            () -> useCase.generateTransactions("")
        );

        assertThrows(
            ValidationException.class,
            () -> useCase.generateTransactions(null)
        );
    }

    @Test
    void testAiFailure() {
        aiService.setSimulateFailure(true);

        assertThrows(
            DomainException.class,
            () -> useCase.generateTransactions("Test AI")
        );
    }

    @Test
    void testInvalidUser() {
        assertThrows(
            ValidationException.class,
            () -> useCase.saveGeneratedTransactions(
                0, List.of()
            )
        );
    }

    @Test
    void testEmptyGeneratedResult() {
        IFinanceAiService emptyService = prompt -> List.of();

        FinanceAiUseCase emptyUseCase =
            new FinanceAiUseCase(emptyService, repository);

        assertThrows(
            ValidationException.class,
            () -> emptyUseCase.generateTransactions("Test")
        );
    }

    @Test
    void testNullGeneratedResult() {
        assertThrows(
            ValidationException.class,
            () -> useCase.saveGeneratedTransactions(1, null)
        );
    }

    @Test
    void testInvalidGeneratedItems() {

        var invalidItems = List.of(
            new AiGeneratedTransaction(
                "", 1000, TransactionType.INCOME
            ),
            new AiGeneratedTransaction(
                null, 1000, TransactionType.INCOME
            ),
            new AiGeneratedTransaction(
                "Makan", -1000, TransactionType.EXPENSE
            ),
            new AiGeneratedTransaction(
                "Test", 1000, null
            )
        );

        for (var item : invalidItems) {
            assertThrows(
                ValidationException.class,
                () -> useCase.saveGeneratedTransactions(
                    1, List.of(item)
                )
            );
        }

        assertThrows(
            ValidationException.class,
            () -> useCase.saveGeneratedTransactions(
                1,
                Arrays.asList((AiGeneratedTransaction) null)
            )
        );

        assertTrue(repository.findAll().isEmpty());
    }
}