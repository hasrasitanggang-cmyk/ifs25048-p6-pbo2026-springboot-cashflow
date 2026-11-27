package org.delcom.ifs25048springbootcashflow.domain.repository;

import java.util.List;

import org.delcom.ifs25048springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25048springbootcashflow.domain.exception.DomainException;

public interface IFinanceAiService {

    List<AiGeneratedTransaction> generateTransactions(
        String prompt
    ) throws DomainException;
}