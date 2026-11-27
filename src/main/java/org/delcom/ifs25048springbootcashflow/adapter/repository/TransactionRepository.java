package org.delcom.ifs25048springbootcashflow.adapter.repository;

import org.delcom.ifs25048springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25048springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25048springbootcashflow.domain.repository.ITransactionRepository;

import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;

public class TransactionRepository
        extends InMemoryRepository<Transaction, Integer>
        implements ITransactionRepository {

    private int nextId = 1;

    public TransactionRepository() {
        super(Transaction::getId);
    }

    @Override
    public Transaction save(Transaction entity) {

        entity.setId(nextId++);

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }

        return super.save(entity);
    }

    @Override
    public List<Transaction> findAll() {

        return super.findAll()
                .stream()
                .sorted(SortOption.NEWEST_FIRST.comparator())
                .toList();
    }

    @Override
    public List<Transaction> findByUserId(Integer userId) {

        return findAll()
                .stream()
                .filter(transaction ->
                    Objects.equals(
                        transaction.getUserId(),
                        userId
                    )
                )
                .toList();
    }

    @Override
    public void update(Transaction entity) {

        for (int i = 0; i < data.size(); i++) {

            if (Objects.equals(
                    data.get(i).getId(),
                    entity.getId()
            )) {

                data.set(i, entity);
                return;
            }
        }
    }
}