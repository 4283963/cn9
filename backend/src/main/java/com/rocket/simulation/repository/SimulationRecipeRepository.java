package com.rocket.simulation.repository;

import com.rocket.simulation.entity.SimulationRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulationRecipeRepository extends JpaRepository<SimulationRecipe, Long> {

    List<SimulationRecipe> findAllByOrderByCreatedAtDesc();

    List<SimulationRecipe> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);
}
