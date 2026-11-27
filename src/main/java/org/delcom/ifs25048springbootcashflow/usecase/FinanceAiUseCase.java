package org.delcom.ifs25048springbootcashflow.usecase;

import org.delcom.ifs25048springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25048springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25048springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25048springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25048springbootcashflow.domain.repository.IFinanceAiService;
import org.delcom.ifs25048springbootcashflow.domain.repository.ITransactionRepository;

import java.util.ArrayList;
import java.util.List;

public class FinanceAiUseCase {

    private final IFinanceAiService aiService;

    private final FinanceUseCase financeUseCase;

    public FinanceAiUseCase(
            IFinanceAiService aiService,
            ITransactionRepository transactionRepository
    ) {

        this.aiService = aiService;

        this.financeUseCase = new FinanceUseCase(
                transactionRepository
        );
    }

    public List<AiGeneratedTransaction> generateTransactions(
            String prompt
    ) throws DomainException {

        if (prompt == null || prompt.isBlank()) {
            throw new ValidationException(
                    "Prompt AI tidak boleh kosong"
            );
        }

        List<AiGeneratedTransaction> generated =
                aiService.generateTransactions(prompt.trim());

        validateGeneratedTransactions(generated);

        return List.copyOf(generated);
    }

    public List<Transaction> saveGeneratedTransactions(
            int userId,
            List<AiGeneratedTransaction> items
    ) throws ValidationException {

        if (userId <= 0) {
            throw new ValidationException(
                    "ID pengguna tidak valid"
            );
        }

        validateGeneratedTransactions(items);

        List<Transaction> saved = new ArrayList<>();

        for (AiGeneratedTransaction item : items) {

            Transaction transaction =
                    financeUseCase.addTransaction(
                            userId,
                            item.description(),
                            item.amount(),
                            item.type(),
                            null
                    );

            saved.add(transaction);
        }

        return saved;
    }

    private void validateGeneratedTransactions(
            List<AiGeneratedTransaction> items
    ) throws ValidationException {

        if (items == null || items.isEmpty()) {
            throw new ValidationException(
                    "Tidak ada transaksi hasil AI"
            );
        }

        for (AiGeneratedTransaction item : items) {

            if (item == null) {
                throw new ValidationException(
                        "Data transaksi AI tidak valid"
                );
            }

            if (item.description() == null
                    || item.description().isBlank()) {

                throw new ValidationException(
                        "Keterangan transaksi AI tidak boleh kosong"
                );
            }

            if (item.amount() <= 0) {
                throw new ValidationException(
                        "Jumlah transaksi AI harus positif"
                );
            }

            if (item.type() == null) {
                throw new ValidationException(
                        "Tipe transaksi AI tidak valid"
                );
            }
        }
    }
}