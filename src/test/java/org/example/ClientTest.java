package org.example;

import org.example.model.Client;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    @Test
    void testCreateClientWithValidDni() {
        Client c = new Client("11223344", "Rosa", "Santos",
                "rosa@mail.com", "987654321",
                LocalDate.of(1998, 3, 21), "Av. Primavera 123");

        assertEquals("11223344", c.getDni());
        assertEquals("Rosa", c.getFirstName());
        assertEquals("Santos", c.getLastName());
        assertEquals("rosa@mail.com", c.getEmail());
    }

    @Test
    void testCreateClientWithoutDniThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Client("", "Rosa", "Santos",
                    "rosa@mail.com", "987654321",
                    LocalDate.of(1998, 3, 21), "Av. Primavera 123");
        });
    }
}
