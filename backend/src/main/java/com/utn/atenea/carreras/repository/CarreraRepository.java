package com.utn.atenea.carreras.repository;

import com.utn.atenea.carreras.model.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarreraRepository extends JpaRepository<Carrera, Long> {

    List<Carrera> findAllByOrderByNombreAsc();
}
