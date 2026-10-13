package com.utn.atenea.carreras.repository;

import com.utn.atenea.carreras.model.Correlatividad;
import com.utn.atenea.carreras.model.CorrelatividadId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrelatividadRepository extends JpaRepository<Correlatividad, CorrelatividadId> {
}
