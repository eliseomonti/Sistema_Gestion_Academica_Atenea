package com.utn.atenea.comisiones.model;

import com.utn.atenea.comisiones.RolDocente;
import jakarta.persistence.*;
import lombok.*;

/** Fila de comision_docente. No es entity: vive y muere con su ComisionMateria. */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ComisionDocente {

    // persona es del módulo personas: se referencia por id.
    @Column(name = "docente_id", nullable = false)
    private Long docenteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolDocente rol;
}
