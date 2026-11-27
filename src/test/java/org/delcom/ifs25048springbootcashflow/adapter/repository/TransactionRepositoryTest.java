
package org.delcom.ifs25048springbootcashflow.adapter.repository;

import org.delcom.ifs25048springbootcashflow.domain.entity.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionRepositoryTest {

    private TransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TransactionRepository();
    }

    private Transaction create(int userId, String name) {

        return new Transaction(
            userId,
            name,
            25000L,
            TransactionType.EXPENSE,
            null
        );
    }

    // =====================================================
    // TEST SAVE
    // =====================================================

    @Test
    void testSave() {

        Transaction result = repository.save(
            create(1, "Makan")
        );

        assertNotNull(result.getId());
        assertNotNull(result.getCreatedAt());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void testSaveWithoutDate() {

        Transaction transaction = create(1, "Buku");

        transaction.setCreatedAt(null);

        repository.save(transaction);

        assertNotNull(transaction.getCreatedAt());
    }

    // =====================================================
    // TEST FIND
    // =====================================================

    @Test
    void testFindAll() {

        repository.save(create(1, "Makan"));
        repository.save(create(1, "Buku"));

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void testFindById() {

        Transaction saved = repository.save(
            create(1, "Makan")
        );

        assertTrue(
            repository.findById(saved.getId()).isPresent()
        );

        assertTrue(
            repository.findById(999).isEmpty()
        );
    }

    @Test
    void testFindByUserId() {

        repository.save(create(1, "Makan"));
        repository.save(create(1, "Buku"));
        repository.save(create(2, "Transportasi"));

        assertEquals(2, repository.findByUserId(1).size());
        assertEquals(1, repository.findByUserId(2).size());
        assertEquals(0, repository.findByUserId(3).size());
    }

    // =====================================================
    // TEST UPDATE
    // =====================================================

    @Test
    void testUpdate() {

        Transaction saved = repository.save(
            create(1, "Makan")
        );

        Transaction replacement = new Transaction(
            saved.getId(),
            1,
            "Makan malam",
            35000L,
            TransactionType.EXPENSE,
            null,
            saved.getCreatedAt()
        );

        repository.update(replacement);

        assertEquals(
            "Makan malam",
            repository.findById(saved.getId())
                .orElseThrow().getDescription()
        );

        assertEquals(
            35000L,
            repository.findById(saved.getId())
                .orElseThrow().getAmount()
        );
    }

    @Test
    void testUpdateNotFound() {

        // Simpan transaksi terlebih dahulu
        Transaction existing = repository.save(
            create(1, "Existing Transaction")
        );

        // Buat transaksi dengan ID yang tidak tersedia
        Transaction transaction = create(1, "Test");

        transaction.setId(999);

        // Jalankan update dengan ID yang tidak cocok
        repository.update(transaction);

        // Data lama harus tetap ada
        assertEquals(1, repository.findAll().size());

        assertEquals(
            "Existing Transaction",
            repository.findById(existing.getId())
                .orElseThrow()
                .getDescription()
        );
    }

    // =====================================================
    // TEST DELETE
    // =====================================================

    @Test
    void testDelete() {

        Transaction saved = repository.save(
            create(1, "Makan")
        );

        assertTrue(repository.deleteById(saved.getId()));
        assertFalse(repository.deleteById(saved.getId()));
        assertTrue(repository.findAll().isEmpty());
    }

    // =====================================================
    // TEST FIND BY PREDICATE
    // =====================================================

    @Test
    void testFindByPredicate() {

        repository.save(create(1, "Makan"));
        repository.save(create(2, "Buku"));

        var result = repository.findBy(
            transaction -> transaction.getUserId() == 1
        );

        assertEquals(1, result.size());
        assertEquals("Makan", result.get(0).getDescription());
    }
}
