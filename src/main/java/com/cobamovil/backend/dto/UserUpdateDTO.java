package com.cobamovil.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserUpdateDTO {
    
    @Size(min = 3, max = 50, message = "Username debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username solo puede contener letras, números y guiones bajos")
    private String username;
    
    @Email(message = "Email debe tener un formato válido")
    @Size(max = 100, message = "Email no puede exceder 100 caracteres")
    private String email;
    
    @Size(min = 8, message = "Password debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&].*$", 
             message = "Password debe contener al menos una letra minúscula, una mayúscula, un número y un carácter especial")
    private String password;

    @Size(max = 20, message = "Teléfono no puede exceder 20 caracteres")
    @Pattern(regexp = "^$|^\\+?[0-9]{7,20}$", message = "Teléfono debe ser un número válido")
    private String phone;
    
    // Constructor vacío
    public UserUpdateDTO() {}
    
    // Getters y Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
