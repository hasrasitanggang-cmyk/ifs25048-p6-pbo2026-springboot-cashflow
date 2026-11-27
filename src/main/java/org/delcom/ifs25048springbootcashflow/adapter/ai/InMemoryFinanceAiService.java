package org.delcom.ifs25048springbootcashflow.adapter.ai;

import org.delcom.ifs25048springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25048springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25048springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25048springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25048springbootcashflow.domain.repository.IFinanceAiService;

import java.util.ArrayList;
import java.util.List;

public class InMemoryFinanceAiService
        implements IFinanceAiService {

    private final List<AiGeneratedTransaction> responses =
            new ArrayList<>();

    private boolean simulateFailure;

    public InMemoryFinanceAiService() {
    }

    public InMemoryFinanceAiService(
            List<AiGeneratedTransaction> defaultResponses
    ) {

        if (defaultResponses != null) {

            responses.addAll(defaultResponses);
        }
    }

    public void setSimulateFailure(
            boolean simulateFailure
    ) {

        this.simulateFailure = simulateFailure;
    }

    public void addCannedResponse(
            String description,
            long amount,
            TransactionType type
    ) {

        responses.add(
            new AiGeneratedTransaction(
                description,
                amount,
                type
            )
        );
    }

    public void clearResponses() {

        responses.clear();
    }

    @Override
    public List<AiGeneratedTransaction> generateTransactions(
            String prompt
    ) throws DomainException {

        if (prompt == null || prompt.isBlank()) {

            throw new ValidationException(
                "Prompt catatan keuangan tidak boleh kosong"
            );
        }

        if (simulateFailure) {

            throw new DomainException(
                "Simulasi kegagalan layanan AI"
            );
        }

        if (!responses.isEmpty()) {

            return new ArrayList<>(responses);
        }

        return List.of(

            new AiGeneratedTransaction(
                "Uang bulanan",
                1000000L,
                TransactionType.INCOME
            ),

            new AiGeneratedTransaction(
                "Makan siang",
                25000L,
                TransactionType.EXPENSE
            ),

            new AiGeneratedTransaction(
                "Transportasi",
                15000L,
                TransactionType.EXPENSE
            )
        );
    }
}