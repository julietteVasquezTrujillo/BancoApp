package org.example.dao;

import org.example.model.Client;

import java.util.List;
import java.util.Optional;

public interface ClientDAO {
    Client save(Client client);                 // Crea, asigna id
    Optional<Client> findById(int id);         // Lee por id
    Optional<Client> findByDni(String dni);    // Lee por DNI
    List<Client> findAll();                    // Lista
    Client update(Client client);              // Actualiza (DNI NO cambia)
    boolean deleteById(int id);                // Elimina
}
