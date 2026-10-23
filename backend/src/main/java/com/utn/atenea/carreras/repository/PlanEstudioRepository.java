package com.utn.atenea.carreras.repository;

import com.utn.atenea.carreras.model.PlanEstudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlanEstudioRepository extends JpaRepository<PlanEstudio, Long> {

    List<PlanEstudio> findByCarreraIdOrderByAnioInicioDesc(Long carreraId);

    @Query("select p.id from PlanEstudio p where p.carrera.id = :carreraId")
    List<Long> findIdsByCarreraId(Long carreraId);
}
