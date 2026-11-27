package org.delcom.ifs25048springbootcashflow;

import org.delcom.ifs25048springbootcashflow.adapter.repository.InMemoryRepository;
import org.delcom.ifs25048springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25048springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25048springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25048springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25048springbootcashflow.usecase.FinanceUseCase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FinalCoverageTest {

    @Test
    void testDeleteRepositoryReturnsFalse() throws Exception {

        TransactionRepository repository =
                new TransactionRepository() {

            @Override
            public boolean deleteById(Integer id) {
                return false;
            }
        };

        FinanceUseCase useCase =
                new FinanceUseCase(repository);

        Transaction transaction = useCase.addExpense(
                1,
                "Makan",
                25000L,
                null
        );

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> useCase.removeTransactionForUser(
                        1,
                        transaction.getId()
                )
        );

        assertEquals(
                transaction.getId().intValue(),
                exception.getId()
        );

        assertTrue(
                repository.findById(
                        transaction.getId()
                ).isPresent()
        );
    }

    @Test
    void testInMemoryRepositoryUpdate() {

        InMemoryRepository<Transaction, Integer> repository =
                new InMemoryRepository<Transaction, Integer>(
                        Transaction::getId
                ) {};

        Transaction transaction = new Transaction(
                1,
                "Makan",
                25000L,
                TransactionType.EXPENSE,
                null
        );

        transaction.setId(1);

        repository.save(transaction);

        transaction.setDescription("Makan malam");
        transaction.setAmount(35000L);

        repository.update(transaction);

        Transaction result = repository.findById(1)
                .orElseThrow();

        assertSame(transaction, result);
        assertEquals("Makan malam", result.getDescription());
        assertEquals(35000L, result.getAmount());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void testAppConstructor() {

        App app = new App();

        assertNotNull(app);
    }
}