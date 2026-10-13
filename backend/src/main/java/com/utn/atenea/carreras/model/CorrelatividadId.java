package com.utn.atenea.carreras.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CorrelatividadId implements Serializable {

    @Column(name = "plan_materia_id")
    private Long planMateriaId;

    @Column(name = "plan_materia_correlativa_id")
    private Long correlativaId;
}
