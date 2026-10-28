package com.utn.atenea.inscripciones.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Inscripción de un estudiante a una mesa de examen final. */
@Entity
@Table(name = "estudiante_inscripcion_examen_final")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InscripcionFinal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // inscripcion_carrera es del módulo personas: se referencia por id.
    @Column(name = "inscripcion_carrera_id", nullable = false, updatable = false)
    private Long inscripcionCarreraId;

    // examen_final es del módulo examenes: se referencia por id.
    @Column(name = "examen_final_id", nullable = false, updatable = false)
    private Long examenFinalId;

    // Cambia sólo con reinscribir.
    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion;

    // estado y fechaBaja cambian juntos (CHECK chk_eief_baja): sólo con darDeBaja y reinscribir.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInscripcion estado = EstadoInscripcion.INSCRIPTO;

    @Column(name = "fecha_baja")
    private LocalDate fechaBaja;

    public InscripcionFinal(Long inscripcionCarreraId, Long examenFinalId, LocalDate fechaInscripcion) {
        this.inscripcionCarreraId = inscripcionCarreraId;
        this.examenFinalId = examenFinalId;
        this.fechaInscripcion = fechaInscripcion;
    }

    public void darDeBaja(LocalDate fecha) {
        this.estado = EstadoInscripcion.BAJA;
        this.fechaBaja = fecha;
    }

    /**
     * Vuelve a anotar a quien se había dado de baja. Se reactiva la misma fila porque el UNIQUE de la
     * tabla no deja crear otra; la baja anterior no queda registrada.
     */
    public void reinscribir(LocalDate fecha) {
        this.fechaInscripcion = fecha;
        this.estado = EstadoInscripcion.INSCRIPTO;
        this.fechaBaja = null;
    }
}
