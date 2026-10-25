package com.utn.atenea.personas.repository;

import com.utn.atenea.personas.model.EstadoInscripcionCarrera;
import com.utn.atenea.personas.model.InscripcionCarrera;
import com.utn.atenea.personas.model.Persona;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Filtros de los listados de estudiantes y docentes, para combinar con PersonaRepository.findAll(Specification, Pageable). */
public final class PersonaSpecifications {

    // '!' y no '\' como escape del LIKE: la barra invertida también es escape en los textos de MySQL.
    private static final char ESCAPE = '!';

    private PersonaSpecifications() {
    }

    public static Specification<Persona> esEstudiante() {
        return (persona, query, cb) -> cb.isNotNull(persona.get("legajoEstudiante"));
    }

    public static Specification<Persona> esDocente() {
        return (persona, query, cb) -> cb.isNotNull(persona.get("legajoDocente"));
    }

    /** Las personas activas (true) o las dadas de baja (false). */
    public static Specification<Persona> conActivo(boolean activo) {
        return (persona, query, cb) -> cb.equal(persona.get("activo"), activo);
    }

    /** Búsqueda de texto del listado de estudiantes: el legajo que se compara es el de estudiante. */
    public static Specification<Persona> estudianteCoincideCon(String texto) {
        return coincideCon(texto, "legajoEstudiante");
    }

    /** Búsqueda de texto del listado de docentes: el legajo que se compara es el de docente. */
    public static Specification<Persona> docenteCoincideCon(String texto) {
        return coincideCon(texto, "legajoDocente");
    }

    /**
     * Cada palabra tiene que estar en el apellido o en el nombre, o ser el comienzo del DNI o el legajo
     * exacto: así "lucia gomez" encuentra a Gómez, Lucía. La collation de la base ignora mayúsculas y tildes.
     */
    private static Specification<Persona> coincideCon(String texto, String atributoLegajo) {
        return (persona, query, cb) -> {
            List<Predicate> palabras = new ArrayList<>();
            for (String palabra : texto.strip().split("\\s+")) {
                String escapada = escaparLike(palabra);
                List<Predicate> opciones = new ArrayList<>(List.of(
                        cb.like(persona.get("apellido"), "%" + escapada + "%", ESCAPE),
                        cb.like(persona.get("nombre"), "%" + escapada + "%", ESCAPE),
                        cb.like(persona.get("dni"), escapada + "%", ESCAPE)));
                if (palabra.matches("\\d{1,18}")) {
                    opciones.add(cb.equal(persona.get(atributoLegajo), Long.valueOf(palabra)));
                }
                palabras.add(cb.or(opciones.toArray(Predicate[]::new)));
            }
            return cb.and(palabras.toArray(Predicate[]::new));
        };
    }

    /**
     * Tiene al menos una inscripción en ese estado, en alguno de esos planes (o en cualquiera, si
     * planEstudioIds es null). Con EXISTS y no con un join, un estudiante con dos inscripciones que
     * cumplen no sale dos veces ni se cuenta dos veces en el total.
     */
    public static Specification<Persona> conInscripcion(EstadoInscripcionCarrera estado, Collection<Long> planEstudioIds) {
        return (persona, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<InscripcionCarrera> inscripcion = subquery.from(InscripcionCarrera.class);

            List<Predicate> condiciones = new ArrayList<>(List.of(
                    cb.equal(inscripcion.get("estudiante"), persona),
                    cb.equal(inscripcion.get("estado"), estado)));
            if (planEstudioIds != null) {
                condiciones.add(inscripcion.get("planEstudioId").in(planEstudioIds));
            }

            subquery.select(inscripcion.get("id")).where(condiciones.toArray(Predicate[]::new));
            return cb.exists(subquery);
        };
    }

    private static String escaparLike(String texto) {
        return texto.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
