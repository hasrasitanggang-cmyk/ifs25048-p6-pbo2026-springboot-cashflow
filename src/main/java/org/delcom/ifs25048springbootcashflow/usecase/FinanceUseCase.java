package org.delcom.ifs25048springbootcashflow.usecase;

import org.delcom.ifs25048springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25048springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25048springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25048springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25048springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25048springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25048springbootcashflow.domain.repository.ITransactionRepository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public class FinanceUseCase {

    private final ITransactionRepository transactionRepository;

    public FinanceUseCase(
            ITransactionRepository transactionRepository
    ) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getTransactionsByUser(int userId) {
        return listTransactions(
                userId,
                null,
                TransactionTypeFilter.ALL,
                SortOption.NEWEST_FIRST
        );
    }

    public List<Transaction> listTransactions(
            int userId,
            String keyword,
            TransactionTypeFilter filter,
            SortOption sortOption
    ) {

        TransactionTypeFilter selectedFilter =
                filter == null
                        ? TransactionTypeFilter.ALL
                        : filter;

        SortOption selectedSort =
                sortOption == null
                        ? SortOption.NEWEST_FIRST
                        : sortOption;

        Stream<Transaction> stream =
                transactionRepository.findByUserId(userId)
                        .stream()
                        .filter(transaction ->
                                selectedFilter.matches(
                                        transaction.getType()
                                )
                        );

        if (keyword != null && !keyword.isBlank()) {

            String search = keyword.trim()
                    .toLowerCase(Locale.ROOT);

            stream = stream.filter(transaction ->
                    transaction.getDescription()
                            .toLowerCase(Locale.ROOT)
                            .contains(search)
            );
        }

        return stream
                .sorted(selectedSort.comparator())
                .toList();
    }

    public Transaction addTransaction(
            int userId,
            String description,
            long amount,
            TransactionType type
    ) throws ValidationException {

        return addTransaction(
                userId,
                description,
                amount,
                type,
                null
        );
    }

    public Transaction addTransaction(
            int userId,
            String description,
            long amount,
            TransactionType type,
            String proofPath
    ) throws ValidationException {

        validateTransaction(
                userId,
                description,
                amount,
                type
        );

        Transaction transaction = new Transaction(
                userId,
                description.trim(),
                amount,
                type,
                proofPath
        );

        return transactionRepository.save(transaction);
    }

    public Transaction addIncome(
            int userId,
            String description,
            long amount,
            String proofPath
    ) throws ValidationException {

        return addTransaction(
                userId,
                description,
                amount,
                TransactionType.INCOME,
                proofPath
        );
    }

    public Transaction addExpense(
            int userId,
            String description,
            long amount,
            String proofPath
    ) throws ValidationException {

        return addTransaction(
                userId,
                description,
                amount,
                TransactionType.EXPENSE,
                proofPath
        );
    }

    public void removeTransactionForUser(
            int userId,
            int transactionId
    ) throws EntityNotFoundException, ValidationException {

        if (transactionId <= 0) {
            throw new ValidationException(
                    "ID transaksi tidak valid"
            );
        }

        Transaction transaction =
                transactionRepository.findById(transactionId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        transactionId
                                )
                        );

        if (!Objects.equals(
                transaction.getUserId(),
                userId
        )) {
            throw new ValidationException(
                    "Transaksi bukan milik pengguna ini"
            );
        }

        if (!transactionRepository.deleteById(transactionId)) {
            throw new EntityNotFoundException(transactionId);
        }
    }

    public List<Transaction> searchTransactions(
            int userId,
            String keyword
    ) {

        return listTransactions(
                userId,
                keyword,
                TransactionTypeFilter.ALL,
                SortOption.NEWEST_FIRST
        );
    }

    public List<Transaction> sortTransactions(
            int userId,
            SortOption sortOption
    ) {

        return listTransactions(
                userId,
                null,
                TransactionTypeFilter.ALL,
                sortOption
        );
    }

    public long getTotalIncome(int userId) {

        return transactionRepository.findByUserId(userId)
                .stream()
                .filter(transaction ->
                        transaction.getType()
                                == TransactionType.INCOME
                )
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    public long getTotalExpense(int userId) {

        return transactionRepository.findByUserId(userId)
                .stream()
                .filter(transaction ->
                        transaction.getType()
                                == TransactionType.EXPENSE
                )
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    public long getBalance(int userId) {

        return getTotalIncome(userId)
                - getTotalExpense(userId);
    }

    public long getTransactionCount(int userId) {

        return transactionRepository.findByUserId(userId)
                .size();
    }

    private void validateTransaction(
            int userId,
            String description,
            long amount,
            TransactionType type
    ) throws ValidationException {

        if (userId <= 0) {
            throw new ValidationException(
                    "ID pengguna tidak valid"
            );
        }

        if (description == null || description.isBlank()) {
            throw new ValidationException(
                    "Keterangan transaksi tidak boleh kosong"
            );
        }

        if (amount <= 0) {
            throw new ValidationException(
                    "Jumlah transaksi harus lebih besar dari nol"
            );
        }

        if (type == null) {
            throw new ValidationException(
                    "Tipe transaksi tidak boleh kosong"
            );
        }
    }
}