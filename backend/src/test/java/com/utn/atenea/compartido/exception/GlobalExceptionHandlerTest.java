package com.utn.atenea.compartido.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sin filtros de seguridad: acá sólo se prueba la traducción de excepciones a ProblemDetail. */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.PruebaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.PruebaController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void recursoNoEncontrado() throws Exception {
        mvc.perform(get("/prueba/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("No existe la carrera 15"));
    }

    @Test
    void recursoDuplicado() throws Exception {
        mvc.perform(get("/prueba/duplicado"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"))
                .andExpect(jsonPath("$.detail").value("Ya existe una persona con DNI 30123456"));
    }

    @Test
    void reglaNegocio() throws Exception {
        mvc.perform(get("/prueba/regla"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regla de negocio no cumplida"))
                .andExpect(jsonPath("$.detail").value("No hay cupo en la comisión"));
    }

    @Test
    void integridadDeDatosNoExponeElMensajeDeLaBase() throws Exception {
        mvc.perform(get("/prueba/integridad"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto con datos existentes"))
                .andExpect(content().string(not(containsString("uk_persona_dni"))));
    }

    @Test
    void errorInesperadoNoExponeElDetalle() throws Exception {
        mvc.perform(get("/prueba/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Error interno"))
                .andExpect(content().string(not(containsString("detalle interno"))));
    }

    @Test
    void validacionDevuelveLosCampos() throws Exception {
        mvc.perform(post("/prueba/validacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"cupo\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores[*].campo").value(containsInAnyOrder("nombre", "cupo")));
    }

    @Test
    void jsonMalFormado() throws Exception {
        mvc.perform(post("/prueba/validacion").contentType(MediaType.APPLICATION_JSON).content("{nombre"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void tipoDeParametroInvalido() throws Exception {
        mvc.perform(get("/prueba/carreras/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    record PruebaRequest(@NotBlank String nombre, @Positive int cupo) {
    }

    @RestController
    static class PruebaController {

        @GetMapping("/prueba/no-encontrado")
        void noEncontrado() {
            throw new RecursoNoEncontradoException("No existe la carrera 15");
        }

        @GetMapping("/prueba/duplicado")
        void duplicado() {
            throw new RecursoDuplicadoException("Ya existe una persona con DNI 30123456");
        }

        @GetMapping("/prueba/regla")
        void regla() {
            throw new ReglaNegocioException("No hay cupo en la comisión");
        }

        @GetMapping("/prueba/integridad")
        void integridad() {
            throw new DataIntegrityViolationException("Duplicate entry for key 'uk_persona_dni'");
        }

        @GetMapping("/prueba/inesperado")
        void inesperado() {
            throw new IllegalStateException("detalle interno");
        }

        @PostMapping("/prueba/validacion")
        void validacion(@Valid @RequestBody PruebaRequest request) {
        }

        @GetMapping("/prueba/carreras/{id}")
        void carrera(@PathVariable Long id) {
        }
    }
}
