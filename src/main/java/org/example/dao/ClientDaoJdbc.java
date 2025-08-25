package org.example.dao;

import org.example.db.DB;
import org.example.model.Client;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientDaoJdbc implements ClientDAO {

    // ---------- SQL (Java 11, sin text blocks) ----------
    private static final String INSERT_SQL =
            "INSERT INTO Client(firstName,lastName,dni,email,phoneNumber,birthDate,address) " +
                    "VALUES (?,?,?,?,?,?,?)";

    private static final String SELECT_BY_ID_SQL =
            "SELECT id, firstName, lastName, dni, email, phoneNumber, birthDate, address " +
                    "FROM Client WHERE id = ?";

    private static final String SELECT_BY_DNI_SQL =
            "SELECT id, firstName, lastName, dni, email, phoneNumber, birthDate, address " +
                    "FROM Client WHERE dni = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT id, firstName, lastName, dni, email, phoneNumber, birthDate, address " +
                    "FROM Client ORDER BY id";

    // OJO: el DNI es único y NO se actualiza
    private static final String UPDATE_SQL =
            "UPDATE Client " +
                    "   SET firstName = ?, lastName = ?, email = ?, phoneNumber = ?, birthDate = ?, address = ? " +
                    " WHERE id = ?";

    private static final String DELETE_SQL =
            "DELETE FROM Client WHERE id = ?";

    // ---------- helpers ----------
    private Client map(ResultSet rs) throws SQLException {
        Client c = new Client();
        c.setId(rs.getInt("id"));
        c.setFirstName(rs.getString("firstName"));
        c.setLastName(rs.getString("lastName"));

        // Si no tienes setDni en Client, hacemos un set por reflexión solo para mapear
        try {
            java.lang.reflect.Field f = Client.class.getDeclaredField("dni");
            f.setAccessible(true);
            f.set(c, rs.getString("dni"));
        } catch (ReflectiveOperationException ignored) { }

        c.setEmail(rs.getString("email"));
        c.setPhoneNumber(rs.getString("phoneNumber"));

        Date bd = rs.getDate("birthDate");
        c.setBirthDate(bd == null ? null : bd.toLocalDate());

        c.setAddress(rs.getString("address"));
        return c;
    }

    private void fill(PreparedStatement ps, Client c) throws SQLException {
        ps.setString(1, c.getFirstName());
        ps.setString(2, c.getLastName());
        ps.setString(3, c.getDni());
        ps.setString(4, c.getEmail());
        ps.setString(5, c.getPhoneNumber());
        if (c.getBirthDate() == null) ps.setNull(6, Types.DATE);
        else ps.setDate(6, Date.valueOf(c.getBirthDate()));
        ps.setString(7, c.getAddress());
    }

    // ---------- CRUD ----------
    @Override
    public Client save(Client client) {
        if (client == null) throw new IllegalArgumentException("client is null");
        if (client.getDni() == null || client.getDni().isBlank())
            throw new IllegalArgumentException("dni required");

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            fill(ps, client);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) client.setId(keys.getInt(1));
            }
            return client;

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new RuntimeException("DNI ya registrado u otra restricción: " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException("Error guardando cliente", e);
        }
    }

    @Override
    public Optional<Client> findById(int id) {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_ID_SQL)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando por id", e);
        }
    }

    @Override
    public Optional<Client> findByDni(String dni) {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_DNI_SQL)) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando por dni", e);
        }
    }

    @Override
    public List<Client> findAll() {
        List<Client> list = new ArrayList<>();
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando clientes", e);
        }
    }

    @Override
    public Client update(Client client) {
        if (client == null || client.getId() == null)
            throw new IllegalArgumentException("id requerido para actualizar");

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_SQL)) {

            // no tocamos DNI
            ps.setString(1, client.getFirstName());
            ps.setString(2, client.getLastName());
            ps.setString(3, client.getEmail());
            ps.setString(4, client.getPhoneNumber());
            if (client.getBirthDate() == null) ps.setNull(5, Types.DATE);
            else ps.setDate(5, Date.valueOf(client.getBirthDate()));
            ps.setString(6, client.getAddress());
            ps.setInt(7, client.getId());

            if (ps.executeUpdate() == 0)
                throw new RuntimeException("Cliente no encontrado para actualizar");

            return client;

        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando cliente", e);
        }
    }

    @Override
    public boolean deleteById(int id) {
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_SQL)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando cliente", e);
        }
    }
}
