package org.example.app;

import org.example.dao.ClientDAO;
import org.example.dao.ClientDaoJdbc;
import org.example.dao.BankAccountDAO;
import org.example.dao.BankAccountDaoJdbc;
import org.example.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        ClientDAO clientDao = new ClientDaoJdbc();
        BankAccountDAO accDao = new BankAccountDaoJdbc();

        // ---------------- CLIENTES ----------------
        // Crear o reutilizar cliente por DNI
        String dni = "11223344";
        Client c = clientDao.findByDni(dni).orElseGet(() -> {
            Client nuevo = new Client(
                    dni, "Rosa", "Santos", "rosa@mail.com", "987654321",
                    LocalDate.of(1998, 3, 21), "Av. Primavera 123"
            );
            return clientDao.save(nuevo);
        });
        System.out.println("Cliente listo: " + c);

        // Actualizar (dni NO cambia)
        c.setEmail("rosa.actualizado@mail.com");
        c.setPhoneNumber("999888777");
        c.setAddress("Jr. Los Tulipanes 456");
        clientDao.update(c);
        System.out.println("Cliente actualizado: " + clientDao.findById(c.getId()).orElse(null));

        // Listar clientes
        System.out.println("----- Clientes -----");
        clientDao.findAll().forEach(System.out::println);

        // ---------------- CUENTAS ----------------
        // Abrir una cuenta Ahorro PEN y otra Corriente USD
        BankAccount penAhorro = new BankAccount();
        penAhorro.setAccountType(AccountType.AHORRO);
        penAhorro.setCurrency(Currency.PEN);
        penAhorro.setBalance(new BigDecimal("0.00"));
        penAhorro.setOverdraftLimit(new BigDecimal("0.00")); // ahorro no usa sobregiro
        penAhorro.setClientId(c.getId());
        penAhorro = accDao.openAccount(penAhorro);

        BankAccount usdCorriente = new BankAccount();
        usdCorriente.setAccountType(AccountType.CORRIENTE);
        usdCorriente.setCurrency(Currency.USD);
        usdCorriente.setBalance(new BigDecimal("200.00"));
        usdCorriente.setOverdraftLimit(new BigDecimal("500.00")); // límite de sobregiro
        usdCorriente.setClientId(c.getId());
        usdCorriente = accDao.openAccount(usdCorriente);

        System.out.println("Cuenta PEN ahorro: " + penAhorro.getAccountNumber());
        System.out.println("Cuenta USD corriente: " + usdCorriente.getAccountNumber());

        // Depósitos y retiros válidos
        accDao.deposit(penAhorro.getAccountNumber(), new BigDecimal("300.00"));
        accDao.withdraw(penAhorro.getAccountNumber(), new BigDecimal("50.00")); // saldo 250

        accDao.deposit(usdCorriente.getAccountNumber(), new BigDecimal("100.00"));  // 300
        accDao.withdraw(usdCorriente.getAccountNumber(), new BigDecimal("700.00")); // 300 - 700 = -400 (válido por sobregiro 500)

        System.out.println("Saldo PEN: " + accDao.getBalance(penAhorro.getAccountNumber()));
        System.out.println("Saldo USD: " + accDao.getBalance(usdCorriente.getAccountNumber()));

        // Intento inválido (debe fallar y lanzar excepción controlada)
        try {
            accDao.withdraw(penAhorro.getAccountNumber(), new BigDecimal("300.00")); // dejaría negativo → inválido
        } catch (RuntimeException ex) {
            System.out.println("Retiro inválido en Ahorro PEN: " + ex.getMessage());
        }

        // Listar cuentas del cliente
        System.out.println("----- Cuentas del cliente -----");
        List<BankAccount> cuentas = accDao.findByClient(c.getId());
        cuentas.forEach(a -> System.out.println(
                a.getAccountNumber() + " | " + a.getCurrency() + " | " + a.getAccountType() + " | " + a.getBalance()
        ));
    }
}
