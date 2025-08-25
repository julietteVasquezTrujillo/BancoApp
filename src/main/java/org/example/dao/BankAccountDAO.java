package org.example.dao;

import org.example.model.BankAccount;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BankAccountDAO {
    BankAccount openAccount(BankAccount account);
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    List<BankAccount> findByClient(int clientId);

    void deposit(String accountNumber, BigDecimal amount);
    void withdraw(String accountNumber, BigDecimal amount);
    BigDecimal getBalance(String accountNumber);
}
