package com.utn.atenea.examenes.service;

import com.utn.atenea.examenes.CondicionCursada;
import com.utn.atenea.examenes.InscriptoACursada;
import com.utn.atenea.examenes.Inscriptos;
import com.utn.atenea.examenes.dto.ActaCursadaResponse;
import com.utn.atenea.examenes.dto.CargaCierreRequest;
import com.utn.atenea.examenes.dto.CierreEstudianteResponse;
import com.utn.atenea.examenes.dto.CierreRequest;
import com.utn.atenea.examenes.model.CierreCursada;
import com.utn.atenea.examenes.repository.CierreCursadaRepository;
import com.utn.atenea.personas.EstudianteResumen;
import com.utn.atenea.personas.PersonasApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Autoservicio del docente: el acta de cierre de una materia que dicta, que deja a cada estudiante regular,
 * promocionado o no regular. docenteId es el personaId del token.
 */
@Service
@Transactional(readOnly = true)
public class CierreCursadaService {

    private final CierreCursadaRepository cierreCursadaRepository;
    private final AccesoDocente accesoDocente;
    private final Inscriptos inscriptos;
    private final PersonasApi personasApi;
    private final Period vigenciaRegularidad;

    public CierreCursadaService(CierreCursadaRepository cierreCursadaRepository, AccesoDocente accesoDocente,
                                Inscriptos inscriptos, PersonasApi personasApi,
                                @Value("${atenea.regularidad.vigencia}") Period vigenciaRegularidad) {
        this.cierreCursadaRepository = cierreCursadaRepository;
        this.accesoDocente = accesoDocente;
        this.inscriptos = inscriptos;
        this.personasApi = personasApi;
        this.vigenciaRegularidad = vigenciaRegularidad;
    }

    public ActaCursadaResponse acta(Long docenteId, Long comisionMateriaId) {
        accesoDocente.validarCursada(docenteId, comisionMateriaId);
        return acta(comisionMateriaId, inscriptos.inscriptosALaCursada(comisionMateriaId));
    }

    /**
     * Sin acta se cierra con fecha de hoy; con una distinta, se rectifica; con la misma condición y nota, no
     * se toca. La rectificación conserva la fecha de cierre de la original: corregir no estira la regularidad.
     */
    @Transactional
    public ActaCursadaResponse cerrar(Long docenteId, Long comisionMateriaId, CargaCierreRequest request) {
        accesoDocente.validarCursada(docenteId, comisionMateriaId);
        List<InscriptoACursada> anotados = inscriptos.inscriptosALaCursada(comisionMateriaId);
        Planilla.validarInscripciones(request.cierres().stream().map(CierreRequest::inscripcionComisionId).toList(),
                anotados.stream().map(InscriptoACursada::inscripcionComisionId).collect(Collectors.toSet()));

        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();
        Map<Long, CierreCursada> vigentes = vigentes(anotados);
        for (CierreRequest cierre : request.cierres()) {
            CierreCursada vigente = vigentes.get(cierre.inscripcionComisionId());
            if (vigente == null) {
                cierreCursadaRepository.save(new CierreCursada(cierre.inscripcionComisionId(), docenteId,
                        cierre.condicion(), cierre.nota(), hoy, vencimiento(cierre.condicion(), hoy), ahora));
            } else if (vigente.getCondicion() != cierre.condicion()
                    || !Objects.equals(vigente.getNotaCursada(), cierre.nota())) {
                cierreCursadaRepository.save(vigente.rectificar(docenteId, cierre.condicion(), cierre.nota(),
                        vigente.getFechaCierre(), vencimiento(cierre.condicion(), vigente.getFechaCierre()), ahora));
            }
        }
        return acta(comisionMateriaId, anotados);
    }

    // Sólo la regularidad vence (CHECK chk_cierre_vencimiento). Se guarda con la vigencia de hoy.
    private LocalDate vencimiento(CondicionCursada condicion, LocalDate fechaCierre) {
        return condicion == CondicionCursada.REGULAR ? fechaCierre.plus(vigenciaRegularidad) : null;
    }

    private ActaCursadaResponse acta(Long comisionMateriaId, List<InscriptoACursada> anotados) {
        Map<Long, EstudianteResumen> estudiantes = personasApi
                .buscarEstudiantes(anotados.stream().map(InscriptoACursada::estudianteId).toList()).stream()
                .collect(Collectors.toMap(EstudianteResumen::personaId, Function.identity()));
        Map<Long, CierreCursada> vigentes = vigentes(anotados);

        List<CierreEstudianteResponse> filas = anotados.stream()
                .map(anotado -> {
                    EstudianteResumen estudiante = estudiantes.get(anotado.estudianteId());
                    CierreCursada cierre = vigentes.get(anotado.inscripcionComisionId());
                    return new CierreEstudianteResponse(anotado.inscripcionComisionId(), estudiante.personaId(),
                            estudiante.legajoEstudiante(), estudiante.apellido(), estudiante.nombre(),
                            cierre != null ? cierre.getCondicion() : null,
                            cierre != null ? cierre.getNotaCursada() : null,
                            cierre != null ? cierre.getFechaCierre() : null,
                            cierre != null ? cierre.getFechaVencimiento() : null);
                })
                .sorted(Comparator.comparing(CierreEstudianteResponse::apellido)
                        .thenComparing(CierreEstudianteResponse::nombre))
                .toList();
        return new ActaCursadaResponse(comisionMateriaId, filas);
    }

    private Map<Long, CierreCursada> vigentes(List<InscriptoACursada> anotados) {
        if (anotados.isEmpty()) {
            return Map.of();
        }
        return cierreCursadaRepository
                .vigentes(anotados.stream().map(InscriptoACursada::inscripcionComisionId).toList()).stream()
                .collect(Collectors.toMap(CierreCursada::getInscripcionComisionId, Function.identity()));
    }
}
