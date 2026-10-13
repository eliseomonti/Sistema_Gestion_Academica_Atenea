package com.utn.atenea.seguridad.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;


@Entity
@Table(name = "rol")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rol {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private NombreRol nombre;

    // nombre es clave natural (UNIQUE y NOT NULL), así que es seguro para usar Rol dentro de un Set.
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Rol otro && nombre == otro.nombre);
    }

    @Override
    public int hashCode() {
        return nombre == null ? 0 : nombre.hashCode();
    }
}
