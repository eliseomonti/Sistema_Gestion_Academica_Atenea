package com.utn.atenea.comisiones.repository;

import com.utn.atenea.comisiones.model.Aula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Las aulas se cargan por SQL: no hay alta ni modificación desde la API.
public interface AulaRepository extends JpaRepository<Aula, Long> {

    List<Aula> findByActivaTrueOrderByNombreAsc();
}
