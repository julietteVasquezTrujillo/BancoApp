package org.example.model;

import java.time.LocalDate;

public class Client {
    private Integer id;
    private String dni;           // DNI debe ser un valor inmutable
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate birthDate;
    private String address;

    public Client() {
        // necesario para mapear con setters
    }
    // Se debe agregar un constructor
    public Client(String dni, String firstName, String lastName,
                  String email, String phoneNumber,
                  LocalDate birthDate, String address) {
        if (dni == null || dni.isBlank()) {
            throw new IllegalArgumentException("dni requerido");
        }
        this.dni = dni;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.address = address;
    }

    // getters / setters (sin setDni)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getDni() { return dni; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", dni='" + dni + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", birthDate=" + birthDate +
                ", address='" + address + '\'' +
                '}';
    }
}
