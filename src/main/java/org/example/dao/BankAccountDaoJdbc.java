package org.example.dao;

import org.example.db.DB;
import org.example.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BankAccountDaoJdbc implements BankAccountDAO {

    // SQL (Java 11: strings simples)
    private static final String INSERT_SQL =
            "INSERT INTO BankAccount(balance, accountType, currency, creationDate, overdraftLimit, client_id) " +
                    "VALUES (?,?,?,?,?,?)";

    private static final String SELECT_BY_ACC_SQL =
            "SELECT id, accountNumber, balance, accountType, currency, creationDate, overdraftLimit, client_id " +
                    "FROM BankAccount WHERE accountNumber = ?";

    private static final String SELECT_BY_CLIENT_SQL =
            "SELECT id, accountNumber, balance, accountType, currency, creationDate, overdraftLimit, client_id " +
                    "FROM BankAccount WHERE client_id = ? ORDER BY id";

    private static final String UPDATE_BALANCE_SQL =
            "UPDATE BankAccount SET balance = ? WHERE accountNumber = ?";

    private BankAccount map(ResultSet rs) throws SQLException {
        BankAccount a = new BankAccount();
        a.setId(rs.getInt("id"));
        a.setAccountNumber(rs.getString("accountNumber"));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setAccountType(AccountType.valueOf(rs.getString("accountType")));
        a.setCurrency(Currency.valueOf(rs.getString("currency")));
        Date cd = rs.getDate("creationDate");
        a.setCreationDate(cd == null ? null : cd.toLocalDate());
        a.setOverdraftLimit(rs.getBigDecimal("overdraftLimit"));
        a.setClientId(rs.getInt("client_id"));
        return a;
    }

    @Override
    public BankAccount openAccount(BankAccount account) {
        if (account == null) throw new IllegalArgumentException("account is null");
        if (account.getAccountType() == null || account.getCurrency() == null)
            throw new IllegalArgumentException("accountType/currency required");

        // Si no viene fecha, usar hoy (tu trigger igual genera accountNumber)
        if (account.getCreationDate() == null) {
            account.setCreationDate(LocalDate.now());
        }
        if (account.getBalance() == null) account.setBalance(new BigDecimal("0.00"));
        if (account.getOverdraftLimit() == null) account.setOverdraftLimit(new BigDecimal("0.00"));

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setBigDecimal(1, account.getBalance());
            ps.setString(2, account.getAccountType().name());
            ps.setString(3, account.getCurrency().name());
            ps.setDate(4, Date.valueOf(account.getCreationDate()));
            ps.setBigDecimal(5, account.getOverdraftLimit());
            ps.setInt(6, account.getClientId());

            ps.executeUpdate();

            // id autogen
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) account.setId(keys.getInt(1));
            }

            // recuperar accountNumber generado por trigger
            return findByClient(account.getClientId()).stream()
                    .filter(a -> a.getId().equals(account.getId()))
                    .findFirst()
                    .orElseGet(() -> findLatestOfClient(con, account.getClientId())); // respaldo

        } catch (SQLException e) {
            throw new RuntimeException("Error abriendo cuenta", e);
        }
    }

    // rescate si fuese necesario: última cuenta del cliente (id máx)
    private BankAccount findLatestOfClient(Connection con, int clientId) {
        String sql = "SELECT id, accountNumber, balance, accountType, currency, creationDate, overdraftLimit, client_id " +
                "FROM BankAccount WHERE client_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<BankAccount> findByAccountNumber(String accountNumber) {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_ACC_SQL)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando cuenta", e);
        }
    }

    @Override
    public List<BankAccount> findByClient(int clientId) {
        List<BankAccount> list = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_CLIENT_SQL)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando cuentas", e);
        }
    }

    @Override
    public void deposit(String accountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("monto debe ser positivo");

        BankAccount a = findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Cuenta no existe"));

        BigDecimal newBalance = a.getBalance().add(amount);
        updateBalance(accountNumber, newBalance);
    }

    @Override
    public void withdraw(String accountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("monto debe ser positivo");

        BankAccount a = findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Cuenta no existe"));

        BigDecimal newBalance = a.getBalance().subtract(amount);

        // Reglas (en BD ya tienes CHECK, pero validamos también aquí)
        if (a.getAccountType() == AccountType.AHORRO) {
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Saldo en AHORRO no puede ser negativo");
            }
        } else { // CORRIENTE
            BigDecimal limit = a.getOverdraftLimit() == null ? new BigDecimal("500.00") : a.getOverdraftLimit();
            if (newBalance.compareTo(limit.negate()) < 0) {
                throw new RuntimeException("Excede el sobregiro permitido (" + limit + ")");
            }
        }

        updateBalance(accountNumber, newBalance);
    }

    @Override
    public BigDecimal getBalance(String accountNumber) {
        return findByAccountNumber(accountNumber)
                .map(BankAccount::getBalance)
                .orElseThrow(() -> new RuntimeException("Cuenta no existe"));
    }

    private void updateBalance(String accountNumber, BigDecimal newBalance) {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_BALANCE_SQL)) {
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, accountNumber);
            if (ps.executeUpdate() == 0) {
                throw new RuntimeException("No se pudo actualizar el saldo");
            }
        } catch (SQLException e) {
            // si la BD rechaza (CHECK), cae aquí
            throw new RuntimeException("Error actualizando saldo: " + e.getMessage(), e);
        }
    }
}
