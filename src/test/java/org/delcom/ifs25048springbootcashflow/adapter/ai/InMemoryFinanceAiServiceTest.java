package org.delcom.ifs25048springbootcashflow.adapter.ai;

import org.delcom.ifs25048springbootcashflow.domain.entity.*;
import org.delcom.ifs25048springbootcashflow.domain.exception.*;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryFinanceAiServiceTest {

    @Test
    void testDefaultResponse() throws Exception {
        var service = new InMemoryFinanceAiService();

        assertEquals(
            3,
            service.generateTransactions("Test").size()
        );
    }

    @Test
    void testCustomResponse() throws Exception {
        var service = new InMemoryFinanceAiService();

        service.addCannedResponse(
            "Bonus", 50000, TransactionType.INCOME
        );

        var result = service.generateTransactions("Test");

        assertEquals(1, result.size());
        assertEquals(50000L, result.get(0).amount());

        service.clearResponses();

        assertEquals(
            3,
            service.generateTransactions("Test").size()
        );
    }

    @Test
    void testConstructor() throws Exception {
        var service = new InMemoryFinanceAiService(
            List.of(new AiGeneratedTransaction(
                "Gaji", 1000000, TransactionType.INCOME
            ))
        );

        assertEquals(
            1,
            service.generateTransactions("Test").size()
        );

        var nullService = new InMemoryFinanceAiService(null);

        assertEquals(
            3,
            nullService.generateTransactions("Test").size()
        );
    }

    @Test
    void testFailure() {
        var service = new InMemoryFinanceAiService();

        service.setSimulateFailure(true);

        assertThrows(
            DomainException.class,
            () -> service.generateTransactions("Test")
        );
    }

    @Test
    void testInvalidPrompt() {
        var service = new InMemoryFinanceAiService();

        assertThrows(
            ValidationException.class,
            () -> service.generateTransactions("")
        );

        assertThrows(
            ValidationException.class,
            () -> service.generateTransactions(null)
        );
    }
}