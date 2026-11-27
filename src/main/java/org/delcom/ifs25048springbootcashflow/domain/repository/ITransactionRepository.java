package org.delcom.ifs25048springbootcashflow.domain.repository;

import java.util.List;

import org.delcom.ifs25048springbootcashflow.domain.entity.Transaction;

public interface ITransactionRepository
        extends IRepository<Transaction, Integer> {

    List<Transaction> findByUserId(Integer userId);
}