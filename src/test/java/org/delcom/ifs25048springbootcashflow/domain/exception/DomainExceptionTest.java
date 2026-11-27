
package org.delcom.ifs25048springbootcashflow.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainExceptionTest {

    @Test
    void constructorWithMessage() {
        DomainException exception =
                new DomainException("Terjadi kesalahan domain");

        assertEquals(
                "Terjadi kesalahan domain",
                exception.getMessage()
        );

        assertNull(exception.getCause());
    }

    @Test
    void constructorWithMessageAndCause() {
        Throwable cause =
                new IllegalArgumentException("Penyebab kesalahan");

        DomainException exception =
                new DomainException("Kesalahan domain", cause);

        assertEquals(
                "Kesalahan domain",
                exception.getMessage()
        );

        assertSame(cause, exception.getCause());
    }
}
