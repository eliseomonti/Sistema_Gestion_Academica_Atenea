package com.utn.atenea.comisiones.repository;

import com.utn.atenea.comisiones.model.ComisionMateria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Las materias se agregan y se quitan desde Comision; esto es para consultarlas. */
public interface ComisionMateriaRepository extends JpaRepository<ComisionMateria, Long> {

    // Con la comisión en la condición, una materia no se encuentra desde la URL de otra comisión.
    Optional<ComisionMateria> findByIdAndComisionId(Long id, Long comisionId);

    // El docente está asignado a la materia de la comisión, con cualquier rol.
    boolean existsByIdAndDocentesDocenteId(Long id, Long docenteId);
}
