package com.utn.atenea.personas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Locale;

/** Datos personales que el admin carga en el alta de un estudiante o de un docente. */
public record DatosPersonalesRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
        @Pattern(regexp = NOMBRE, message = "El nombre sólo puede tener letras, separadas por espacio, guion o apóstrofe")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
        @Pattern(regexp = NOMBRE, message = "El apellido sólo puede tener letras, separadas por espacio, guion o apóstrofe")
        String apellido,

        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "^[1-9]\\d{6,7}$", message = "El DNI debe tener 7 u 8 dígitos, sin puntos ni 0 adelante")
        String dni,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos: código de área sin 0 y número sin 15")
        String telefono,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento tiene que ser anterior a hoy")
        LocalDate fechaNacimiento,

        @NotBlank(message = "El email es obligatorio")
        @Size(max = 100, message = "El email no puede tener más de 100 caracteres")
        @Email(regexp = ".+@.+\\..+", message = "El email no tiene un formato válido")
        String email
) {

    // Letras de cualquier idioma (tildes, ñ, ç...), con un espacio, guion o apóstrofe entre palabras.
    private static final String NOMBRE = "^\\p{L}+([ '-]\\p{L}+)*$";

    // Jackson construye el record con este constructor, así que la normalización corre antes que
    // @Valid: los espacios de más no hacen fallar el formato y un texto vacío llega como null,
    // que sólo dispara el "es obligatorio" en vez de tres errores para el mismo campo.
    public DatosPersonalesRequest {
        nombre = normalizar(nombre);
        apellido = normalizar(apellido);
        dni = normalizar(dni);
        telefono = normalizar(telefono);
        email = normalizar(email);
        if (email != null) {
            email = email.toLowerCase(Locale.ROOT);
        }
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String normalizado = texto.strip().replaceAll("\\s+", " ");
        return normalizado.isEmpty() ? null : normalizado;
    }
}
