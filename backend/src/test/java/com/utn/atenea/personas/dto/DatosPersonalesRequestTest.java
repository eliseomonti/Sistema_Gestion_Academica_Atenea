package com.utn.atenea.personas.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba las validaciones y la normalización del request, sin levantar Spring. */
class DatosPersonalesRequestTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static final String DNI = "30111222";
    private static final String TELEFONO = "2234567890";
    private static final LocalDate NACIMIENTO = LocalDate.of(2000, 5, 10);
    private static final String EMAIL = "juan@mail.com";

    @Test
    void datosValidosNoTienenErrores() {
        assertThat(camposConError(datos("María José", "O'Connor-Pérez", DNI, TELEFONO, NACIMIENTO, EMAIL))).isEmpty();
    }

    @Test
    void aceptaLetrasDeOtrosIdiomas() {
        assertThat(camposConError(datos("João", "Gonçalves", DNI, TELEFONO, NACIMIENTO, EMAIL))).isEmpty();
    }

    @Test
    void normalizaEspaciosYPasaElEmailAMinusculas() {
        DatosPersonalesRequest request =
                datos("  María   José ", " Pérez ", " 30111222 ", " 2234567890 ", NACIMIENTO, "  Juan@Mail.COM ");

        assertThat(request.nombre()).isEqualTo("María José");
        assertThat(request.apellido()).isEqualTo("Pérez");
        assertThat(request.dni()).isEqualTo(DNI);
        assertThat(request.telefono()).isEqualTo(TELEFONO);
        assertThat(request.email()).isEqualTo("juan@mail.com");
        assertThat(camposConError(request)).isEmpty();
    }

    @Test
    void camposVaciosDanUnSoloErrorPorCampo() {
        List<String> errores = camposConError(datos("  ", "", null, " ", null, ""));

        assertThat(errores).containsExactlyInAnyOrder(
                "nombre", "apellido", "dni", "telefono", "fechaNacimiento", "email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Juan2", "Juan_Carlos", "Juan--Carlos", "-Juan", "Juan@", "J"})
    void rechazaNombresInvalidos(String nombre) {
        assertThat(camposConError(datos(nombre, "Pérez", DNI, TELEFONO, NACIMIENTO, EMAIL))).containsExactly("nombre");
    }

    @ParameterizedTest
    @ValueSource(strings = {"07123456", "30.111.222", "123456", "123456789", "30111a22", "-3011122"})
    void rechazaDniInvalidos(String dni) {
        assertThat(camposConError(datos("Juan", "Pérez", dni, TELEFONO, NACIMIENTO, EMAIL))).containsExactly("dni");
    }

    @ParameterizedTest
    @ValueSource(strings = {"022345678901", "223154567890", "223456789", "223-456-7890", "+542234567890"})
    void rechazaTelefonosQueNoTienen10Digitos(String telefono) {
        assertThat(camposConError(datos("Juan", "Pérez", DNI, telefono, NACIMIENTO, EMAIL))).containsExactly("telefono");
    }

    @ParameterizedTest
    @ValueSource(strings = {"juan@gmail", "juan.gmail.com", "@gmail.com"})
    void rechazaEmailsInvalidos(String email) {
        assertThat(camposConError(datos("Juan", "Pérez", DNI, TELEFONO, NACIMIENTO, email))).containsExactly("email");
    }

    @Test
    void rechazaFechaDeNacimientoDeHoyOFutura() {
        assertThat(camposConError(datos("Juan", "Pérez", DNI, TELEFONO, LocalDate.now(), EMAIL)))
                .containsExactly("fechaNacimiento");
        assertThat(camposConError(datos("Juan", "Pérez", DNI, TELEFONO, LocalDate.now().plusDays(1), EMAIL)))
                .containsExactly("fechaNacimiento");
    }

    private static DatosPersonalesRequest datos(String nombre, String apellido, String dni, String telefono,
                                                LocalDate fechaNacimiento, String email) {
        return new DatosPersonalesRequest(nombre, apellido, dni, telefono, fechaNacimiento, email);
    }

    private static List<String> camposConError(DatosPersonalesRequest request) {
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }
}
