package com.utn.atenea.carreras.repository;

import com.utn.atenea.carreras.model.PlanEstudio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanEstudioRepository extends JpaRepository<PlanEstudio, Long> {

    List<PlanEstudio> findByCarreraIdOrderByAnioInicioDesc(Long carreraId);
}
