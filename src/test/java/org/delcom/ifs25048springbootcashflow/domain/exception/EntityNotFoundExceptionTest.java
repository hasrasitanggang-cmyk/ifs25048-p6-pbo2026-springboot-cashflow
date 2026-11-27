
package org.delcom.ifs25048springbootcashflow.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityNotFoundExceptionTest {

    @Test
    void constructorAndGetId() {
        int expectedId = 42;

        EntityNotFoundException exception =
                new EntityNotFoundException(expectedId);

        assertEquals(expectedId, exception.getId());

        assertEquals(
                "Todo dengan ID 42 tidak ditemukan.",
                exception.getMessage()
        );
    }
}
